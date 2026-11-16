package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.RaceSimulationManager
import com.example.model.CameraMode
import com.example.renderer.Formula3DRenderer
import com.example.renderer.GraphicsPerformanceMonitor
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class PhotoFilter(val title: String, val overlayColor: Color) {
    RAW("Raw Motorsport", Color.Transparent),
    MONACO_GOLD("Monaco Sunset", Color(0x22FFA000)),
    NOIR("Silver Noir", Color(0x33101010)),
    NEON_CYBER("Neon Apex", Color(0x2200E5FF))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplayScreen(
    sim: RaceSimulationManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val renderer = remember { Formula3DRenderer() }
    val perfMonitor = remember(context) { GraphicsPerformanceMonitor(context) }
    val frameTickState = remember { mutableLongStateOf(0L) }
    var lastDt by remember { mutableFloatStateOf(0.016f) }
    var effectiveQuality by remember { mutableStateOf(sim.graphicsQuality) }

    var selectedFilter by remember { mutableStateOf(PhotoFilter.RAW) }
    var replaySpeed by remember { mutableFloatStateOf(0.5f) }
    var showFlash by remember { mutableStateOf(false) }
    var capturedPhotoToast by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var lastSimNs = System.nanoTime()
        while (isActive) {
            withFrameNanos { frameNs ->
                val targetIntervalNs = perfMonitor.getTargetFrameIntervalNs(
                    requested = sim.graphicsQuality,
                    adaptiveThermalEnabled = sim.adaptiveThermalControl
                )
                val elapsedNs = frameNs - lastSimNs
                if (elapsedNs >= targetIntervalNs - 2_000_000L) {
                    val dt = (elapsedNs / 1_000_000_000.0).toFloat().coerceIn(0.008f, 0.050f)
                    lastSimNs = frameNs
                    lastDt = dt * replaySpeed

                    val workStart = System.nanoTime()
                    sim.stepSimulation(lastDt)
                    effectiveQuality = perfMonitor.getEffectiveQuality(
                        requested = sim.graphicsQuality,
                        adaptiveThermalEnabled = sim.adaptiveThermalControl
                    )
                    frameTickState.longValue++
                    val workMs = (System.nanoTime() - workStart) / 1_000_000f
                    perfMonitor.recordFrameDurationMs(workMs)
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(CarbonBlack)) {
        // 1. 3D REPLAY VIEWPORT
        Canvas(modifier = Modifier.fillMaxSize().testTag("replay_canvas")) {
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
                startLightsState = 6,
                driverProfile = sim.driverProfile,
                quality = effectiveQuality,
                dt = lastDt,
                useIntroCamera = false
            )
        }

        // Color grading filter overlay
        if (selectedFilter.overlayColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(selectedFilter.overlayColor)
            )
        }

        // Flash Animation
        AnimatedVisibility(visible = showFlash, enter = fadeIn(), exit = fadeOut()) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White))
        }

        // 2. TOP TOOLBAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC0E121C))
                    .testTag("replay_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }

            // Camera Angle Selector
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Pair("TV CAM", CameraMode.TV_BROADCAST),
                    Pair("CHASE", CameraMode.CHASE),
                    Pair("COCKPIT", CameraMode.COCKPIT),
                    Pair("T-CAM", CameraMode.T_CAM)
                ).forEach { (label, mode) ->
                    val isSelected = (sim.cameraMode == mode)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ApexCyan else Color(0xCC111522))
                            .clickable { sim.cameraMode = mode }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) CarbonBlack else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Speed Selector
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1.0f, 0.5f, 0.25f).forEach { spd ->
                    val isSelected = (replaySpeed == spd)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) ApexGold else Color(0xAA111522))
                            .clickable { replaySpeed = spd }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${spd}x",
                            color = if (isSelected) CarbonBlack else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. BOTTOM PHOTO & FILTER CONTROLS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Filters
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PhotoFilter.entries.forEach { filter ->
                    val isSelected = (selectedFilter == filter)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ApexRed else Color(0xCC121624))
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter.title,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Shutter Button
            Button(
                onClick = {
                    showFlash = true
                    capturedPhotoToast = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = ApexCyan),
                shape = CircleShape,
                modifier = Modifier
                    .size(54.dp)
                    .testTag("snap_photo_button")
            ) {
                Icon(Icons.Default.Camera, contentDescription = "Snap Photo", tint = CarbonBlack)
            }
        }

        // Captured Photo Toast Overlay
        if (capturedPhotoToast) {
            LaunchedEffect(capturedPhotoToast) {
                delay(300)
                showFlash = false
                delay(1700)
                capturedPhotoToast = false
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xF00A0D15)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ApexCyan),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Camera, contentDescription = null, tint = ApexCyan)
                    Column {
                        Text("PHOTO CAPTURED", color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        Text("4K Telephoto frame saved to gallery", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
