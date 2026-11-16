package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.career.CareerManager
import com.example.game.RaceSimulationManager
import com.example.model.CameraMode
import com.example.model.GraphicsQuality
import com.example.model.RaceIntroPhase
import com.example.renderer.Formula3DRenderer
import com.example.renderer.GraphicsPerformanceMonitor
import com.example.ui.components.PedalControls
import com.example.ui.components.RacingHUD
import com.example.ui.components.VirtualSteeringControl
import com.example.ui.theme.*
import kotlinx.coroutines.isActive
import java.util.Locale

@Composable
fun RaceScreen(
    sim: RaceSimulationManager,
    careerManager: CareerManager? = null,
    onNavigateBack: () -> Unit,
    onNavigateToReplay: () -> Unit = {},
    onNavigateToTelemetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler {
        sim.stopAudio()
        onNavigateBack()
    }

    val context = LocalContext.current
    val renderer = remember { Formula3DRenderer() }
    val perfMonitor = remember(context) { GraphicsPerformanceMonitor(context) }

    var isPaused by remember { mutableStateOf(false) }
    val frameTickState = remember { mutableLongStateOf(0L) }
    var hudTick by remember { mutableLongStateOf(0L) }
    var lastDt by remember { mutableFloatStateOf(0.016f) }
    var effectiveQuality by remember { mutableStateOf(sim.graphicsQuality) }

    // VSync-paced Simulation & Render Loop with Adaptive Thermal / Battery Framing
    LaunchedEffect(Unit) {
        sim.startRaceSession(playIntro = true)
        var lastSimNs = System.nanoTime()
        var lastHudUpdateNs = lastSimNs

        while (isActive) {
            withFrameNanos { frameNs ->
                val targetIntervalNs = perfMonitor.getTargetFrameIntervalNs(
                    requested = sim.graphicsQuality,
                    adaptiveThermalEnabled = sim.adaptiveThermalControl
                )
                val elapsedNs = frameNs - lastSimNs
                // Throttle frame rate when Low quality or thermal/power-saver active
                if (elapsedNs >= targetIntervalNs - 2_000_000L) {
                    val dt = (elapsedNs / 1_000_000_000.0).toFloat().coerceIn(0.008f, 0.050f)
                    lastSimNs = frameNs
                    lastDt = dt

                    val startWorkNs = System.nanoTime()
                    if (!isPaused) {
                        sim.stepSimulation(dt)
                    }
                    effectiveQuality = perfMonitor.getEffectiveQuality(
                        requested = sim.graphicsQuality,
                        adaptiveThermalEnabled = sim.adaptiveThermalControl
                    )
                    frameTickState.longValue++

                    // Update Compose HUD at ~20 Hz (or immediately while countdown/intro changes)
                    if (frameNs - lastHudUpdateNs >= 50_000_000L) {
                        lastHudUpdateNs = frameNs
                        hudTick++
                    }

                    val workMs = (System.nanoTime() - startWorkNs) / 1_000_000f
                    perfMonitor.recordFrameDurationMs(workMs)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sim.stopAudio()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(CarbonBlack)) {
        // 1. 3D SIMULATION CANVAS (isolated draw-phase invalidation for smooth FPS & low battery use)
        RaceViewportCanvas(
            renderer = renderer,
            sim = sim,
            quality = effectiveQuality,
            dt = lastDt,
            frameTickState = frameTickState
        )

        // 2. CINEMATIC RACE INTRODUCTION OVERLAY
        AnimatedVisibility(
            visible = sim.isIntroActive && !isPaused,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            CinematicIntroOverlay(
                sim = sim,
                hudTick = hudTick,
                onSkip = {
                    sim.skipIntro()
                    hudTick++
                }
            )
        }

        // 3. RACING TELEMETRY HUD (hidden during flyover intro, visible for countdown + race)
        if (!sim.isIntroActive) {
            RacingHudLayer(
                sim = sim,
                hudTick = hudTick,
                onCameraToggle = {
                    sim.cameraMode = when (sim.cameraMode) {
                        CameraMode.CHASE -> CameraMode.COCKPIT
                        CameraMode.COCKPIT -> CameraMode.T_CAM
                        CameraMode.T_CAM -> CameraMode.TV_BROADCAST
                        CameraMode.TV_BROADCAST -> CameraMode.CHASE
                    }
                    hudTick++
                },
                onPauseToggle = { isPaused = !isPaused },
                onPitCall = {
                    sim.triggerPitStop()
                    hudTick++
                }
            )

            // 4. ON-SCREEN DRIVING CONTROLS
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 20.dp, bottom = 20.dp)
            ) {
                VirtualSteeringControl(
                    controlType = sim.controlType,
                    steerValue = sim.playerState.steerInput,
                    onSteerChange = { steer ->
                        sim.playerState.steerInput = steer
                    }
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 20.dp)
            ) {
                PedalControls(
                    throttle = sim.playerState.throttleInput,
                    brake = sim.playerState.brakeInput,
                    onThrottleChange = { sim.playerState.throttleInput = it },
                    onBrakeChange = { sim.playerState.brakeInput = it }
                )
            }
        }

        // 5. PAUSE MENU DIALOG
        if (isPaused) {
            AlertDialog(
                onDismissRequest = { isPaused = false },
                title = {
                    Text(
                        "SESSION PAUSED",
                        color = ApexCyan,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Track: ${sim.circuit.name}", color = TextPrimary)
                        Text("Weather: ${sim.weather.title}", color = TextSecondary)
                        Text("Tires: ${sim.carSetup.tireCompound.displayName}", color = TextSecondary)
                        Text("Graphics: ${effectiveQuality.title}", color = ApexCyan, fontSize = 12.sp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    isPaused = false
                                    onNavigateToReplay()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("pause_photo_mode_btn")
                            ) {
                                Text("PHOTO MODE", color = ApexCyan, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    isPaused = false
                                    onNavigateToTelemetry()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("pause_telemetry_btn")
                            ) {
                                Text("TELEMETRY", color = ApexGold, fontSize = 11.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { isPaused = false },
                        colors = ButtonDefaults.buttonColors(containerColor = ApexCyan),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("resume_button")
                    ) {
                        Text("RESUME", color = CarbonBlack, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                sim.startRaceSession(playIntro = true)
                                isPaused = false
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("restart_session_button")
                        ) {
                            Text("RESTART", color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = {
                                sim.stopAudio()
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("exit_race_button")
                        ) {
                            Text("EXIT", color = ApexRed)
                        }
                    }
                },
                containerColor = CarbonSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 6. RACE FINISHED CHEQUERED FLAG OVERLAY
        if (sim.isRaceFinished) {
            var hasRecorded by remember(sim.isRaceFinished) { mutableStateOf(false) }
            if (!hasRecorded) {
                hasRecorded = true
                careerManager?.let { cm ->
                    val activeEv = cm.activeEvent
                    if (activeEv != null) {
                        cm.recordRaceResult(activeEv.eventId, sim.playerRacePosition, isChampionshipEvent = true)
                        cm.activeEvent = null
                    } else {
                        cm.recordRaceResult("quick_race", sim.playerRacePosition, isChampionshipEvent = false)
                    }
                }
            }

            AlertDialog(
                onDismissRequest = { /* stay on results */ },
                title = {
                    Text(
                        "CHEQUERED FLAG!",
                        color = ApexGold,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                text = {
                    val standings = remember(sim.isRaceFinished) { sim.getStandings() }
                    val totalRaceSec = (sim.playerFinishTimeMs / 1000f) + sim.penaltySeconds
                    val raceMin = (totalRaceSec / 60).toInt()
                    val raceRem = totalRaceSec % 60
                    val bestSec = sim.bestLapTimeMs / 1000f
                    val bestMin = (bestSec / 60).toInt()
                    val bestRem = bestSec % 60

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "FINISHED P${sim.playerRacePosition}",
                            color = ApexRed,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = String.format(Locale.US, "Race Time: %02d:%06.3f", raceMin, raceRem) +
                                if (sim.penaltySeconds > 0f) " (+${sim.penaltySeconds.toInt()}s pen)" else "",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Text(
                            text = String.format(Locale.US, "Best Lap: %02d:%06.3f", bestMin, bestRem),
                            color = ApexPurple,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        HorizontalDivider(color = CarbonBorder, modifier = Modifier.padding(vertical = 4.dp))
                        standings.forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "P${entry.position} #${entry.number} ${entry.shortCode}",
                                    color = if (entry.isPlayer) ApexCyan else TextSecondary,
                                    fontWeight = if (entry.isPlayer) FontWeight.Black else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (entry.finished) {
                                        val s = entry.totalAdjustedTimeMs / 1000f
                                        String.format(Locale.US, "%02d:%05.2f", (s / 60).toInt(), s % 60)
                                    } else {
                                        "LAP ${entry.completedLaps + 1}"
                                    },
                                    color = if (entry.isPlayer) ApexGold else TextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            sim.stopAudio()
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ApexRed),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("finish_continue_button")
                    ) {
                        Text("CONTINUE", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            sim.startRaceSession(playIntro = true)
                        },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("race_again_button")
                    ) {
                        Text("RACE AGAIN", color = TextPrimary)
                    }
                },
                containerColor = CarbonSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun RaceViewportCanvas(
    renderer: Formula3DRenderer,
    sim: RaceSimulationManager,
    quality: GraphicsQuality,
    dt: Float,
    frameTickState: State<Long>
) {
    Canvas(modifier = Modifier.fillMaxSize().testTag("race_canvas")) {
        // Read inside draw phase so only Canvas redraws at 30/60 FPS without recomposition
        @Suppress("UNUSED_VARIABLE")
        val tick = frameTickState.value

        renderer.render(
            drawScope = this,
            playerState = sim.playerState,
            setup = sim.carSetup,
            circuit = sim.circuit,
            aiRivals = sim.aiEngine.aiRivals,
            cameraMode = sim.cameraMode,
            weather = sim.weather,
            startLightsState = sim.startLights,
            driverProfile = sim.driverProfile,
            quality = quality,
            dt = dt,
            introPhase = sim.introPhase,
            introElapsedSec = sim.introElapsedSec,
            useIntroCamera = true
        )
    }
}

@Composable
private fun RacingHudLayer(
    sim: RaceSimulationManager,
    hudTick: Long,
    onCameraToggle: () -> Unit,
    onPauseToggle: () -> Unit,
    onPitCall: () -> Unit
) {
    @Suppress("UNUSED_VARIABLE")
    val refresh = hudTick
    RacingHUD(
        sim = sim,
        onCameraToggle = onCameraToggle,
        onPauseToggle = onPauseToggle,
        onPitCall = onPitCall
    )
}

@Composable
private fun CinematicIntroOverlay(
    sim: RaceSimulationManager,
    hudTick: Long,
    onSkip: () -> Unit
) {
    @Suppress("UNUSED_VARIABLE")
    val refresh = hudTick
    val level = sim.circuit.currentLevel
    val phaseLabel = when (sim.introPhase) {
        RaceIntroPhase.CIRCUIT_FLYOVER -> "CIRCUIT AERIAL OVERVIEW"
        RaceIntroPhase.GRID_SHOWCASE -> "STARTING GRID SHOWCASE"
        RaceIntroPhase.CAMERA_HANDOFF -> "DRIVER POV ENGAGED"
        RaceIntroPhase.NONE -> "RACE START"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle cinematic letterbox framing top & bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xD005070C), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xD805070C))
                    )
                )
        )

        // Top-Left Grand Prix Broadcast Title Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xDC0B0F18)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, ApexRed.copy(alpha = 0.8f)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
                .widthIn(max = 360.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = ApexRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = phaseLabel,
                        color = ApexCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = level.trackTitle.uppercase(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${level.country} • ${level.lengthKm} KM • ${level.turnsCount} TURNS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                HorizontalDivider(
                    color = CarbonBorder,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(sim.driverProfile.liveryTheme.primaryHex))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "#${sim.driverProfile.driverNumber}",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column {
                        Text(
                            text = sim.driverProfile.driverName.uppercase(),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${sim.driverProfile.liveryTheme.title} • ${sim.weather.title}",
                            color = ApexGold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Bottom-Right Skip Intro Button (>= 48dp touch target)
        Button(
            onClick = onSkip,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xE6141A26)),
            border = BorderStroke(1.dp, ApexCyan),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp)
                .defaultMinSize(minWidth = 136.dp, minHeight = 48.dp)
                .testTag("skip_intro_button")
        ) {
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = "Skip Intro",
                tint = ApexCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "SKIP INTRO",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
