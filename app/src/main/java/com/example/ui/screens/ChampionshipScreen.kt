package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.career.CareerManager
import com.example.career.ChampionshipEvent
import com.example.career.ChampionshipStage
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChampionshipScreen(
    careerManager: CareerManager,
    onNavigateBack: () -> Unit,
    onStartCareerEvent: (ChampionshipEvent, ChampionshipStage) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Career Stages, 1 = Standings

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ApexGold, modifier = Modifier.size(22.dp))
                        Text(
                            "APEX FORMULA CAREER",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("championship_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    Surface(
                        color = CarbonSurface,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, ApexGold.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("${careerManager.credits} CR", color = ApexGold, fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("•", color = TextSecondary)
                            Text("${careerManager.careerTrophiesCount} 🏆", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
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
        ) {
            // Driver Level & XP Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(0.dp, Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(careerManager.driverName.uppercase(), color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                            Text("Apex Dynamics Pro Driver • Tier ${careerManager.driverLevel}", color = TextSecondary, fontSize = 12.sp)
                        }
                        Surface(
                            color = ApexRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ApexRed.copy(alpha = 0.5f))
                        ) {
                            Text(
                                "LVL ${careerManager.driverLevel}",
                                color = ApexRed,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    val currentXpInLevel = careerManager.experiencePoints % 1500
                    val xpProgress = (currentXpInLevel / 1500f).coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("EXP: ${careerManager.experiencePoints} XP", color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("Next Tier: ${(careerManager.driverLevel) * 1500} XP", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        LinearProgressIndicator(
                            progress = { xpProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ApexGold,
                            trackColor = CarbonBorder
                        )
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CarbonSurface,
                contentColor = ApexRed
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("CHAMPIONSHIP STAGES", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                    selectedContentColor = ApexRed,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("DRIVER STANDINGS", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                    selectedContentColor = ApexRed,
                    unselectedContentColor = TextSecondary
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(careerManager.stages) { stage ->
                        StageCard(
                            stage = stage,
                            playerXp = careerManager.experiencePoints,
                            onStartEvent = { event ->
                                onStartCareerEvent(event, stage)
                            }
                        )
                    }
                }
            } else {
                val standings = listOf(
                    Triple("Liam Vance (You)", "VAN", 86),
                    Triple("Marcus Lindqvist", "LIN", 78),
                    Triple("Carlos Silva", "SIL", 64),
                    Triple("Yuki Takahashi", "TAK", 48),
                    Triple("Oliver Bennett", "BEN", 35),
                    Triple("Soren Kjaer", "KJA", 28),
                    Triple("Matteo Rossi", "ROS", 19)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "FIA WORLD FORMULA CHAMPIONSHIP STANDINGS",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    items(standings.size) { idx ->
                        val driver = standings[idx]
                        val pos = idx + 1
                        val isPlayer = driver.first.contains("You")
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPlayer) Color(0xFF1E2638) else CarbonSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isPlayer) 1.5.dp else 1.dp,
                                color = if (isPlayer) ApexRed else CarbonBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "P$pos",
                                        color = when (pos) {
                                            1 -> ApexGold
                                            2 -> TextPrimary
                                            3 -> Color(0xFFCD7F32)
                                            else -> TextSecondary
                                        },
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 16.sp
                                    )
                                    Column {
                                        Text(driver.first, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Apex Motorsport • ${driver.second}", color = TextSecondary, fontSize = 11.sp)
                                    }
                                }
                                Text(
                                    "${driver.third} PTS",
                                    color = if (isPlayer) ApexRed else TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StageCard(
    stage: ChampionshipStage,
    playerXp: Int,
    onStartEvent: (ChampionshipEvent) -> Unit
) {
    val isUnlocked = stage.isUnlocked || playerXp >= stage.stageId.requiredXpToUnlock

    Card(
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isUnlocked) ApexCyan.copy(alpha = 0.5f) else CarbonBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "STAGE ${stage.stageId.stageNumber}:",
                            color = if (isUnlocked) ApexCyan else TextSecondary,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        if (stage.isTrophyAwarded) {
                            Text("🏆 COMPLETED", color = ApexGold, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                    Text(
                        stage.stageId.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        stage.stageId.subtitle,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                if (!isUnlocked) {
                    Surface(
                        color = CarbonBorder,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                            Text("${stage.stageId.requiredXpToUnlock} XP", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            HorizontalDivider(color = CarbonBorder)

            stage.events.forEach { event ->
                EventRow(
                    event = event,
                    isStageUnlocked = isUnlocked,
                    onStartEvent = { onStartEvent(event) }
                )
            }
        }
    }
}

@Composable
fun EventRow(
    event: ChampionshipEvent,
    isStageUnlocked: Boolean,
    onStartEvent: () -> Unit
) {
    val canPlay = isStageUnlocked

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CarbonBlack.copy(alpha = 0.5f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(event.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (event.isCompleted) {
                    Surface(
                        color = ApexGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "P${event.playerFinishedPosition} (+${event.earnedPoints} PTS)",
                            color = ApexGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(event.subtitle, color = TextSecondary, fontSize = 11.sp)
            Text("Prize: +${event.prizeCredits} CR • +${event.rewardXp} XP", color = ApexCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }

        Button(
            onClick = onStartEvent,
            enabled = canPlay,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (event.isCompleted) CarbonBorder else ApexRed,
                disabledContainerColor = CarbonBorder.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("start_event_${event.eventId}")
        ) {
            Icon(
                imageVector = if (event.isCompleted) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (event.isCompleted) ApexGold else Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (event.isCompleted) "REPLAY" else "RACE",
                color = if (event.isCompleted) TextPrimary else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}
