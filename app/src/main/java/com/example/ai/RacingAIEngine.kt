package com.example.ai

import com.example.model.*
import kotlin.math.*

data class AICar(
    val driver: Driver,
    var trackDistance: Float,
    var lateralOffset: Float, // distance from centerline in meters (-5.5m to +5.5m)
    var speedMs: Float = 0f,
    var headingRad: Float = 0f,
    var position: Vector3D = Vector3D(),
    var isOvertaking: Boolean = false,
    var currentLap: Int = 1,
    var lastLapTimeMs: Long = 0L,
    var isDrzActive: Boolean = false,
    var ersActive: Boolean = false,
    var engineSpec: EngineSpec = EngineSpec.APEX_WORKS,
    val checkpointsCleared: BooleanArray = booleanArrayOf(true, false, false),
    var finishedRace: Boolean = false,
    var finishTimeMs: Long = 0L
)

class RacingAIEngine(
    val circuit: CircuitTrack
) {
    val aiRivals = mutableListOf<AICar>()
    var difficulty: AIDifficulty = AIDifficulty.INTERMEDIATE

    init {
        initializeRivalGrid()
    }

    private fun initializeRivalGrid() {
        aiRivals.clear()
        val defaultDrivers = listOf(
            Pair(Driver("Marcus Lindqvist", "LIN", "Vortex Racing", 1, 0xFF1A237E, aiSkill = 0.99f), EngineSpec.VORTEX_ECO),
            Pair(Driver("Carlos Silva", "SIL", "Silver Arrow", 55, 0xFF78909C, aiSkill = 0.96f), EngineSpec.SILVER_HYBRID),
            Pair(Driver("Yuki Takahashi", "TAK", "Modena Corsa", 22, 0xFFD32F2F, aiSkill = 0.94f), EngineSpec.MODENA_TURBO),
            Pair(Driver("Oliver Bennett", "BEN", "Apex Academy", 8, 0xFF00B0FF, aiSkill = 0.91f), EngineSpec.APEX_WORKS)
        )

        // Default P1, P2 ahead of player (P3), P4 & P5 behind player
        val gridDistances = listOf(22f, 11f, -11f, -22f)
        val gridOffsets = listOf(-2.6f, 2.6f, 2.6f, -2.6f)

        for (i in defaultDrivers.indices) {
            val (driver, engine) = defaultDrivers[i]
            val dist = gridDistances[i]
            val pt = circuit.getPointAtDistance(dist)
            val posX = pt.position.x + pt.normal.x * gridOffsets[i]
            val posY = pt.position.y + pt.normal.y * gridOffsets[i]
            val heading = atan2(pt.tangent.y, pt.tangent.x)

            aiRivals.add(
                AICar(
                    driver = driver,
                    trackDistance = dist,
                    lateralOffset = gridOffsets[i],
                    speedMs = 0f,
                    headingRad = heading,
                    position = Vector3D(posX, posY, pt.position.z),
                    engineSpec = engine
                )
            )
        }
    }

    /**
     * Arranges AI rivals cleanly around the player's starting grid slot (1..5).
     */
    fun resetToStartingGrid(playerGridSlot: Int = 3) {
        val clampedPlayerSlot = playerGridSlot.coerceIn(1, aiRivals.size + 1)
        val gridSpacingM = 11f
        val allSlots = (1..(aiRivals.size + 1)).filter { it != clampedPlayerSlot }

        aiRivals.forEachIndexed { idx, ai ->
            val slot = allSlots.getOrElse(idx) { idx + 1 }
            // Relative to player start line (slot 1 = farthest forward, player starts at 0f)
            val dist = (clampedPlayerSlot - slot) * gridSpacingM
            val offset = if (slot % 2 == 1) -2.6f else 2.6f
            val pt = circuit.getPointAtDistance(dist)

            ai.trackDistance = dist
            ai.lateralOffset = offset
            ai.speedMs = 0f
            ai.currentLap = 1
            ai.isOvertaking = false
            ai.isDrzActive = false
            ai.ersActive = false
            ai.finishedRace = false
            ai.finishTimeMs = 0L
            ai.checkpointsCleared[0] = true
            ai.checkpointsCleared[1] = false
            ai.checkpointsCleared[2] = false
            ai.position = Vector3D(
                pt.position.x + pt.normal.x * offset,
                pt.position.y + pt.normal.y * offset,
                pt.position.z
            )
            ai.headingRad = atan2(pt.tangent.y, pt.tangent.x)
        }
    }

    fun step(
        playerState: VehicleState,
        dt: Float,
        totalLaps: Int = 3,
        raceElapsedMs: Long = 0L
    ) {
        val clampedDt = dt.coerceIn(0.005f, 0.05f)
        val totalLen = circuit.totalLengthMeters.coerceAtLeast(100f)
        val maxLateral = (circuit.widthMeters * 0.5f) - 1.3f

        val playerSpline = circuit.findNearestSplinePointNearDistance(
            playerState.position.x,
            playerState.position.y,
            playerState.currentTrackDistance
        )
        val playerLateralOffset = circuit.getLateralOffset(playerState.position.x, playerState.position.y, playerSpline)

        for (i in aiRivals.indices) {
            val ai = aiRivals[i]

            // 1. Look ahead on spline proportional to reaction delay + speed
            val lookaheadDist = (ai.speedMs * (1.25f + difficulty.reactionDelaySec)).coerceIn(28f, 100f)
            val currentPt = circuit.getPointAtDistance(ai.trackDistance)
            val aheadPt = circuit.getPointAtDistance(ai.trackDistance + lookaheadDist)

            val dot = (currentPt.tangent.x * aheadPt.tangent.x + currentPt.tangent.y * aheadPt.tangent.y).coerceIn(-1f, 1f)
            val angleDiff = acos(dot)
            val cornerCurvature = angleDiff / lookaheadDist

            // Corner speed target scaled by difficulty & driver rating
            val baseMaxCornerSpeed = sqrt((18.5f * difficulty.cornerGripScale) / (cornerCurvature + 0.001f))
                .coerceIn(20f, 94f)
            val driverPaceFactor = ai.driver.aiSkill * difficulty.speedScale
            val enginePowerBonus = (ai.engineSpec.peakTorqueNm / 445f).coerceIn(0.95f, 1.05f)
            var targetSpeed = baseMaxCornerSpeed * driverPaceFactor * enginePowerBonus

            // DRS & Elite ERS attack on straightaways
            ai.isDrzActive = currentPt.drsZone && cornerCurvature < 0.004f
            ai.ersActive = difficulty.aggression > 0.7f && cornerCurvature < 0.003f && ai.speedMs > 40f
            if (ai.isDrzActive) {
                targetSpeed *= (1.04f + difficulty.aggression * 0.05f)
            }
            if (ai.ersActive) {
                targetSpeed *= 1.035f
            }

            // 2. Overtaking, Multi-Car Traffic & Defensive Positioning
            var desiredLateral = sin(ai.trackDistance * 0.0048f) * 2.2f
            ai.isOvertaking = false

            // Check distance to player
            val playerDistGap = playerState.currentTrackDistance - ai.trackDistance
            if (playerDistGap in 1.5f..28f) {
                // Player is directly ahead of this AI
                ai.isOvertaking = true
                val passSide = if (playerLateralOffset >= 0f) -3.4f else 3.4f
                desiredLateral = passSide
                if (playerDistGap < 7.5f && abs(playerLateralOffset - ai.lateralOffset) < 2.1f) {
                    // Brake or match speed safely if lane isn't clear yet
                    val followCap = (playerState.speedMs + difficulty.aggression * 2.5f).coerceAtLeast(10f)
                    targetSpeed = min(targetSpeed, followCap)
                }
            } else if (playerDistGap in -18f..-2f && difficulty.aggression >= 0.75f && !ai.isDrzActive) {
                // Pro / Elite AI defends racing line cleanly without swerving unpredictably
                desiredLateral = (playerLateralOffset * 0.45f).coerceIn(-maxLateral * 0.6f, maxLateral * 0.6f)
            }

            // Check other AI cars ahead to avoid AI train collisions & pass slower cars
            for (j in aiRivals.indices) {
                if (i == j) continue
                val other = aiRivals[j]
                val gap = other.trackDistance - ai.trackDistance
                if (gap in 1.5f..24f) {
                    val latDiff = abs(other.lateralOffset - ai.lateralOffset)
                    if (latDiff < 2.6f) {
                        ai.isOvertaking = true
                        desiredLateral = if (other.lateralOffset >= 0f) -3.3f else 3.3f
                        if (gap < 7f && latDiff < 1.9f) {
                            targetSpeed = min(targetSpeed, other.speedMs * 0.98f)
                        }
                    }
                }
            }

            val steerGain = if (ai.isOvertaking) difficulty.overtakeAgility else 0.95f
            ai.lateralOffset = (ai.lateralOffset + (desiredLateral - ai.lateralOffset) * steerGain * clampedDt)
                .coerceIn(-maxLateral, maxLateral)

            // 3. Longitudinal Acceleration / Braking with Engine & Difficulty Scaling
            if (ai.speedMs < targetSpeed) {
                val baseAccel = 10.2f * ai.driver.aiSkill * difficulty.accelScale * enginePowerBonus
                val boostMult = if (ai.ersActive) 1.12f else 1.0f
                ai.speedMs = min(ai.speedMs + baseAccel * boostMult * clampedDt, targetSpeed)
            } else {
                val brakeRate = 25.0f * (0.85f + difficulty.cornerGripScale * 0.2f)
                ai.speedMs = (ai.speedMs - brakeRate * clampedDt).coerceAtLeast(targetSpeed)
            }

            // 4. Advance Distance, World Coordinates & Sector Checkpoints
            ai.trackDistance += ai.speedMs * clampedDt
            val updatedSpline = circuit.getPointAtDistance(ai.trackDistance)

            ai.position.x = updatedSpline.position.x + updatedSpline.normal.x * ai.lateralOffset
            ai.position.y = updatedSpline.position.y + updatedSpline.normal.y * ai.lateralOffset
            ai.position.z = updatedSpline.position.z
            ai.headingRad = atan2(updatedSpline.tangent.y, updatedSpline.tangent.x)

            // Record cleared checkpoints on current lap
            val secIdx = (updatedSpline.sector - 1).coerceIn(0, 2)
            if (ai.trackDistance >= 0f) {
                if (secIdx == 0 || ai.checkpointsCleared[secIdx - 1]) {
                    ai.checkpointsCleared[secIdx] = true
                }
            }

            // Validated Lap Completion
            if (ai.trackDistance >= totalLen * ai.currentLap &&
                ai.checkpointsCleared[0] && ai.checkpointsCleared[1] && ai.checkpointsCleared[2]
            ) {
                ai.currentLap++
                ai.checkpointsCleared[0] = true
                ai.checkpointsCleared[1] = false
                ai.checkpointsCleared[2] = false
                if (!ai.finishedRace && ai.currentLap > totalLaps) {
                    ai.finishedRace = true
                    ai.finishTimeMs = raceElapsedMs
                }
            }
        }
    }
}
