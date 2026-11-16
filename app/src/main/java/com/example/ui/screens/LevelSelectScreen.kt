package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WbSunny
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
import com.example.model.TrackLevelId
import com.example.model.WeatherCondition
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelSelectScreen(
    currentLevel: TrackLevelId,
    currentWeather: WeatherCondition = WeatherCondition.DRY_SUNNY,
    onSelectLevel: (TrackLevelId, WeatherCondition) -> Unit,
    onStartRace: (TrackLevelId, WeatherCondition) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    var selectedLevel by remember { mutableStateOf(currentLevel) }
    var selectedWeather by remember { mutableStateOf(currentWeather) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SELECT TRACK & ENVIRONMENT",
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("level_select_back_button")) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Track Carousel
            Text(
                "CIRCUIT ENVIRONMENT (6 TRACKS)",
                color = ApexCyan,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(TrackLevelId.entries) { level ->
                    val isSelected = (selectedLevel == level)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF161C2A) else CarbonSurface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) ApexCyan else CarbonBorder
                        ),
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                            .clickable { selectedLevel = level }
                            .testTag("level_card_${level.levelNumber}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Badge(
                                        containerColor = when (level.levelNumber) {
                                            1 -> ApexCyan
                                            2 -> ApexGold
                                            3 -> ApexRed
                                            4 -> ApexPurple
                                            5 -> Color(0xFF00B0FF)
                                            else -> Color(0xFFFFB300)
                                        }
                                    ) {
                                        Text(
                                            "CIRCUIT ${level.levelNumber}",
                                            color = CarbonBlack,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp
                                        )
                                    }

                                    Badge(containerColor = Color(0xFF202636)) {
                                        Text(
                                            level.difficulty,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                Text(
                                    text = level.trackTitle,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 2
                                )

                                Text(
                                    text = level.country,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("LENGTH", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        Text("${level.lengthKm} KM", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Column {
                                        Text("TURNS", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        Text("${level.turnsCount}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Column {
                                        Text("DEFAULT", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        Text(level.defaultWeather.title.take(6), color = ApexGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                Text(
                                    text = level.description,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    maxLines = 3
                                )
                            }

                            Button(
                                onClick = {
                                    selectedLevel = level
                                    onSelectLevel(level, selectedWeather)
                                    onStartRace(level, selectedWeather)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("race_level_${level.levelNumber}_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) ApexRed else Color(0xFF232A3C)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text(
                                        if (isSelected) "RACE THIS CIRCUIT" else "SELECT",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Weather & Environment Condition Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "DYNAMIC ENVIRONMENT & WEATHER (7 CONDITIONS)",
                    color = ApexGold,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WeatherCondition.values().forEach { weather ->
                        val isSelected = (selectedWeather == weather)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedWeather = weather },
                            label = { Text(weather.title, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ApexCyan,
                                selectedLabelColor = CarbonBlack,
                                containerColor = CarbonSurface,
                                labelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("weather_chip_${weather.name}")
                        )
                    }
                }
            }

            // Bottom Confirmation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CarbonSurface)
                    .border(1.dp, CarbonBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selectedLevel.trackTitle.uppercase(),
                        color = ApexCyan,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Weather: ${selectedWeather.title} • Grip: ${(selectedWeather.ambientGrip * 100).toInt()}%",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = {
                        onSelectLevel(selectedLevel, selectedWeather)
                        onStartRace(selectedLevel, selectedWeather)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ApexRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("launch_selected_level_btn")
                ) {
                    Text("START RACE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}
