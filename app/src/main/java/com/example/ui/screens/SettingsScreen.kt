package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.RaceSimulationManager
import com.example.model.AIDifficulty
import com.example.model.ControlType
import com.example.model.GraphicsQuality
import com.example.model.WeatherCondition
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    sim: RaceSimulationManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    var selectedControl by remember { mutableStateOf(sim.controlType) }
    var selectedWeather by remember { mutableStateOf(sim.weather) }
    var selectedGraphics by remember { mutableStateOf(sim.graphicsQuality) }
    var selectedAiDifficulty by remember { mutableStateOf(sim.aiDifficulty) }
    var steerSensitivity by remember { mutableFloatStateOf(sim.carSetup.steeringSensitivity) }
    var adaptiveThermal by remember { mutableStateOf(sim.adaptiveThermalControl) }
    var abs by remember { mutableStateOf(sim.physics.absEnabled) }
    var tcs by remember { mutableStateOf(sim.physics.tractionControlEnabled) }
    var steerAssist by remember { mutableStateOf(sim.physics.steeringAssistEnabled) }
    var autoShift by remember { mutableStateOf(sim.physics.automaticTransmission) }
    var volume by remember { mutableFloatStateOf(sim.audio.masterVolume) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "GAME & SIMULATION SETTINGS",
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CarbonSurface)
            )
        },
        containerColor = CarbonBlack
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Control Scheme Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("STEERING & CONTROLS", color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 13.sp, fontFamily = FontFamily.Monospace)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("Virtual Wheel", ControlType.VIRTUAL_WHEEL, "control_wheel_btn"),
                            Triple("Left/Right", ControlType.LEFT_RIGHT_BUTTONS, "control_buttons_btn"),
                            Triple("Tilt Gyro", ControlType.TILT_SENSOR, "control_tilt_btn")
                        ).forEach { (label, type, tag) ->
                            val isSelected = (selectedControl == type)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ApexCyan.copy(alpha = 0.25f) else Color(0xFF191F2C))
                                    .clickable {
                                        selectedControl = type
                                        sim.controlType = type
                                    }
                                    .padding(vertical = 12.dp)
                                    .testTag(tag),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Steering Sensitivity Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Steering Sensitivity", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                String.format(Locale.US, "%.2fx", steerSensitivity),
                                color = ApexCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = steerSensitivity,
                            onValueChange = {
                                steerSensitivity = it
                                sim.carSetup.steeringSensitivity = it
                            },
                            valueRange = 0.5f..1.6f,
                            colors = SliderDefaults.colors(thumbColor = ApexCyan, activeTrackColor = ApexCyan),
                            modifier = Modifier.testTag("steering_sensitivity_slider")
                        )
                    }
                }
            }

            // AI Opponent Difficulty Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "AI OPPONENT DIFFICULTY",
                        color = ApexCyan,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Triple("BEGINNER", AIDifficulty.BEGINNER, "ai_diff_beginner_btn"),
                            Triple("INTERM.", AIDifficulty.INTERMEDIATE, "ai_diff_intermediate_btn"),
                            Triple("PRO", AIDifficulty.PROFESSIONAL, "ai_diff_professional_btn"),
                            Triple("ELITE", AIDifficulty.ELITE, "ai_diff_elite_btn")
                        ).forEach { (label, diff, tag) ->
                            val isSelected = (selectedAiDifficulty == diff)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ApexGold.copy(alpha = 0.28f) else Color(0xFF191F2C))
                                    .clickable {
                                        selectedAiDifficulty = diff
                                        sim.aiDifficulty = diff
                                    }
                                    .padding(vertical = 12.dp)
                                    .testTag(tag),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Text(
                        text = "${selectedAiDifficulty.displayName}: ${selectedAiDifficulty.description}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Graphics Quality & Thermal Optimization Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "GRAPHICS & THERMAL OPTIMIZATION",
                        color = ApexCyan,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("LOW", GraphicsQuality.LOW, "graphics_low_btn"),
                            Triple("MEDIUM", GraphicsQuality.MEDIUM, "graphics_medium_btn"),
                            Triple("HIGH", GraphicsQuality.HIGH, "graphics_high_btn")
                        ).forEach { (label, tier, tag) ->
                            val isSelected = (selectedGraphics == tier)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) ApexRed.copy(alpha = 0.28f) else Color(0xFF191F2C))
                                    .clickable {
                                        selectedGraphics = tier
                                        sim.graphicsQuality = tier
                                    }
                                    .padding(vertical = 12.dp)
                                    .testTag(tag),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Text(
                        text = "${selectedGraphics.title}: ${selectedGraphics.subtitle}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Adaptive Thermal & Battery Saver", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Auto-scales FPS & VFX when device warms up", color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = adaptiveThermal,
                            onCheckedChange = {
                                adaptiveThermal = it
                                sim.adaptiveThermalControl = it
                            },
                            modifier = Modifier.testTag("adaptive_thermal_switch"),
                            colors = SwitchDefaults.colors(checkedThumbColor = ApexCyan, checkedTrackColor = ApexCyan.copy(alpha = 0.5f))
                        )
                    }
                }
            }

            // Weather & Track Conditions
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("WEATHER & LIGHTING CONDITIONS", color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 13.sp, fontFamily = FontFamily.Monospace)

                    WeatherCondition.entries.forEach { cond ->
                        val isSelected = (selectedWeather == cond)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF1B2436) else Color.Transparent)
                                .clickable {
                                    selectedWeather = cond
                                    sim.weather = cond
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cond.title, color = if (isSelected) Color.White else TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedWeather = cond
                                    sim.weather = cond
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = ApexCyan)
                            )
                        }
                    }
                }
            }

            // Driving Assists Toggles
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("MOTORSPORT DRIVING ASSISTS", color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 13.sp, fontFamily = FontFamily.Monospace)

                    // Steering Assist
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Steering Sensitivity Assist", color = TextPrimary, fontSize = 13.sp)
                        Switch(
                            checked = steerAssist,
                            onCheckedChange = {
                                steerAssist = it
                                sim.physics.steeringAssistEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = ApexCyan, checkedTrackColor = ApexCyan.copy(alpha = 0.5f))
                        )
                    }

                    // Anti-lock Brakes (ABS)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Anti-Lock Brakes (ABS)", color = TextPrimary, fontSize = 13.sp)
                        Switch(
                            checked = abs,
                            onCheckedChange = {
                                abs = it
                                sim.physics.absEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = ApexCyan, checkedTrackColor = ApexCyan.copy(alpha = 0.5f))
                        )
                    }

                    // Traction Control (TCS)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Traction Control (TCS)", color = TextPrimary, fontSize = 13.sp)
                        Switch(
                            checked = tcs,
                            onCheckedChange = {
                                tcs = it
                                sim.physics.tractionControlEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = ApexCyan, checkedTrackColor = ApexCyan.copy(alpha = 0.5f))
                        )
                    }

                    // Transmission
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Automatic Transmission", color = TextPrimary, fontSize = 13.sp)
                        Switch(
                            checked = autoShift,
                            onCheckedChange = {
                                autoShift = it
                                sim.physics.automaticTransmission = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = ApexCyan, checkedTrackColor = ApexCyan.copy(alpha = 0.5f))
                        )
                    }
                }
            }

            // Audio Volume Slider
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("V6 TURBO ENGINE AUDIO VOLUME", color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        Text("${(volume * 100).toInt()}%", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            sim.audio.masterVolume = it
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = ApexRed, activeTrackColor = ApexRed)
                    )
                }
            }
        }
    }
}
