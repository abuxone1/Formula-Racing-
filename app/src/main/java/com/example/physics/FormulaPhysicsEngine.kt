package com.example.physics

import com.example.ai.AICar
import com.example.model.*
import kotlin.math.*

/**
 * Unified Formula Racing vehicle dynamics engine:
 * - Powertrain torque curves (EngineSpec), 8-speed gearbox + MGU-K hybrid ERS boost
 * - Adjustable top-speed governor, realistic braking & carbon-ceramic thermal curves
 * - Dynamic longitudinal & lateral weight transfer across axles and tires
 * - Pacejka-inspired tire grip, thermal window, asymmetric per-wheel wear & lockup
 * - Speed-sensitive smoothed steering response
 * - Barrier & Car-to-Car impulse collisions, structural damage, and track recovery
 */
class FormulaPhysicsEngine(
    val circuit: CircuitTrack
) {
    // 8-speed gear ratios + final drive
    private val gearRatios = floatArrayOf(0f, 3.25f, 2.50f, 2.02f, 1.66f, 1.41f, 1.23f, 1.09f, 0.97f)
    private val finalDrive = 3.85f
    private val wheelRadius = 0.36f // 18-inch Formula wheels + tire sidewall
    private val wheelbaseM = 3.60f
    private val trackWidthM = 1.76f
    private val cgHeightM = 0.32f

    // Driving assists settings
    var absEnabled: Boolean = true
    var tractionControlEnabled: Boolean = true
    var steeringAssistEnabled: Boolean = true
    var automaticTransmission: Boolean = true

    fun shiftUp(state: VehicleState) {
        if (state.gear < 8) {
            state.gear++
            state.exhaustPopTimer = 0.14f
        }
    }

    fun shiftDown(state: VehicleState) {
        if (state.gear > 1) {
            state.gear--
            state.exhaustPopTimer = 0.18f
        }
    }

    fun recoverToTrack(state: VehicleState) {
        val nearest = circuit.findNearestSplinePointNearDistance(
            state.position.x,
            state.position.y,
            state.currentTrackDistance
        )
        state.position.x = nearest.position.x
        state.position.y = nearest.position.y
        state.position.z = nearest.position.z
        state.headingRad = atan2(nearest.tangent.y, nearest.tangent.x)
        state.speedMs = state.speedMs.coerceAtMost(14f)
        state.pitchRad = 0f
        state.rollRad = 0f
        state.lateralG = 0f
        state.longitudinalG = 0f
        state.effectiveSteer = 0f
        state.isOffTrack = false
        state.isOnCurb = false
        state.offTrackTimeAccumSec = 0f
        state.recoveryCooldownSec = 2.0f
    }

    fun step(
        state: VehicleState,
        setup: CarSetup,
        weather: WeatherCondition,
        dt: Float
    ) {
        val clampedDt = dt.coerceIn(0.005f, 0.05f)
        state.impactCooldownTimer = (state.impactCooldownTimer - clampedDt).coerceAtLeast(0f)
        state.recoveryCooldownSec = (state.recoveryCooldownSec - clampedDt).coerceAtLeast(0f)

        val carMass = 798f + setup.fuelLiters
        val carSpeed = state.speedMs

        // 1. Aerodynamics (with front-wing & aero-floor damage effects)
        val airDensity = 1.225f
        val aeroSpeedSq = carSpeed * carSpeed
        val aeroHealth = (1f - state.damageFrontWing * 0.42f - state.damageAeroFloor * 0.25f).coerceIn(0.35f, 1.0f)
        val baseDownforceCoeff = 1.45f * setup.downforceScalar * aeroHealth
        val downforceNewtons = 0.5f * airDensity * baseDownforceCoeff * 2.2f * aeroSpeedSq

        var dragCoeff = 0.72f * setup.dragScalar * (1f + state.damageAeroFloor * 0.30f)
        if (state.drsOpen) {
            dragCoeff *= 0.70f // 30% drag reduction with DRS open
        }
        val dragNewtons = 0.5f * airDensity * dragCoeff * 1.6f * aeroSpeedSq

        // 2. Track Boundaries, Surface Detection & Barrier Clamp
        val nearestSpline = circuit.findNearestSplinePointNearDistance(
            state.position.x,
            state.position.y,
            state.currentTrackDistance
        )
        var lateralOffset = circuit.getLateralOffset(state.position.x, state.position.y, nearestSpline)
        val halfWidth = circuit.widthMeters * 0.5f
        val barrierLimit = halfWidth + 4.8f

        if (abs(lateralOffset) > barrierLimit) {
            // Barrier collision: clamp back inside guardrail, scrub speed, deflect heading, apply damage
            val sign = if (lateralOffset > 0f) 1f else -1f
            val excess = abs(lateralOffset) - barrierLimit
            state.position.x -= nearestSpline.normal.x * sign * excess
            state.position.y -= nearestSpline.normal.y * sign * excess
            lateralOffset = sign * barrierLimit

            val trackHeading = atan2(nearestSpline.tangent.y, nearestSpline.tangent.x)
            val angleDelta = normalizeAngle(trackHeading - state.headingRad)
            state.headingRad = normalizeAngle(state.headingRad + angleDelta * 0.55f)

            val impactSpeed = state.speedMs * (0.28f + abs(sin(angleDelta)) * 0.55f)
            state.speedMs *= 0.72f
            if (state.impactCooldownTimer <= 0f && impactSpeed > 4.5f) {
                applyStructuralDamage(state, impactSpeed, setup.engineSpec.durability, isFront = abs(angleDelta) > 0.35f)
                state.impactCooldownTimer = 0.45f
            }
        }

        val onCurb = abs(lateralOffset) in (halfWidth * 0.90f)..(halfWidth * 1.25f)
        val isOff = abs(lateralOffset) > (halfWidth * 1.25f)
        state.isOnCurb = onCurb
        state.isOffTrack = isOff
        if (isOff) {
            state.offTrackTimeAccumSec += clampedDt
        } else {
            state.offTrackTimeAccumSec = 0f
        }

        // DRS Zone Detection
        state.drsAvailable = nearestSpline.drsZone && !state.isOffTrack && state.damageAeroFloor < 0.75f
        if (!state.drsAvailable && state.drsOpen) {
            state.drsOpen = false
        }
        if (state.brakeInput > 0.05f) {
            state.drsOpen = false
        }

        // 3. Surface, Weather, Thermal & Tire Wear Friction Factors
        val surfaceGrip = when {
            isOff -> 0.44f  // Grass / gravel runoff
            onCurb -> 0.88f // Rumble kerbs
            else -> 1.0f
        }

        val compound = setup.tireCompound
        val weatherGrip = when (weather) {
            WeatherCondition.HEAVY_RAIN, WeatherCondition.RAIN_WET -> if (compound == TireCompound.WET) 0.92f else 0.48f
            WeatherCondition.WET_TRACK -> if (compound == TireCompound.WET) 0.96f else 0.72f
            WeatherCondition.FOGGY -> 0.94f
            WeatherCondition.CLOUDY -> 0.98f
            WeatherCondition.SUNSET -> 0.99f
            WeatherCondition.NIGHT_GP -> 0.96f
            WeatherCondition.DRY_SUNNY -> if (compound == TireCompound.WET) 0.83f else 1.0f
        }

        // Average tire thermal window penalty (cold or severely overheated tires lose grip)
        val avgTemp = (state.tireTemps[0] + state.tireTemps[1] + state.tireTemps[2] + state.tireTemps[3]) * 0.25f
        val tempDelta = abs(avgTemp - compound.optimumTempC)
        val thermalGripFactor = (1.0f - ((tempDelta - 14f).coerceAtLeast(0f) / 140f)).coerceIn(0.84f, 1.0f)

        val avgWear = (state.tireWear[0] + state.tireWear[1] + state.tireWear[2] + state.tireWear[3]) * 0.25f
        val wearGripFactor = (1.0f - avgWear * 0.38f).coerceIn(0.55f, 1.0f)

        val tireGripFactor = compound.gripMultiplier * wearGripFactor * thermalGripFactor * surfaceGrip * weatherGrip

        // 4. Powertrain (EngineSpec) & 8-Speed Gearbox
        val engineSpec = setup.engineSpec
        val currentGear = state.gear.coerceIn(1, 8)
        val totalRatio = gearRatios[currentGear] * finalDrive
        val wheelAngularVelocity = carSpeed / wheelRadius
        val engineAngularVelocity = wheelAngularVelocity * totalRatio
        val calculatedRpm = (engineAngularVelocity * (60f / (2f * PI.toFloat()))).coerceAtLeast(4200f)
        state.rpm = calculatedRpm.coerceIn(4000f, engineSpec.redlineRpm)

        state.exhaustPopTimer = (state.exhaustPopTimer - clampedDt).coerceAtLeast(0f)
        if (automaticTransmission) {
            val upshiftRpm = engineSpec.redlineRpm * 0.915f
            val downshiftRpm = 6400f
            if (state.rpm >= upshiftRpm && currentGear < 8 && state.throttleInput > 0.25f) {
                state.gear++
                state.exhaustPopTimer = 0.14f
            } else if (state.rpm < downshiftRpm && currentGear > 1) {
                state.gear--
                state.exhaustPopTimer = 0.18f
            }
        }
        if (state.rpm > engineSpec.redlineRpm * 0.97f && state.throttleInput > 0.7f) {
            state.exhaustPopTimer = state.exhaustPopTimer.coerceAtLeast(0.08f)
        }

        // Engine torque curve shaped by EngineSpec & engine health
        val rpmRange = (engineSpec.redlineRpm - 4000f).coerceAtLeast(8000f)
        val rpmNorm = ((state.rpm - 4000f) / rpmRange).coerceIn(0f, 1f)
        val torqueCurve = 0.72f + sin(rpmNorm * PI.toFloat() * 0.92f) * 0.32f
        val engineHealth = (1f - state.damageEngine * 0.45f).coerceIn(0.45f, 1.0f)
        var driveTorque = engineSpec.peakTorqueNm * torqueCurve * engineHealth * state.throttleInput

        // MGU-K Hybrid Electric Boost (engineSpec dependent)
        if (state.ersDeploying && state.ersEnergy > 0.02f && state.throttleInput > 0.15f) {
            driveTorque += engineSpec.mgukTorqueNm
            state.ersEnergy = (state.ersEnergy - 0.05f * clampedDt).coerceAtLeast(0f)
        }

        var driveForce = (driveTorque * totalRatio * 0.92f) / wheelRadius

        // Rear wheel longitudinal traction limit (wheelspin check)
        val maxRearTraction = state.rearAxleLoadN * 1.80f * tireGripFactor
        var wheelspinFactor = 0f
        if (driveForce > maxRearTraction && state.throttleInput > 0.2f) {
            if (tractionControlEnabled) {
                driveForce = maxRearTraction * 0.95f
            } else {
                wheelspinFactor = ((driveForce - maxRearTraction) / maxRearTraction.coerceAtLeast(1f)).coerceIn(0f, 1f)
                driveForce = maxRearTraction * (0.84f - wheelspinFactor * 0.12f)
            }
        }

        // 5. Adjustable Max Speed & Pit Speed Limiter Governor
        val targetMaxSpeedMs = if (state.isPitLimiterOn) {
            22.22f // 80 km/h pit speed limiter
        } else {
            (setup.maxSpeedKph / 3.6f).coerceIn(55f, 108f)
        }
        if (carSpeed >= targetMaxSpeedMs * 0.985f) {
            val overshoot = ((carSpeed - targetMaxSpeedMs * 0.985f) / (targetMaxSpeedMs * 0.015f)).coerceIn(0f, 1.5f)
            driveForce *= (1f - overshoot).coerceAtLeast(0f)
        }

        // 6. Realistic Braking, Carbon-Ceramic Thermal Bite & ABS
        val brakeBiasFront = (setup.brakeBias / 100f).coerceIn(0.48f, 0.65f)
        val brakeThermalMult = when {
            state.brakeDiscTemp < 320f -> 0.86f // Cold carbon discs
            state.brakeDiscTemp > 980f -> 0.84f // Overheated brake fade
            else -> 1.00f
        }
        val maxTireBrakeForce = (state.frontAxleLoadN + state.rearAxleLoadN) * 2.05f * tireGripFactor
        val rawBrakeDemand = state.brakeInput * 36000f * brakeThermalMult
        var brakeLockupSeverity = 0f
        var effectiveBrakeForce = min(rawBrakeDemand, maxTireBrakeForce)

        if (!absEnabled && state.brakeInput > 0.84f && carSpeed > 6f) {
            // Wheel lockup: braking efficiency drops and front tires scrub heavily
            brakeLockupSeverity = ((state.brakeInput - 0.84f) / 0.16f).coerceIn(0.2f, 1f)
            effectiveBrakeForce *= (1f - 0.22f * brakeLockupSeverity)
        }

        // MGU-K Hybrid Energy Harvesting under braking
        if (state.brakeInput > 0.1f && carSpeed > 8f && state.ersEnergy < 1.0f) {
            state.ersEnergy = (state.ersEnergy + 0.045f * state.brakeInput * clampedDt).coerceAtMost(1.0f)
        }

        // 7. Total Longitudinal Acceleration & Smooth Stop
        val rollingResistance = (165f + carSpeed * 4.5f) * (if (isOff) 2.4f else 1.0f)
        val netLongitudinalForce = driveForce - effectiveBrakeForce - dragNewtons - rollingResistance
        var accelerationMs2 = netLongitudinalForce / carMass

        var nextSpeedMs = (carSpeed + accelerationMs2 * clampedDt).coerceAtLeast(0f)
        if (nextSpeedMs > targetMaxSpeedMs && state.isPitLimiterOn) {
            nextSpeedMs = (carSpeed - 28f * clampedDt).coerceAtLeast(targetMaxSpeedMs)
        } else if (nextSpeedMs > targetMaxSpeedMs * 1.015f) {
            nextSpeedMs = targetMaxSpeedMs * 1.015f
        }
        if (carSpeed > 0.01f || nextSpeedMs > 0.01f) {
            accelerationMs2 = (nextSpeedMs - carSpeed) / clampedDt
        } else {
            accelerationMs2 = 0f
        }
        state.speedMs = nextSpeedMs
        state.longitudinalG = accelerationMs2 / 9.81f

        // Fuel consumption scaled by EngineSpec burn rate
        setup.fuelLiters = (setup.fuelLiters - state.throttleInput * engineSpec.fuelBurnRate * clampedDt).coerceAtLeast(0.5f)

        // 8. Steering Sensitivity, Smooth Actuator & Pacejka Cornering Grip
        val clampedSteerInput = state.steerInput.coerceIn(-1f, 1f)
        val steerRate = if (steeringAssistEnabled) 9.5f else 14.0f
        val steerAlpha = (steerRate * clampedDt).coerceIn(0f, 1f)
        state.effectiveSteer += (clampedSteerInput - state.effectiveSteer) * steerAlpha

        val speedReduction = (1.0f - (nextSpeedMs / 105f).coerceIn(0f, 0.68f))
        val sensitivityScale = setup.steeringSensitivity.coerceIn(0.5f, 1.6f)
        val maxSteerAngleRad = (0.50f * speedReduction * sensitivityScale).coerceIn(0.06f, 0.68f)
        val actualSteerAngle = state.effectiveSteer * maxSteerAngleRad

        val kinematicYawRate = (nextSpeedMs / wheelbaseM) * tan(actualSteerAngle)
        val totalNormalLoad = (carMass * 9.81f) + downforceNewtons
        val maxLateralForce = totalNormalLoad * 1.88f * tireGripFactor
        val centripetalReqForce = carMass * nextSpeedMs * abs(kinematicYawRate)
        val gripRatio = if (maxLateralForce > 1f) (centripetalReqForce / maxLateralForce) else 0f

        val actualYawRate = if (gripRatio <= 1.0f) {
            kinematicYawRate
        } else {
            kinematicYawRate * (1.0f / gripRatio.coerceAtLeast(1.0f))
        }

        state.headingRad = normalizeAngle(state.headingRad + actualYawRate * clampedDt)
        val lateralAccelMs2 = nextSpeedMs * actualYawRate
        state.lateralG = lateralAccelMs2 / 9.81f

        // 9. Suspension Dynamics & Weight Transfer (Longitudinal + Lateral)
        val aeroBalanceFront = (0.43f + (setup.frontWingAngle - setup.rearWingAngle) * 0.004f).coerceIn(0.36f, 0.52f)
        val staticFrontLoad = carMass * 9.81f * 0.46f + downforceNewtons * aeroBalanceFront
        val staticRearLoad = carMass * 9.81f * 0.54f + downforceNewtons * (1f - aeroBalanceFront)
        val longTransferN = (carMass * accelerationMs2 * cgHeightM) / wheelbaseM
        // Acceleration (+longAccel) transfers load to REAR; Braking (-longAccel) transfers load to FRONT
        state.frontAxleLoadN = (staticFrontLoad - longTransferN).coerceIn(1200f, totalNormalLoad * 0.85f)
        state.rearAxleLoadN = (staticRearLoad + longTransferN).coerceIn(1200f, totalNormalLoad * 0.85f)

        val latTransferRatio = ((carMass * lateralAccelMs2 * cgHeightM) / (trackWidthM * totalNormalLoad.coerceAtLeast(1f))).coerceIn(-0.35f, 0.35f)
        state.lateralLoadRatio = (0.5f + latTransferRatio).coerceIn(0.15f, 0.85f)

        val stiffnessDamp = setup.springStiffness.coerceIn(0.7f, 1.3f)
        val targetPitch = (-state.longitudinalG * (0.028f / stiffnessDamp)).coerceIn(-0.075f, 0.075f)
        val targetRoll = (state.lateralG * (0.038f / stiffnessDamp)).coerceIn(-0.09f, 0.09f)
        state.pitchRad += (targetPitch - state.pitchRad) * 0.25f
        state.rollRad += (targetRoll - state.rollRad) * 0.25f

        // 10. Update World Position & Directional Track Distance
        val forwardX = cos(state.headingRad)
        val forwardY = sin(state.headingRad)
        state.position.x += forwardX * nextSpeedMs * clampedDt
        state.position.y += forwardY * nextSpeedMs * clampedDt
        state.position.z = nearestSpline.position.z

        // Only advance track progress cleanly along the track spline direction
        val alongTangent = forwardX * nearestSpline.tangent.x + forwardY * nearestSpline.tangent.y
        state.currentTrackDistance = (state.currentTrackDistance + alongTangent * nextSpeedMs * clampedDt).coerceAtLeast(0f)
        state.wheelSpinAngleRad = (state.wheelSpinAngleRad + wheelAngularVelocity * clampedDt) % (2f * PI.toFloat())

        state.suspensionBounceOffset = when {
            isOff && nextSpeedMs > 4f -> sin(state.currentTrackDistance * 4.5f) * (0.055f / stiffnessDamp)
            onCurb && nextSpeedMs > 4f -> sin(state.currentTrackDistance * 8.0f) * (0.025f / stiffnessDamp)
            else -> sin(state.currentTrackDistance * 0.4f) * (nextSpeedMs / 100f) * 0.008f
        }

        // 11. Asymmetric Tire Wear & Thermal Modeling (FL=0, FR=1, RL=2, RR=3)
        val distanceKm = (nextSpeedMs * clampedDt) / 1000f
        val leftWeightMult = (1.0f - (state.lateralLoadRatio - 0.5f) * 1.4f).coerceIn(0.55f, 1.55f)
        val rightWeightMult = (1.0f + (state.lateralLoadRatio - 0.5f) * 1.4f).coerceIn(0.55f, 1.55f)
        val surfaceWearMult = if (isOff) 2.2f else if (onCurb) 1.35f else 1.0f

        val baseWear = distanceKm * compound.wearRatePerKm * (1.0f + abs(state.lateralG) * 0.45f) * surfaceWearMult
        val lockupWear = brakeLockupSeverity * brakeBiasFront * 0.006f * clampedDt
        val spinWear = wheelspinFactor * 0.005f * clampedDt

        state.tireWear[0] = (state.tireWear[0] + baseWear * leftWeightMult + lockupWear).coerceAtMost(1.0f)
        state.tireWear[1] = (state.tireWear[1] + baseWear * rightWeightMult + lockupWear).coerceAtMost(1.0f)
        state.tireWear[2] = (state.tireWear[2] + baseWear * leftWeightMult + spinWear).coerceAtMost(1.0f)
        state.tireWear[3] = (state.tireWear[3] + baseWear * rightWeightMult + spinWear).coerceAtMost(1.0f)

        val tempTargetBase = compound.optimumTempC + (abs(state.lateralG) * 6.5f) + (state.speedKph / 52f)
        state.tireTemps[0] += ((tempTargetBase * leftWeightMult.coerceIn(0.92f, 1.12f) + brakeLockupSeverity * 22f) - state.tireTemps[0]) * 0.025f
        state.tireTemps[1] += ((tempTargetBase * rightWeightMult.coerceIn(0.92f, 1.12f) + brakeLockupSeverity * 22f) - state.tireTemps[1]) * 0.025f
        state.tireTemps[2] += ((tempTargetBase * leftWeightMult.coerceIn(0.92f, 1.12f) + wheelspinFactor * 18f) - state.tireTemps[2]) * 0.025f
        state.tireTemps[3] += ((tempTargetBase * rightWeightMult.coerceIn(0.92f, 1.12f) + wheelspinFactor * 18f) - state.tireTemps[3]) * 0.025f

        // Brake disc heating & cooling airflow
        val brakeHeatTarget = 280f + (state.brakeInput * 660f)
        val coolingRate = if (state.brakeInput > 0.05f) 0.09f else 0.035f
        state.brakeDiscTemp += (brakeHeatTarget - state.brakeDiscTemp) * coolingRate
    }

    /**
     * Resolves 2D/3D car-to-car collisions between the player car and AI rivals,
     * exchanging momentum, separating overlapping footprints, and applying impact damage.
     * Returns true if a significant collision occurred this step.
     */
    fun resolveVehicleCollisions(
        playerState: VehicleState,
        setup: CarSetup,
        aiRivals: List<AICar>
    ): Boolean {
        val collisionRadius = 2.25f
        val minDistSq = collisionRadius * collisionRadius
        var collided = false

        val pFwdX = cos(playerState.headingRad)
        val pFwdY = sin(playerState.headingRad)
        val pVx = pFwdX * playerState.speedMs
        val pVy = pFwdY * playerState.speedMs

        for (ai in aiRivals) {
            val dx = playerState.position.x - ai.position.x
            val dy = playerState.position.y - ai.position.y
            val distSq = dx * dx + dy * dy
            if (distSq >= minDistSq) continue

            val dist = sqrt(distSq).coerceAtLeast(0.05f)
            val nx = dx / dist
            val ny = dy / dist
            val overlap = collisionRadius - dist

            // Separate player and AI along collision normal
            playerState.position.x += nx * overlap * 0.60f
            playerState.position.y += ny * overlap * 0.60f
            ai.position.x -= nx * overlap * 0.40f
            ai.position.y -= ny * overlap * 0.40f

            val aiFwdX = cos(ai.headingRad)
            val aiFwdY = sin(ai.headingRad)
            val aVx = aiFwdX * ai.speedMs
            val aVy = aiFwdY * ai.speedMs

            val relVx = pVx - aVx
            val relVy = pVy - aVy
            val closingSpeed = -(relVx * nx + relVy * ny)

            if (closingSpeed > 0f) {
                val restitution = 0.35f
                val impulse = (1f + restitution) * closingSpeed * 0.5f

                val newPVx = pVx + nx * impulse
                val newPVy = pVy + ny * impulse
                val newAVx = aVx - nx * impulse
                val newAVy = aVy - ny * impulse

                playerState.speedMs = sqrt(newPVx * newPVx + newPVy * newPVy).coerceAtLeast(0f) * 0.92f
                ai.speedMs = sqrt(newAVx * newAVx + newAVy * newAVy).coerceAtLeast(0f) * 0.92f

                // Subtle yaw deflection on player & lateral nudge on AI
                if (playerState.speedMs > 2f) {
                    val targetHeading = atan2(newPVy, newPVx)
                    val yawShift = normalizeAngle(targetHeading - playerState.headingRad)
                    playerState.headingRad = normalizeAngle(playerState.headingRad + yawShift * 0.25f)
                }

                // Track lateral shift on AI car so it avoids sticking
                val spline = circuit.findNearestSplinePointNearDistance(ai.position.x, ai.position.y, ai.trackDistance)
                ai.lateralOffset = circuit.getLateralOffset(ai.position.x, ai.position.y, spline)
                    .coerceIn(-circuit.widthMeters * 0.45f, circuit.widthMeters * 0.45f)

                if (playerState.impactCooldownTimer <= 0f && closingSpeed > 3.5f) {
                    val isFrontHit = (pFwdX * -nx + pFwdY * -ny) > 0.35f
                    applyStructuralDamage(playerState, closingSpeed, setup.engineSpec.durability, isFront = isFrontHit)
                    playerState.impactCooldownTimer = 0.40f
                }
                collided = true
            }
        }
        return collided
    }

    private fun applyStructuralDamage(
        state: VehicleState,
        impactSpeedMs: Float,
        durability: Float,
        isFront: Boolean
    ) {
        val severity = ((impactSpeedMs - 3.0f) / 36f).coerceIn(0.03f, 0.45f) / durability.coerceAtLeast(0.75f)
        if (isFront) {
            state.damageFrontWing = (state.damageFrontWing + severity * 0.85f).coerceAtMost(1.0f)
        } else {
            state.damageAeroFloor = (state.damageAeroFloor + severity * 0.65f).coerceAtMost(1.0f)
        }
        if (impactSpeedMs > 16f) {
            state.damageEngine = (state.damageEngine + severity * 0.35f).coerceAtMost(1.0f)
        }
    }

    private fun normalizeAngle(rad: Float): Float {
        var a = rad
        val twoPi = (2.0 * PI).toFloat()
        while (a > PI) a -= twoPi
        while (a < -PI) a += twoPi
        return a
    }
}
