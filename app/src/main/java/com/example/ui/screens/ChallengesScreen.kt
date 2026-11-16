package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.career.CareerManager
import com.example.ui.theme.*

data class DailyChallenge(
    val id: String,
    val title: String,
    val description: String,
    val rewardCredits: Int,
    val rewardXp: Int,
    var isCompleted: Boolean = false,
    var isClaimed: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(
    careerManager: CareerManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val challenges = remember {
        mutableStateListOf(
            DailyChallenge("ch_1", "Apex Sprint Master", "Complete 3 fast laps in any quick race session", 1200, 400, isCompleted = true, isClaimed = false),
            DailyChallenge("ch_2", "Speed Trap Velocity", "Reach over 345 km/h on any straightaway", 1800, 650, isCompleted = false, isClaimed = false),
            DailyChallenge("ch_3", "Garage Tuner", "Upgrade any vehicle performance part to Stage 1 or higher", 2500, 800, isCompleted = true, isClaimed = false),
            DailyChallenge("ch_4", "Championship Contender", "Win any event in Career Mode Stage 1 or 2", 3500, 1200, isCompleted = false, isClaimed = false)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = ApexGold, modifier = Modifier.size(22.dp))
                        Text(
                            "DAILY MOTORSPORT CHALLENGES",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("challenges_back_button")) {
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
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CarbonSurface)
            )
        },
        containerColor = CarbonBlack
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ApexCyan.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("SEASON 1 • DAILY DRIVER OBJECTIVES", color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("Complete daily driving missions to earn bonus credits, XP, and unlock championship rewards.", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            items(challenges) { challenge ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (challenge.isClaimed) CarbonBorder else ApexRed.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(challenge.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (challenge.isCompleted && !challenge.isClaimed) {
                                    Badge(containerColor = ApexGold) {
                                        Text("READY", color = CarbonBlack, fontWeight = FontWeight.Black, fontSize = 9.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(challenge.description, color = TextSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Reward: +${challenge.rewardCredits} CR • +${challenge.rewardXp} XP", color = ApexCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (challenge.isCompleted && !challenge.isClaimed) {
                                    challenge.isClaimed = true
                                    careerManager.credits += challenge.rewardCredits
                                    careerManager.saveCareerProgress()
                                }
                            },
                            enabled = challenge.isCompleted && !challenge.isClaimed,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (challenge.isClaimed) CarbonBorder else ApexRed,
                                disabledContainerColor = CarbonBorder.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (challenge.isClaimed) "CLAIMED" else if (challenge.isCompleted) "CLAIM" else "IN PROGRESS",
                                color = if (challenge.isClaimed) TextSecondary else Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
