package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.GameScreen
import com.example.model.TrackLevelId
import com.example.ui.theme.*

@Composable
fun MainMenuScreen(
    currentLevel: TrackLevelId,
    onNavigate: (GameScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTutorial by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT COLUMN: STUDIO BRANDING & CINEMATIC HERO GRAPHIC
        Box(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, CarbonBorder, RoundedCornerShape(16.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.formula_hero_1793088525247),
                contentDescription = "Car Racing: FORMULA Hero Car",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xBB0A0D14), Color.Transparent, Color(0xEE0A0D14)),
                            startY = 0f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = "APEX DYNAMICS MOTORSPORT",
                    color = ApexCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "CAR RACING: FORMULA",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "DREAM. BUILD. RACE. • 60 FPS SIMULATOR",
                    color = ApexRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Badge(containerColor = ApexRed) {
                    Text("ACTIVE LEVEL ${currentLevel.levelNumber}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 9.sp)
                }
                Text(
                    text = "${currentLevel.trackTitle} (${currentLevel.lengthKm} km)",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // RIGHT COLUMN: RACING ACTIONS & SIMULATION HUBS
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Primary Action: Start Quick Race
            Button(
                onClick = { onNavigate(GameScreen.RACE) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_quick_race_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ApexRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Text(
                        text = "START QUICK RACE",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    )
                }
            }

            // 2. Select Grand Prix Level & Environment
            OutlinedButton(
                onClick = { onNavigate(GameScreen.LEVEL_SELECT) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("select_level_button"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ApexCyan),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF131A28))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(18.dp))
                    Text(
                        text = "SELECT TRACK & WEATHER (6 CIRCUITS)",
                        color = ApexCyan,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }

            // 3. Driving Controls & Camera Tutorial Button
            OutlinedButton(
                onClick = { showTutorial = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("tutorial_button"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ApexGold),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1F1A12))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Help, contentDescription = null, tint = ApexGold, modifier = Modifier.size(18.dp))
                    Text(
                        text = "HOW TO DRIVE: CONTROLS & CAMERA GUIDE",
                        color = ApexGold,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            // 4. C++ & Blueprint Vehicle Physics Lab
            OutlinedButton(
                onClick = { onNavigate(GameScreen.PHYSICS_LAB) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("physics_lab_button"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ApexGold),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1E1A12))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = ApexGold, modifier = Modifier.size(18.dp))
                    Text(
                        text = "C++ & BLUEPRINT VEHICLE PHYSICS LAB",
                        color = ApexGold,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            // Cloud Leaderboard & Profile Sync
            OutlinedButton(
                onClick = { onNavigate(GameScreen.CLOUD_LEADERBOARD) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("cloud_leaderboard_button"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ApexCyan),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF0F1E28))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(18.dp))
                    Text(
                        text = "CLOUD LEADERBOARD & DRIVER SYNC",
                        color = ApexCyan,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }

            // Online Multiplayer & Matchmaking
            Button(
                onClick = { onNavigate(GameScreen.ONLINE_MULTIPLAYER) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("online_multiplayer_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ApexRed)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text(
                        text = "🌐 ONLINE MULTIPLEX & MATCHMAKING",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }

            // Secondary Features Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onNavigate(GameScreen.REPLAY_PHOTO_MODE) },
                    modifier = Modifier.weight(1f).height(44.dp).testTag("replay_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CarbonSurface)
                ) {
                    Text("📷 PHOTO & REPLAY", color = ApexCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { onNavigate(GameScreen.TELEMETRY_ANALYSIS) },
                    modifier = Modifier.weight(1f).height(44.dp).testTag("telemetry_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CarbonSurface)
                ) {
                    Text("📊 TELEMETRY", color = ApexGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onNavigate(GameScreen.DRIVER_CUSTOMIZATION) },
                    modifier = Modifier.weight(1f).height(44.dp).testTag("customization_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CarbonSurface)
                ) {
                    Text("🏎️ LIVERY & DRIVER", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = { onNavigate(GameScreen.GARAGE) },
                    modifier = Modifier.weight(1f).height(44.dp).testTag("garage_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CarbonSurface)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(16.dp))
                        Text("GARAGE", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onNavigate(GameScreen.CHAMPIONSHIP) },
                    modifier = Modifier.weight(1f).height(44.dp).testTag("championship_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CarbonSurface)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ApexGold, modifier = Modifier.size(16.dp))
                        Text("SEASON", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                OutlinedButton(
                    onClick = { onNavigate(GameScreen.SETTINGS) },
                    modifier = Modifier.weight(1f).height(44.dp).testTag("settings_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CarbonSurface)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Text("SETTINGS", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                OutlinedButton(
                    onClick = { onNavigate(GameScreen.ARCHITECTURE_DOCS) },
                    modifier = Modifier.weight(1f).height(44.dp).testTag("architecture_docs_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CarbonSurface)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(16.dp))
                        Text("UE5 DOCS", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    // Driving Controls & Camera Tutorial Dialog
    if (showTutorial) {
        AlertDialog(
            onDismissRequest = { showTutorial = false },
            title = {
                Text(
                    "HOW TO DRIVE: CONTROLS & CAMERAS",
                    color = ApexCyan,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TutorialStep("1. Camera Modes", "Tap the Camera icon (top right) during racing to cycle between Chase Cam, Cockpit Cam, Front Bumper / T-Cam, and Cinematic Replay Cam.")
                    TutorialStep("2. Steering & Controls", "Use the Virtual Steering Wheel, Left/Right touch buttons, or Tilt Gyro Sensor. Adjust steering sensitivity in Settings.")
                    TutorialStep("3. Throttle & Brakes", "Use the progressive Throttle and Brake pedals (bottom right). Brake firmly before entering heavy braking zones.")
                    TutorialStep("4. Manual Shifting", "Tap `+` and `−` on the digital cluster to shift gears when manual transmission is enabled.")
                    TutorialStep("5. DRS & ERS Boost", "Activate DRS on straightaways and deploy ERS hybrid boost for maximum overtaking pace.")
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTutorial = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ApexRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("GOT IT, LET'S RACE!", color = Color.White, fontWeight = FontWeight.Black)
                }
            },
            containerColor = CarbonSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun TutorialStep(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = ApexGold, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        Text(description, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
    }
}
