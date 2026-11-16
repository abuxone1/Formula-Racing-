package com.example.game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.ai.RacingAIEngine
import com.example.audio.FormulaAudioEngine
import com.example.model.*
import com.example.physics.FormulaPhysicsEngine
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sin

class RaceSimulationManager(
    private val context: Context? = null
) {
    val circuit = CircuitTrack()
    val physics = FormulaPhysicsEngine(circuit)
    val aiEngine = RacingAIEngine(circuit)
    val audio = FormulaAudioEngine()

    // Public Observable States
    val playerState = VehicleState()
    var carSetup = CarSetup()
    var weather = WeatherCondition.DRY_SUNNY
    var cameraMode = CameraMode.CHASE
    var controlType = ControlType.VIRTUAL_WHEEL

    // AI & Race Rules Settings
    var startingGridSlot: Int = 3
    var aiDifficulty: AIDifficulty
        get() = aiEngine.difficulty
        set(value) {
            aiEngine.difficulty = value
        }

    // Graphics & Performance Settings
    var graphicsQuality = GraphicsQuality.MEDIUM
    var adaptiveThermalControl = true

    // Timing, Checkpoints & Penalties
    var currentLap = 1
    var totalLaps = 3
    var raceElapsedMs = 0L
    var lapStartTimeMs = 0L
    var currentLapTimeMs = 0L
    var bestLapTimeMs = 84320L // Default target 1:24.320
    var lastLapTimeMs = 0L
    var sector1TimeMs = 0L
    var sector2TimeMs = 0L
    var sector3TimeMs = 0L
    var currentSector = 1
    var deltaVsBestSec = 0.0f
    var playerRacePosition = 1
    var isRaceFinished = false
    var playerFinishTimeMs = 0L

    val playerCheckpointsCleared = booleanArrayOf(true, false, false)
    var cornerCutWarnings = 0
    var penaltySeconds = 0f
    private var offTrackCutTimerSec = 0f
    private var collisionRadioCooldownSec = 0f

    val radio = RaceEngineerRadio()
    var driverProfile = DriverProfile()
    val telemetryHistory = mutableListOf<TelemetrySample>()

    // Cinematic Race Intro & Start Sequence: 0=Pre, 1..5=Lights On, 6=Lights Out!
    var isIntroActive = true
    var introElapsedSec = 0f
    val introDurationSec = 4.2f
    val introPhase: RaceIntroPhase
        get() = when {
            !isIntroActive -> RaceIntroPhase.NONE
            introElapsedSec < 1.6f -> RaceIntroPhase.CIRCUIT_FLYOVER
            introElapsedSec < 3.2f -> RaceIntroPhase.GRID_SHOWCASE
            introElapsedSec < introDurationSec -> RaceIntroPhase.CAMERA_HANDOFF
            else -> RaceIntroPhase.NONE
        }

    var startLights = 0
    var isStartingCountdown = true
    private var countdownTimerSec = 0f

    // Pit Stop status
    var isPitStopping = false
    var pitStopProgress = 0f

    private val vibrator: Vibrator? = try {
        if (context != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } else null
    } catch (_: Exception) { null }

    private var lastSector = 1

    fun selectTrackLevel(level: TrackLevelId) {
        circuit.loadLevel(level)
        weather = level.defaultWeather
        startRaceSession()
    }

    fun startRaceSession(playIntro: Boolean = true, gridSlot: Int = startingGridSlot) {
        startingGridSlot = gridSlot.coerceIn(1, aiEngine.aiRivals.size + 1)
        playerState.reset()
        val startPt = circuit.getPointAtDistance(0f)
        playerState.position = Vector3D(startPt.position.x, startPt.position.y, startPt.position.z)
        playerState.headingRad = atan2(startPt.tangent.y, startPt.tangent.x)

        audio.start()
        isRaceFinished = false
        currentLap = 1
        currentSector = 1
        lastSector = 1
        raceElapsedMs = 0L
        lapStartTimeMs = 0L
        currentLapTimeMs = 0L
        playerFinishTimeMs = 0L
        cornerCutWarnings = 0
        penaltySeconds = 0f
        offTrackCutTimerSec = 0f
        collisionRadioCooldownSec = 0f
        playerCheckpointsCleared[0] = true
        playerCheckpointsCleared[1] = false
        playerCheckpointsCleared[2] = false

        startLights = 0
        countdownTimerSec = 0f
        isStartingCountdown = true
        isIntroActive = playIntro
        introElapsedSec = 0f
        isPitStopping = false
        pitStopProgress = 0f
        telemetryHistory.clear()

        aiEngine.resetToStartingGrid(startingGridSlot)
        updateRacePositions()
    }

    fun skipIntro() {
        isIntroActive = false
        introElapsedSec = introDurationSec
        countdownTimerSec = 0f
        startLights = 1
    }

    fun shiftUp() {
        physics.shiftUp(playerState)
    }

    fun shiftDown() {
        physics.shiftDown(playerState)
    }

    fun recoverCarToTrack() {
        if (playerState.recoveryCooldownSec <= 0f) {
            physics.recoverToTrack(playerState)
            offTrackCutTimerSec = 0f
            radio.triggerMessage(RadioMessageType.STRATEGY, "Car recovered to circuit centerline. Clear ahead.")
            triggerHaptic(35)
        }
    }

    fun stepSimulation(dt: Float) {
        val clampedDt = dt.coerceIn(0.005f, 0.05f)
        if (isRaceFinished) {
            audio.targetRpm = 4200f
            audio.throttle = 0f
            return
        }

        collisionRadioCooldownSec = (collisionRadioCooldownSec - clampedDt).coerceAtLeast(0f)

        // 0. Cinematic Race Introduction Sequence
        if (isIntroActive) {
            introElapsedSec += clampedDt
            val revWave = 5200f + sin(introElapsedSec * 5.2f) * 1400f
            playerState.rpm = revWave
            audio.targetRpm = revWave
            audio.throttle = 0.2f
            if (introElapsedSec >= introDurationSec) {
                isIntroActive = false
                countdownTimerSec = 0f
                startLights = 1
            }
            return
        }

        // 1. Starting Lights Sequence Handling
        if (isStartingCountdown) {
            countdownTimerSec += clampedDt
            val stage = (countdownTimerSec / 0.65f).toInt() + 1
            if (stage <= 5) {
                startLights = stage.coerceIn(1, 5)
            } else if (countdownTimerSec >= 3.9f) {
                startLights = 6 // LIGHTS OUT! GO!
                isStartingCountdown = false
                lapStartTimeMs = 0L
                radio.triggerMessage(RadioMessageType.PACE, "LIGHTS OUT! Good reaction, push hard into Turn 1.")
                triggerHaptic(60)
            }
            if (startLights < 6) {
                playerState.speedMs = 0f
                playerState.rpm = 4200f + playerState.throttleInput * 8200f
                audio.targetRpm = playerState.rpm
                audio.throttle = playerState.throttleInput
                return
            }
        }

        radio.update()

        // Advance deterministic simulation clock (pause-safe & unit-test deterministic)
        val dtMs = (clampedDt * 1000f).toLong().coerceAtLeast(1L)
        raceElapsedMs += dtMs
        currentLapTimeMs = raceElapsedMs - lapStartTimeMs

        // 2. Player Vehicle Physics Step
        val prevDamage = playerState.overallDamage
        physics.step(playerState, carSetup, weather, clampedDt)

        // 3. Competitive AI Opponents Step
        aiEngine.step(playerState, clampedDt, totalLaps = totalLaps, raceElapsedMs = raceElapsedMs)

        // 4. Car-to-Car Collision Detection & Response
        val collided = physics.resolveVehicleCollisions(playerState, carSetup, aiEngine.aiRivals)
        if (collided) {
            triggerHaptic(45)
        }
        if ((playerState.overallDamage - prevDamage) > 0.04f && collisionRadioCooldownSec <= 0f) {
            collisionRadioCooldownSec = 6.0f
            val dmgPct = (playerState.overallDamage * 100f).toInt()
            radio.triggerMessage(RadioMessageType.ALERT, "Contact detected! Structural damage at $dmgPct%. Box when ready.")
        }

        // Automatic recovery if stuck deeply off-track for 4s
        if (playerState.offTrackTimeAccumSec >= 4.0f && playerState.speedKph < 18f && playerState.recoveryCooldownSec <= 0f) {
            recoverCarToTrack()
        }

        // Record telemetry sample every ~40m
        val currentDist = playerState.currentTrackDistance
        if (telemetryHistory.isEmpty() || (currentDist - telemetryHistory.last().distanceM) > 40f) {
            telemetryHistory.add(
                TelemetrySample(
                    distanceM = currentDist % circuit.totalLengthMeters,
                    speedKph = playerState.speedKph,
                    throttle = playerState.throttleInput,
                    brake = playerState.brakeInput,
                    gear = playerState.gear,
                    lateralG = playerState.lateralG,
                    rpm = playerState.rpm
                )
            )
            if (telemetryHistory.size > 200) telemetryHistory.removeAt(0)
        }

        // 5. Track Limits & Track-Cutting Penalty System
        val nearestPt = circuit.findNearestSplinePointNearDistance(
            playerState.position.x,
            playerState.position.y,
            currentDist
        )
        val isCuttingCorner = playerState.isOffTrack && (nearestPt.curbLeft || nearestPt.curbRight) && playerState.speedKph > 65f && !isPitStopping
        val isSustainedOffTrackShortcut = playerState.isOffTrack && playerState.speedKph > 120f && !isPitStopping
        if (isCuttingCorner || isSustainedOffTrackShortcut) {
            offTrackCutTimerSec += clampedDt
            if (offTrackCutTimerSec >= 1.1f) {
                offTrackCutTimerSec = 0f
                cornerCutWarnings++
                if (cornerCutWarnings <= 2) {
                    radio.triggerMessage(
                        RadioMessageType.STRATEGY,
                        "TRACK LIMITS: Warning $cornerCutWarnings/3 — keep inside track boundaries."
                    )
                } else {
                    penaltySeconds += 5.0f
                    radio.triggerMessage(
                        RadioMessageType.ALERT,
                        "TRACK CUT PENALTY: +5.0s added to final race time (Total +${penaltySeconds.toInt()}s)."
                    )
                    triggerHaptic(75)
                }
            }
        } else {
            offTrackCutTimerSec = (offTrackCutTimerSec - clampedDt * 0.5f).coerceAtLeast(0f)
        }

        // 6. Sequential Sector Checkpoints & Validated Lap Counting
        currentSector = nearestPt.sector.coerceIn(1, 3)
        val secIdx = currentSector - 1
        if (secIdx == 0 || playerCheckpointsCleared[secIdx - 1]) {
            playerCheckpointsCleared[secIdx] = true
        }

        if (currentSector != lastSector) {
            when (lastSector) {
                1 -> sector1TimeMs = currentLapTimeMs
                2 -> sector2TimeMs = currentLapTimeMs - sector1TimeMs
            }
            lastSector = currentSector
        }

        val totalTrackLen = circuit.totalLengthMeters
        if (currentDist >= totalTrackLen * currentLap &&
            playerCheckpointsCleared[0] &&
            playerCheckpointsCleared[1] &&
            playerCheckpointsCleared[2]
        ) {
            lastLapTimeMs = currentLapTimeMs
            sector3TimeMs = currentLapTimeMs - (sector1TimeMs + sector2TimeMs)

            if (bestLapTimeMs == 0L || lastLapTimeMs < bestLapTimeMs) {
                bestLapTimeMs = lastLapTimeMs
                radio.triggerMessage(RadioMessageType.PACE, "PURPLE LAP! You set the fastest lap of the race.")
            } else {
                radio.triggerMessage(RadioMessageType.STRATEGY, "Lap $currentLap done. Good consistency, maintain tire temps.")
            }

            currentLap++
            lapStartTimeMs = raceElapsedMs
            lastSector = 1
            playerCheckpointsCleared[0] = true
            playerCheckpointsCleared[1] = false
            playerCheckpointsCleared[2] = false
            triggerHaptic(100)

            if (currentLap > totalLaps) {
                playerFinishTimeMs = raceElapsedMs
                isRaceFinished = true
                updateRacePositions()
                val penaltyNote = if (penaltySeconds > 0f) " (+${penaltySeconds.toInt()}s pen)" else ""
                radio.triggerMessage(RadioMessageType.PACE, "CHEQUERED FLAG! P$playerRacePosition finish$penaltyNote!")
            }
        }

        // Live Delta calculation
        val expectedTimeAtDist = ((currentDist % totalTrackLen) / totalTrackLen.coerceAtLeast(1f)) * (bestLapTimeMs / 1000f)
        deltaVsBestSec = (currentLapTimeMs / 1000f) - expectedTimeAtDist

        // 7. Update Finishing / Live Positions
        updateRacePositions()

        // 8. Audio Parameter Sync
        audio.targetRpm = playerState.rpm
        audio.throttle = playerState.throttleInput
        audio.isBraking = playerState.brakeInput > 0.1f
        audio.tireSlip = abs(playerState.lateralG) * 0.25f

        // 9. Pit Stop Box Logic & Full Structural Repair
        if (isPitStopping) {
            pitStopProgress += clampedDt * 0.4f
            if (pitStopProgress >= 1.0f) {
                isPitStopping = false
                pitStopProgress = 0f
                playerState.tireWear = floatArrayOf(0.01f, 0.01f, 0.01f, 0.01f)
                playerState.damageFrontWing = 0f
                playerState.damageAeroFloor = 0f
                playerState.damageEngine = 0f
                carSetup.fuelLiters = 45f
                radio.triggerMessage(RadioMessageType.STRATEGY, "Pit stop complete: fresh tires & repairs fitted!")
            }
        }

        if (playerState.isOffTrack) {
            triggerHaptic(20)
        }
    }

    fun getStandings(): List<RaceStandingEntry> {
        val totalLen = circuit.totalLengthMeters.coerceAtLeast(100f)
        val entries = mutableListOf<RaceStandingEntry>()

        val playerEffectiveProgress = if (isRaceFinished) {
            totalLaps * totalLen + totalLen
        } else {
            playerState.currentTrackDistance
        }
        entries.add(
            RaceStandingEntry(
                position = 1,
                driverName = driverProfile.driverName,
                shortCode = driverProfile.driverName.take(3).uppercase(),
                teamName = driverProfile.liveryTheme.title,
                number = driverProfile.driverNumber,
                isPlayer = true,
                completedLaps = (currentLap - 1).coerceIn(0, totalLaps),
                totalProgressMeters = playerEffectiveProgress,
                finished = isRaceFinished,
                completionTimeMs = if (isRaceFinished) playerFinishTimeMs else raceElapsedMs,
                penaltySeconds = penaltySeconds
            )
        )

        for (ai in aiEngine.aiRivals) {
            entries.add(
                RaceStandingEntry(
                    position = 1,
                    driverName = ai.driver.name,
                    shortCode = ai.driver.shortCode,
                    teamName = ai.driver.team,
                    number = ai.driver.number,
                    isPlayer = false,
                    completedLaps = (ai.currentLap - 1).coerceIn(0, totalLaps),
                    totalProgressMeters = if (ai.finishedRace) totalLaps * totalLen + totalLen else ai.trackDistance,
                    finished = ai.finishedRace,
                    completionTimeMs = if (ai.finishedRace) ai.finishTimeMs else raceElapsedMs,
                    penaltySeconds = 0f
                )
            )
        }

        // Sort: finished cars sorted by totalAdjustedTimeMs (completion + penalty); unfinished by totalProgressMeters desc
        val sorted = entries.sortedWith { a, b ->
            when {
                a.finished && b.finished -> a.totalAdjustedTimeMs.compareTo(b.totalAdjustedTimeMs)
                a.finished != b.finished -> if (a.finished) -1 else 1
                else -> b.totalProgressMeters.compareTo(a.totalProgressMeters)
            }
        }

        return sorted.mapIndexed { index, item -> item.copy(position = index + 1) }
    }

    private fun updateRacePositions() {
        val standings = getStandings()
        val playerEntry = standings.firstOrNull { it.isPlayer }
        if (playerEntry != null) {
            playerRacePosition = playerEntry.position
        }
    }

    private fun triggerHaptic(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun triggerPitStop() {
        if (!isPitStopping) {
            isPitStopping = true
            pitStopProgress = 0f
            playerState.speedMs = 0f
        }
    }

    fun stopAudio() {
        audio.stop()
    }
}
