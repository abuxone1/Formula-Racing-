package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiOperationState
import com.example.ai.GeminiRaceEngineerService
import com.example.ai.RaceStrategyReport
import com.example.game.RaceSimulationManager
import com.example.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryAnalysisScreen(
    sim: RaceSimulationManager,
    geminiService: GeminiRaceEngineerService,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val scope = rememberCoroutineScope()
    val samples = sim.telemetryHistory
    val circuitLen = sim.circuit.totalLengthMeters

    // Gemini AI Strategy state
    var strategyState by remember { mutableStateOf<AiOperationState<RaceStrategyReport>>(AiOperationState.Idle) }

    // Gemini Interactive Radio Streaming state
    var driverQuery by remember { mutableStateOf("") }
    var radioResponse by remember { mutableStateOf("") }
    var isStreamingRadio by remember { mutableStateOf(false) }
    var radioError by remember { mutableStateOf<String?>(null) }

    fun formatTime(ms: Long): String {
        if (ms <= 0L) return "--:--.---"
        val totalSec = ms / 1000f
        val mins = (totalSec / 60).toInt()
        val secs = totalSec % 60
        return String.format(java.util.Locale.US, "%d:%05.2f", mins, secs)
    }

    fun requestAiStrategy() {
        scope.launch {
            strategyState = AiOperationState.Loading
            try {
                val wear = sim.playerState.tireWear
                val avgWear = (wear.average().toFloat() * 100f).coerceIn(0f, 100f)
                val report = geminiService.analyzeStrategy(
                    track = sim.circuit.currentLevel,
                    currentLap = sim.currentLap,
                    totalLaps = sim.totalLaps,
                    position = sim.playerRacePosition,
                    tireCompound = sim.carSetup.tireCompound.displayName,
                    tireWearPercent = avgWear,
                    frontLeftWear = wear.getOrElse(0) { 0.05f } * 100f,
                    frontRightWear = wear.getOrElse(1) { 0.05f } * 100f,
                    rearLeftWear = wear.getOrElse(2) { 0.05f } * 100f,
                    rearRightWear = wear.getOrElse(3) { 0.05f } * 100f,
                    fuelPercent = (sim.carSetup.fuelLiters / 110f) * 100f,
                    ersPercent = sim.playerState.ersEnergy * 100f,
                    weather = sim.weather.title,
                    lastLapTimeFormatted = formatTime(sim.lastLapTimeMs),
                    bestLapTimeFormatted = formatTime(sim.bestLapTimeMs),
                    gapAheadSec = 1.35f,
                    gapBehindSec = 2.12f
                )
                strategyState = AiOperationState.Success(report)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                strategyState = AiOperationState.Error(e.localizedMessage ?: "AI Telemetry connection error.")
            }
        }
    }

    fun sendDriverRadioMessage(questionText: String) {
        if (questionText.isBlank() || isStreamingRadio) return
        scope.launch {
            isStreamingRadio = true
            radioResponse = ""
            radioError = null
            try {
                val wearPct = (sim.playerState.tireWear.average().toFloat() * 100f).toInt()
                val ersPct = (sim.playerState.ersEnergy * 100f).toInt()
                val telemetrySummary = "Circuit: ${sim.circuit.currentLevel.trackTitle}, Lap: ${sim.currentLap}/${sim.totalLaps}, Pos: P${sim.playerRacePosition}, Tire Wear: $wearPct%, Compound: ${sim.carSetup.tireCompound.name}, ERS: $ersPct%"
                geminiService.askPitWallStream(questionText, telemetrySummary).collect { chunk ->
                    radioResponse += chunk
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                radioError = e.localizedMessage ?: "Radio transmission failed."
            } finally {
                isStreamingRadio = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "MOTEC TELEMETRY & AI STRATEGIST",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("telemetry_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // GEMINI AI RACE STRATEGIST & PIT WALL BANNER
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1522)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ApexCyan),
                    modifier = Modifier.fillMaxWidth().testTag("gemini_strategist_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ApexCyan)
                                Column {
                                    Text(
                                        "GEMINI PIT WALL STRATEGIST",
                                        color = ApexCyan,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        "Powered by Gemini 3.8 Flash & Firebase AI Logic",
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Button(
                                onClick = { requestAiStrategy() },
                                colors = ButtonDefaults.buttonColors(containerColor = ApexCyan),
                                shape = RoundedCornerShape(8.dp),
                                enabled = strategyState !is AiOperationState.Loading,
                                modifier = Modifier.testTag("request_strategy_button")
                            ) {
                                if (strategyState is AiOperationState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = CarbonBlack,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ANALYZING...", color = CarbonBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CarbonBlack, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ANALYZE TELEMETRY", color = CarbonBlack, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            }
                        }

                        // Strategy State Output
                        when (val state = strategyState) {
                            is AiOperationState.Idle -> {
                                Text(
                                    "Tap 'Analyze Telemetry' to receive AI pit-window recommendations, undercut analysis, and tire management advice based on live car data.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            is AiOperationState.Loading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF141C2B))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CircularProgressIndicator(color = ApexCyan, strokeWidth = 3.dp)
                                        Text(
                                            "Evaluating tire thermal degradation, fuel burns, and undercut delta with Gemini...",
                                            color = TextPrimary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                            is AiOperationState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x33FF1744))
                                        .border(1.dp, ApexRed, RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text("AI TELEMETRY ERROR", color = ApexRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text(state.message, color = TextPrimary, fontSize = 11.sp)
                                    }
                                }
                            }
                            is AiOperationState.Success -> {
                                val report = state.data
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF151D2D))
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "CALLSIGN: ${report.callsign.uppercase()}",
                                            color = ApexGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            "UNDERCUT THREAT: ${report.undercutPotential.uppercase()}",
                                            color = if (report.undercutPotential.contains("High", true)) ApexRed else ApexGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Divider(color = Color(0xFF26334D))

                                    Text(
                                        "PIT WINDOW RECOMMENDATION:",
                                        color = ApexCyan,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(report.pitWindowRecommendation, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                                    Text(
                                        "TIRE & ERS STRATEGY:",
                                        color = ApexGold,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text("${report.tireManagementAdvice} | ${report.ersFuelStrategy}", color = TextSecondary, fontSize = 12.sp)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF0C1019))
                                            .border(1.dp, Color(0xFF232D42), RoundedCornerShape(6.dp))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            "📻 RADIO: \"${report.engineerRadioQuote}\"",
                                            color = Color(0xFF80D8FF),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }

            // INTERACTIVE DRIVER-TO-PIT-WALL RADIO (STREAMING GEMINI)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.HeadsetMic, contentDescription = null, tint = ApexGreen)
                            Text(
                                "INTERACTIVE TEAM RADIO (STREAMING GEMINI)",
                                color = TextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Preset quick questions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val preset1 = "Should we box early?"
                            val preset2 = "How are my tires?"
                            val preset3 = "Where am I losing time?"

                            listOf(preset1, preset2, preset3).forEach { chip ->
                                OutlinedButton(
                                    onClick = {
                                        driverQuery = chip
                                        sendDriverRadioMessage(chip)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2D384E)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(chip, fontSize = 9.sp, maxLines = 1)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = driverQuery,
                                onValueChange = { driverQuery = it },
                                placeholder = { Text("Ask race engineer over radio...", fontSize = 12.sp, color = TextSecondary) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("driver_radio_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ApexCyan,
                                    unfocusedBorderColor = CarbonBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )

                            IconButton(
                                onClick = {
                                    sendDriverRadioMessage(driverQuery)
                                },
                                enabled = driverQuery.isNotBlank() && !isStreamingRadio,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (driverQuery.isNotBlank() && !isStreamingRadio) ApexGreen else Color(0xFF232B3D))
                                    .testTag("driver_radio_send_button")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send Radio", tint = CarbonBlack)
                            }
                        }

                        if (isStreamingRadio || radioResponse.isNotEmpty() || radioError != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0C1019))
                                    .border(1.dp, if (radioError != null) ApexRed else ApexGreen, RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("📻 PIT WALL RESPONSE", color = ApexGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        if (isStreamingRadio) {
                                            Text("TRANSMITTING...", color = ApexGold, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }

                                    if (radioError != null) {
                                        Text(radioError ?: "", color = ApexRed, fontSize = 12.sp)
                                    } else {
                                        Text(
                                            radioResponse.ifEmpty { "Awaiting telemetry uplink..." },
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Summary KPI Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val peakSpeed = samples.maxOfOrNull { it.speedKph } ?: 328f
                    val peakBrakeG = 5.2f
                    val peakLatG = samples.maxOfOrNull { abs(it.lateralG) } ?: 4.2f

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("PEAK SPEED", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("${peakSpeed.toInt()} KM/H", color = ApexGold, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("MAX BRAKING G", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(String.format("-%.1f G", peakBrakeG), color = ApexRed, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("MAX CORNERING", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text(String.format("%.2f G", peakLatG), color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                }
            }

            // 1. SPEED TRACE GRAPH (KM/H vs DISTANCE)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SPEED TRACE (KM/H VS DISTANCE)", color = ApexGold, fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("0 to ${(circuitLen / 1000f).toInt()} KM", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0A0D14))
                                .padding(8.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                drawLine(Color(0xFF1B2234), Offset(0f, h * 0.25f), Offset(w, h * 0.25f), 1f)
                                drawLine(Color(0xFF1B2234), Offset(0f, h * 0.50f), Offset(w, h * 0.50f), 1f)
                                drawLine(Color(0xFF1B2234), Offset(0f, h * 0.75f), Offset(w, h * 0.75f), 1f)

                                if (samples.isNotEmpty()) {
                                    val path = Path()
                                    samples.forEachIndexed { idx, s ->
                                        val px = (s.distanceM / circuitLen) * w
                                        val py = h - (s.speedKph / 360f) * h
                                        if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                    }
                                    drawPath(path, color = ApexGold, style = Stroke(width = 2.5.dp.toPx()))
                                }
                            }
                        }
                    }
                }
            }

            // 2. THROTTLE & BRAKE TRACE
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PEDALS: THROTTLE (GREEN) & BRAKE (RED)", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("0% to 100%", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0A0D14))
                                .padding(8.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                if (samples.isNotEmpty()) {
                                    val tPath = Path()
                                    val bPath = Path()
                                    samples.forEachIndexed { idx, s ->
                                        val px = (s.distanceM / circuitLen) * w
                                        val pyT = h - (s.throttle * h * 0.85f)
                                        val pyB = h - (s.brake * h * 0.85f)
                                        if (idx == 0) {
                                            tPath.moveTo(px, pyT)
                                            bPath.moveTo(px, pyB)
                                        } else {
                                            tPath.lineTo(px, pyT)
                                            bPath.lineTo(px, pyB)
                                        }
                                    }
                                    drawPath(tPath, color = ApexGreen, style = Stroke(width = 2.dp.toPx()))
                                    drawPath(bPath, color = ApexRed, style = Stroke(width = 2.dp.toPx()))
                                }
                            }
                        }
                    }
                }
            }

            // 3. LATERAL CORNERING G-FORCE TRACE
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("LATERAL CORNERING G-FORCE", color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("Left (-5G) to Right (+5G)", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0A0D14))
                                .padding(8.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val midY = h * 0.5f

                                drawLine(Color(0xFF232D42), Offset(0f, midY), Offset(w, midY), 1f)

                                if (samples.isNotEmpty()) {
                                    val gPath = Path()
                                    samples.forEachIndexed { idx, s ->
                                        val px = (s.distanceM / circuitLen) * w
                                        val py = midY - (s.lateralG / 5.0f) * midY
                                        if (idx == 0) gPath.moveTo(px, py) else gPath.lineTo(px, py)
                                    }
                                    drawPath(gPath, color = ApexCyan, style = Stroke(width = 2.dp.toPx()))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
