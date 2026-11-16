package com.example.ui.components

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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.RaceSimulationManager
import com.example.model.CameraMode
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun RacingHUD(
    sim: RaceSimulationManager,
    onCameraToggle: () -> Unit,
    onPauseToggle: () -> Unit,
    onPitCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = sim.playerState
    val setup = sim.carSetup

    Box(modifier = modifier.fillMaxSize()) {
        // 1. TOP HEADER TELEMETRY
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Position & Lap Count
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                // Position Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ApexRed)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("position_badge")
                ) {
                    Text(
                        text = "P${sim.playerRacePosition}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Lap Counter
                Column {
                    Text(
                        text = "LAP",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${sim.currentLap} / ${sim.totalLaps}",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Track-Cutting Penalty Badge (only visible when > 0s)
                if (sim.penaltySeconds > 0f) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xDDF57F17))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("penalty_badge")
                    ) {
                        Text(
                            text = "+${sim.penaltySeconds.toInt()}s PEN",
                            color = CarbonBlack,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Center: Lap Times & Live Delta
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Current Lap Time
                val curSeconds = sim.currentLapTimeMs / 1000f
                val curMinutes = (curSeconds / 60).toInt()
                val curSecRemainder = curSeconds % 60
                val curTimeStr = String.format(Locale.US, "%02d:%06.3f", curMinutes, curSecRemainder)

                Text(
                    text = curTimeStr,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                // Live Delta Indicator
                val delta = sim.deltaVsBestSec
                val deltaStr = String.format(Locale.US, "%+6.3f", delta)
                val deltaColor = if (delta <= 0f) ApexGreen else ApexRed

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(deltaColor.copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = deltaStr,
                        color = deltaColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Right: Camera Toggle, Pit Stop & Pause
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Camera Toggle
                IconButton(
                    onClick = onCameraToggle,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x66181D2A))
                        .testTag("camera_toggle_button")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = TextPrimary)
                }

                // Pit Call Button
                IconButton(
                    onClick = onPitCall,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (sim.isPitStopping) ApexCyan else Color(0x66181D2A))
                        .testTag("pit_call_button")
                ) {
                    Icon(Icons.Default.Build, contentDescription = "Pit Box", tint = if (sim.isPitStopping) CarbonBlack else TextPrimary)
                }

                // Pause Button
                IconButton(
                    onClick = onPauseToggle,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x66181D2A))
                        .testTag("pause_button")
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = TextPrimary)
                }
            }
        }

        // PIT-WALL RACE ENGINEER RADIO BANNER
        sim.radio.activeMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xF00A0E18)),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(msg.type.badgeColorHex)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 58.dp)
                    .testTag("radio_message_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(msg.type.badgeColorHex))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = msg.type.title,
                            color = CarbonBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = msg.text,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 2. PIT STOPPING OVERLAY
        AnimatedVisibility(
            visible = sim.isPitStopping,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xDD0D111A)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ApexCyan)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("APEX PIT STOP IN PROGRESS", color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { sim.pitStopProgress },
                        modifier = Modifier.width(220.dp).height(8.dp),
                        color = ApexCyan,
                        trackColor = Color(0xFF202636)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Changing tires & Front Wing adjust...", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        // 3. STARTING LIGHTS BANNER (During Countdown)
        if (sim.isStartingCountdown && sim.startLights in 1..5) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xCC000000)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (i in 1..5) {
                        val isLit = (i <= sim.startLights)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isLit) ApexRed else Color(0xFF262A34))
                        )
                    }
                }
            }
        }

        // 3B. OFF-TRACK / STRUCTURAL DAMAGE & TRACK RECOVERY BAR
        if (state.isOffTrack || state.overallDamage > 0.05f) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.overallDamage > 0.05f) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCC1A1014))
                            .border(1.dp, ApexRed, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("damage_indicator")
                    ) {
                        Text(
                            text = "DMG ${(state.overallDamage * 100f).toInt()}%",
                            color = ApexRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                if (state.isOffTrack) {
                    Button(
                        onClick = { sim.recoverCarToTrack() },
                        colors = ButtonDefaults.buttonColors(containerColor = ApexGold),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("recover_car_button")
                    ) {
                        Text(
                            text = "RECOVER CAR",
                            color = CarbonBlack,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 4. BOTTOM TACHOMETER, GEAR, AND DRIVING TELEMETRY
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // RPM Shift Light LEDs
            val totalLeds = 16
            val rpmRatio = ((state.rpm - 6000f) / 9000f).coerceIn(0f, 1f)
            val litLeds = (rpmRatio * totalLeds).toInt()

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x990A0D14))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                for (led in 0 until totalLeds) {
                    val ledColor = when {
                        led < 6 -> ApexGreen
                        led < 11 -> ApexRed
                        else -> ApexPurple
                    }
                    val isLit = led < litLeds
                    Box(
                        modifier = Modifier
                            .size(width = 12.dp, height = 7.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isLit) ledColor else Color(0xFF1E2330))
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Digital Cluster: Gear & Speedometer
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC0D101A))
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                // Manual Shift Down (only in Manual Transmission mode)
                if (!sim.physics.automaticTransmission) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1D2434))
                            .clickable { sim.shiftDown() }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("shift_down_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("−", color = ApexCyan, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Gear
                Text(
                    text = "${state.gear}",
                    color = ApexCyan,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                // Manual Shift Up (only in Manual Transmission mode)
                if (!sim.physics.automaticTransmission) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1D2434))
                            .clickable { sim.shiftUp() }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("shift_up_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = ApexGreen, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Speed
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "${state.speedKph.toInt()}",
                        color = TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "KM/H",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // DRS & ERS System Controls
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // DRS Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    state.drsOpen -> ApexGreen
                                    state.drsAvailable -> ApexCyan.copy(alpha = 0.35f)
                                    else -> Color(0xFF1D2230)
                                }
                            )
                            .clickable(enabled = state.drsAvailable) {
                                state.drsOpen = !state.drsOpen
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("drs_button")
                    ) {
                        Text(
                            text = if (state.drsOpen) "DRS ON" else "DRS",
                            color = if (state.drsOpen) CarbonBlack else if (state.drsAvailable) ApexCyan else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // ERS Boost Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (state.ersDeploying) ApexGold else ApexCyan.copy(alpha = 0.25f))
                            .clickable {
                                state.ersDeploying = !state.ersDeploying
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("ers_button")
                    ) {
                        Text(
                            text = "ERS ${(state.ersEnergy * 100).toInt()}%",
                            color = if (state.ersDeploying) CarbonBlack else ApexCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
