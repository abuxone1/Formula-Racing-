package com.example.renderer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.ai.AICar
import com.example.model.CameraMode
import com.example.model.CarSetup
import com.example.model.DriverProfile
import com.example.model.GraphicsQuality
import com.example.model.RaceIntroPhase
import com.example.model.TireCompound
import com.example.model.Vector3D
import com.example.model.VehicleState
import com.example.model.WeatherCondition
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class Formula3DRenderer {

    private val camera = Camera3D()
    private val textures = FormulaTextures()
    private val particleSystem = FormulaParticleSystem()
    private val carMesh = FormulaCar3DMesh()
    private val environmentRenderer = FormulaEnvironmentRenderer()

    private val sunDirTmp = FloatArray(3)
    private val camSpaceTmp = CamPoint3D()

    // Pre-allocated Z-sort table for player (-1) + AI cars (0..aiRivals.size-1)
    private val maxRenderVehicles = 12
    private val vehicleIndices = IntArray(maxRenderVehicles)
    private val vehicleDepths = FloatArray(maxRenderVehicles)

    // Reusable vectors for camera interpolation during cinematic intro
    private val raceCamPos = Vector3D()
    private val raceCamTarget = Vector3D()
    private var raceCamFov = 70f

    fun render(
        drawScope: DrawScope,
        playerState: VehicleState,
        setup: CarSetup,
        circuit: com.example.model.CircuitTrack,
        aiRivals: List<AICar>,
        cameraMode: CameraMode,
        weather: WeatherCondition,
        startLightsState: Int,
        driverProfile: DriverProfile = DriverProfile(),
        quality: GraphicsQuality = GraphicsQuality.MEDIUM,
        dt: Float = 0.016f,
        introPhase: RaceIntroPhase = RaceIntroPhase.NONE,
        introElapsedSec: Float = 0f,
        useIntroCamera: Boolean = false
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        if (width <= 0f || height <= 0f) return

        // 1. Ensure GPU-friendly cached textures match active graphics tier
        textures.ensureInitialized(quality.textureResolution)

        // 2. Position Camera (either cinematic race intro or dynamic gameplay cam)
        val isIntroRendering = useIntroCamera && introPhase != RaceIntroPhase.NONE
        if (isIntroRendering) {
            updateIntroCamera(playerState, circuit, cameraMode, introPhase, introElapsedSec)
        } else {
            updateRaceCamera(playerState, cameraMode)
            camera.position = raceCamPos
            camera.target = raceCamTarget
            camera.fovDegrees = raceCamFov
            camera.updateVectors()
        }

        // 3. Determine Directional Light Vector based on weather
        computeSunDirection(weather, sunDirTmp)

        // 4. Sky, Sun/Stars, Skyline & Distant Horizon
        environmentRenderer.drawSkyAndAtmosphere(
            drawScope = drawScope,
            width = width,
            height = height,
            weather = weather,
            trackLevel = circuit.currentLevel,
            camera = camera,
            quality = quality
        )

        // 5. Textured Track Surface, Kerbs, Grid, Barriers, Grandstands, Spectators & Start Gantry
        val focusDistance = if (isIntroRendering && introPhase == RaceIntroPhase.CIRCUIT_FLYOVER) {
            16f
        } else {
            playerState.currentTrackDistance
        }
        environmentRenderer.drawTrackAndStructures(
            drawScope = drawScope,
            camera = camera,
            textures = textures,
            circuit = circuit,
            focusDistanceMeters = focusDistance,
            weather = weather,
            quality = quality,
            startLightsState = startLightsState,
            width = width,
            height = height
        )

        // 6. Depth-sorted 3D Vehicles (AI Rivals + Player Car)
        val drawPlayerExterior = isIntroRendering || cameraMode != CameraMode.COCKPIT
        drawAllVehiclesSorted(
            drawScope = drawScope,
            playerState = playerState,
            setup = setup,
            driverProfile = driverProfile,
            aiRivals = aiRivals,
            weather = weather,
            quality = quality,
            drawPlayerExterior = drawPlayerExterior,
            width = width,
            height = height
        )

        // 7. Particles (Tire Smoke, Wet Spray, Skid Sparks, Gravel Dust, Exhaust Flames, Rain)
        particleSystem.updateAndRender(
            drawScope = drawScope,
            camera = camera,
            state = playerState,
            weather = weather,
            quality = quality,
            dt = dt,
            width = width,
            height = height
        )

        // 8. Foreground 3D Cockpit View (when active during gameplay/replay)
        if (!isIntroRendering && cameraMode == CameraMode.COCKPIT) {
            carMesh.drawCockpitView(
                drawScope = drawScope,
                textures = textures,
                state = playerState,
                setup = setup,
                profile = driverProfile,
                quality = quality,
                width = width,
                height = height
            )
        }
    }

    private fun drawAllVehiclesSorted(
        drawScope: DrawScope,
        playerState: VehicleState,
        setup: CarSetup,
        driverProfile: DriverProfile,
        aiRivals: List<AICar>,
        weather: WeatherCondition,
        quality: GraphicsQuality,
        drawPlayerExterior: Boolean,
        width: Float,
        height: Float
    ) {
        var count = 0
        val aiCount = aiRivals.size.coerceAtMost(maxRenderVehicles - 1)
        for (i in 0 until aiCount) {
            val ai = aiRivals[i]
            camera.toCameraSpace(ai.position.x, ai.position.y, ai.position.z, camSpaceTmp)
            vehicleIndices[count] = i
            vehicleDepths[count] = camSpaceTmp.z
            count++
        }
        if (drawPlayerExterior && count < maxRenderVehicles) {
            camera.toCameraSpace(playerState.position.x, playerState.position.y, playerState.position.z, camSpaceTmp)
            vehicleIndices[count] = -1
            vehicleDepths[count] = camSpaceTmp.z
            count++
        }

        // Insertion sort back-to-front (largest camZ first)
        for (i in 1 until count) {
            val keyIdx = vehicleIndices[i]
            val keyDepth = vehicleDepths[i]
            var j = i - 1
            while (j >= 0 && vehicleDepths[j] < keyDepth) {
                vehicleIndices[j + 1] = vehicleIndices[j]
                vehicleDepths[j + 1] = vehicleDepths[j]
                j--
            }
            vehicleIndices[j + 1] = keyIdx
            vehicleDepths[j + 1] = keyDepth
        }

        val playerPrimary = Color(driverProfile.liveryTheme.primaryHex)
        val playerAccent = Color(driverProfile.liveryTheme.accentHex)
        val playerHelmet = Color(driverProfile.helmetDesign.primaryHex)
        val playerVisor = Color(driverProfile.helmetDesign.visorTintHex)

        for (i in 0 until count) {
            val depth = vehicleDepths[i]
            if (depth < -3f || depth > quality.lookaheadMeters + 20f) continue

            val id = vehicleIndices[i]
            if (id == -1) {
                carMesh.drawCar3D(
                    drawScope = drawScope,
                    camera = camera,
                    textures = textures,
                    position = playerState.position,
                    headingRad = playerState.headingRad,
                    pitchRad = playerState.pitchRad,
                    rollRad = playerState.rollRad,
                    bounceZ = playerState.suspensionBounceOffset,
                    steerInput = playerState.steerInput,
                    wheelSpinRad = playerState.wheelSpinAngleRad,
                    brakeAmount = playerState.brakeInput,
                    brakeTempC = playerState.brakeDiscTemp,
                    drsOpen = playerState.drsOpen,
                    frontWingDamage = playerState.damageFrontWing,
                    isPitLimiter = playerState.isPitLimiterOn,
                    speedKph = playerState.speedKph,
                    primaryColor = playerPrimary,
                    accentColor = playerAccent,
                    helmetPrimary = playerHelmet,
                    helmetVisor = playerVisor,
                    tireCompound = setup.tireCompound,
                    weather = weather,
                    quality = quality,
                    sunDirX = sunDirTmp[0],
                    sunDirY = sunDirTmp[1],
                    sunDirZ = sunDirTmp[2],
                    width = width,
                    height = height
                )
            } else {
                val ai = aiRivals[id]
                val teamCol = Color(ai.driver.teamColorHex)
                val accentCol = if (id % 2 == 0) Color(0xFF00E5FF) else Color(0xFFFFD600)
                val aiCompound = when (id % 3) {
                    0 -> TireCompound.SOFT
                    1 -> TireCompound.MEDIUM
                    else -> TireCompound.HARD
                }
                val aiSpin = (ai.trackDistance / 0.36f)
                carMesh.drawCar3D(
                    drawScope = drawScope,
                    camera = camera,
                    textures = textures,
                    position = ai.position,
                    headingRad = ai.headingRad,
                    pitchRad = 0f,
                    rollRad = 0f,
                    bounceZ = 0f,
                    steerInput = 0f,
                    wheelSpinRad = aiSpin,
                    brakeAmount = if (ai.speedMs < 2f) 0.4f else 0f,
                    brakeTempC = 450f,
                    drsOpen = false,
                    frontWingDamage = 0f,
                    isPitLimiter = false,
                    speedKph = ai.speedMs * 3.6f,
                    primaryColor = teamCol,
                    accentColor = accentCol,
                    helmetPrimary = Color(0xFFFFB300),
                    helmetVisor = Color(0xFF101828),
                    tireCompound = aiCompound,
                    weather = weather,
                    quality = quality,
                    sunDirX = sunDirTmp[0],
                    sunDirY = sunDirTmp[1],
                    sunDirZ = sunDirTmp[2],
                    width = width,
                    height = height
                )
            }
        }
    }

    private fun updateRaceCamera(playerState: VehicleState, mode: CameraMode) {
        val carX = playerState.position.x
        val carY = playerState.position.y
        val carZ = playerState.position.z
        val heading = playerState.headingRad
        val fwdX = cos(heading)
        val fwdY = sin(heading)
        val rightX = -sin(heading)
        val rightY = cos(heading)

        val latSway = (playerState.lateralG * 0.14f).coerceIn(-0.45f, 0.45f)
        val speedShake = if (playerState.speedKph > 220f) {
            sin((System.currentTimeMillis() % 1000L) * 0.08f) * 0.015f
        } else 0f

        when (mode) {
            CameraMode.CHASE -> {
                val followDist = 7.4f + (playerState.speedMs / 36f).coerceIn(0f, 2.8f)
                val camHeight = 2.45f - (playerState.pitchRad * 2.2f)
                raceCamPos.x = carX - fwdX * followDist + rightX * latSway
                raceCamPos.y = carY - fwdY * followDist + rightY * latSway
                raceCamPos.z = carZ + camHeight + speedShake

                raceCamTarget.x = carX + fwdX * 14f
                raceCamTarget.y = carY + fwdY * 14f
                raceCamTarget.z = carZ + 1.05f
                raceCamFov = 66f + (playerState.speedMs / 18f).coerceIn(0f, 16f)
            }
            CameraMode.COCKPIT -> {
                val eyeFwd = -0.32f
                val eyeHeight = 0.88f + playerState.suspensionBounceOffset
                raceCamPos.x = carX + fwdX * eyeFwd
                raceCamPos.y = carY + fwdY * eyeFwd
                raceCamPos.z = carZ + eyeHeight + speedShake * 0.6f

                val lookAheadShift = playerState.steerInput * 3.5f
                raceCamTarget.x = carX + fwdX * 26f + rightX * lookAheadShift
                raceCamTarget.y = carY + fwdY * 26f + rightY * lookAheadShift
                raceCamTarget.z = carZ + 0.84f
                raceCamFov = 75f + (playerState.speedMs / 30f).coerceIn(0f, 8f)
            }
            CameraMode.T_CAM -> {
                val tDist = -0.72f
                val tHeight = 1.38f + playerState.suspensionBounceOffset * 0.8f
                raceCamPos.x = carX + fwdX * tDist + rightX * (latSway * 0.35f)
                raceCamPos.y = carY + fwdY * tDist + rightY * (latSway * 0.35f)
                raceCamPos.z = carZ + tHeight + speedShake * 0.8f

                raceCamTarget.x = carX + fwdX * 30f
                raceCamTarget.y = carY + fwdY * 30f
                raceCamTarget.z = carZ + 0.72f
                raceCamFov = 72f + (playerState.speedMs / 24f).coerceIn(0f, 10f)
            }
            CameraMode.TV_BROADCAST -> {
                raceCamPos.x = carX + rightX * 14.5f - fwdX * 9.5f
                raceCamPos.y = carY + rightY * 14.5f - fwdY * 9.5f
                raceCamPos.z = carZ + 3.6f
                raceCamTarget.x = carX
                raceCamTarget.y = carY
                raceCamTarget.z = carZ + 0.9f
                raceCamFov = 44f
            }
        }
    }

    private fun updateIntroCamera(
        playerState: VehicleState,
        circuit: com.example.model.CircuitTrack,
        targetMode: CameraMode,
        phase: RaceIntroPhase,
        elapsedSec: Float
    ) {
        updateRaceCamera(playerState, targetMode)

        val carX = playerState.position.x
        val carY = playerState.position.y
        val carZ = playerState.position.z
        val heading = playerState.headingRad
        val fwdX = cos(heading)
        val fwdY = sin(heading)
        val rightX = -sin(heading)
        val rightY = cos(heading)

        when (phase) {
            RaceIntroPhase.CIRCUIT_FLYOVER -> {
                val t = (elapsedSec / 1.6f).coerceIn(0f, 1f)
                val gridPt = circuit.getPointAtDistance(16f)
                val flyFwd = 34f - t * 22f
                val flyRight = -16f + t * 10f
                val flyZ = 15.5f - t * 7.0f

                camera.position = Vector3D(
                    gridPt.position.x + gridPt.tangent.x * flyFwd + gridPt.normal.x * flyRight,
                    gridPt.position.y + gridPt.tangent.y * flyFwd + gridPt.normal.y * flyRight,
                    gridPt.position.z + flyZ
                )
                camera.target = Vector3D(
                    gridPt.position.x + gridPt.tangent.x * 6f,
                    gridPt.position.y + gridPt.tangent.y * 6f,
                    gridPt.position.z + 1.2f
                )
                camera.fovDegrees = 58f
            }
            RaceIntroPhase.GRID_SHOWCASE -> {
                val t = ((elapsedSec - 1.6f) / 1.6f).coerceIn(0f, 1f)
                val angleSweep = (0.35f + t * 1.15f)
                val orbitDist = 7.8f - t * 1.8f
                val ox = cos(heading + angleSweep) * orbitDist
                val oy = sin(heading + angleSweep) * orbitDist

                camera.position = Vector3D(
                    carX + ox,
                    carY + oy,
                    carZ + 1.9f + t * 0.4f
                )
                camera.target = Vector3D(
                    carX + fwdX * 0.5f,
                    carY + fwdY * 0.5f,
                    carZ + 0.65f
                )
                camera.fovDegrees = 52f + t * 10f
            }
            RaceIntroPhase.CAMERA_HANDOFF -> {
                val tRaw = ((elapsedSec - 3.2f) / 1.0f).coerceIn(0f, 1f)
                // Smooth cubic ease-in-out
                val t = tRaw * tRaw * (3f - 2f * tRaw)

                val endAngle = 1.50f
                val startOrbitX = carX + cos(heading + endAngle) * 6.0f
                val startOrbitY = carY + sin(heading + endAngle) * 6.0f
                val startOrbitZ = carZ + 2.3f

                camera.position = Vector3D(
                    lerp(startOrbitX, raceCamPos.x, t),
                    lerp(startOrbitY, raceCamPos.y, t),
                    lerp(startOrbitZ, raceCamPos.z, t)
                )
                camera.target = Vector3D(
                    lerp(carX + fwdX * 0.5f, raceCamTarget.x, t),
                    lerp(carY + fwdY * 0.5f, raceCamTarget.y, t),
                    lerp(carZ + 0.65f, raceCamTarget.z, t)
                )
                camera.fovDegrees = lerp(62f, raceCamFov, t)
            }
            RaceIntroPhase.NONE -> {
                camera.position = raceCamPos
                camera.target = raceCamTarget
                camera.fovDegrees = raceCamFov
            }
        }
        camera.updateVectors()
    }

    private fun computeSunDirection(weather: WeatherCondition, out: FloatArray) {
        when (weather) {
            WeatherCondition.DRY_SUNNY -> {
                out[0] = 0.45f; out[1] = 0.35f; out[2] = 0.82f
            }
            WeatherCondition.SUNSET -> {
                out[0] = 0.72f; out[1] = 0.42f; out[2] = 0.55f
            }
            WeatherCondition.NIGHT_GP -> {
                out[0] = 0.25f; out[1] = 0.25f; out[2] = 0.93f
            }
            WeatherCondition.RAIN_WET, WeatherCondition.HEAVY_RAIN, WeatherCondition.WET_TRACK -> {
                out[0] = 0.30f; out[1] = 0.20f; out[2] = 0.90f
            }
            else -> {
                out[0] = 0.40f; out[1] = 0.30f; out[2] = 0.85f
            }
        }
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
