package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiOperationState
import com.example.ai.GeminiRaceEngineerService
import com.example.ai.SetupRecommendation
import com.example.career.CareerManager
import com.example.career.GarageVehicleDef
import com.example.model.CarSetup
import com.example.model.TireCompound
import com.example.model.TrackLevelId
import com.example.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    setup: CarSetup,
    careerManager: CareerManager,
    geminiService: GeminiRaceEngineerService,
    currentTrack: TrackLevelId,
    onNavigateBack: () -> Unit,
    onStartTestDrive: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Vehicles, 1 = Customization, 2 = Performance, 3 = Setup Tuning

    var frontWing by remember { mutableFloatStateOf(setup.frontWingAngle) }
    var rearWing by remember { mutableFloatStateOf(setup.rearWingAngle) }
    var brakeBias by remember { mutableFloatStateOf(setup.brakeBias) }
    var fuel by remember { mutableFloatStateOf(setup.fuelLiters) }
    var maxSpeedKph by remember { mutableFloatStateOf(setup.maxSpeedKph) }
    var springStiffness by remember { mutableFloatStateOf(setup.springStiffness) }
    var selectedCompound by remember { mutableStateOf(setup.tireCompound) }

    var setupAiState by remember { mutableStateOf<AiOperationState<SetupRecommendation>>(AiOperationState.Idle) }

    fun saveSetup() {
        setup.frontWingAngle = frontWing
        setup.rearWingAngle = rearWing
        setup.brakeBias = brakeBias
        setup.fuelLiters = fuel
        setup.maxSpeedKph = maxSpeedKph
        setup.springStiffness = springStiffness
        setup.tireCompound = selectedCompound
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(22.dp))
                        Text(
                            "APEX GARAGE & SHOWROOM",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            saveSetup()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("garage_back_button")
                    ) {
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
        bottomBar = {
            Surface(
                color = CarbonSurface,
                border = BorderStroke(1.dp, CarbonBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            saveSetup()
                            onNavigateBack()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Text("SAVE & EXIT", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            saveSetup()
                            onStartTestDrive()
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("test_drive_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ApexRed)
                    ) {
                        Text("TEST DRIVE", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        },
        containerColor = CarbonBlack
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CarbonSurface,
                contentColor = ApexRed
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("SHOWROOM", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    selectedContentColor = ApexRed,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("CUSTOMIZE", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    icon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    selectedContentColor = ApexRed,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("UPGRADES", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    icon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    selectedContentColor = ApexRed,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("SETUP", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    selectedContentColor = ApexRed,
                    unselectedContentColor = TextSecondary
                )
            }

            when (selectedTab) {
                0 -> VehicleShowroomTab(careerManager = careerManager)
                1 -> VehicleCustomizationTab(careerManager = careerManager)
                2 -> VehicleUpgradesTab(careerManager = careerManager)
                3 -> SetupTuningTab(
                    frontWing = frontWing, onFrontWingChange = { frontWing = it },
                    rearWing = rearWing, onRearWingChange = { rearWing = it },
                    brakeBias = brakeBias, onBrakeBiasChange = { brakeBias = it },
                    fuel = fuel, onFuelChange = { fuel = it },
                    maxSpeedKph = maxSpeedKph, onMaxSpeedChange = { maxSpeedKph = it },
                    springStiffness = springStiffness, onSpringStiffnessChange = { springStiffness = it },
                    selectedCompound = selectedCompound, onCompoundChange = { selectedCompound = it },
                    setupAiState = setupAiState,
                    onRequestAi = {
                        scope.launch {
                            setupAiState = AiOperationState.Loading
                            try {
                                val rec = geminiService.recommendSetup(
                                    track = currentTrack,
                                    currentSetup = setup,
                                    topSpeedRecordedKph = maxSpeedKph,
                                    handlingFeedback = "Balance straight-line speed with high-speed corner agility."
                                )
                                setupAiState = AiOperationState.Success(rec)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                setupAiState = AiOperationState.Error(e.localizedMessage ?: "AI Setup failed.")
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun VehicleShowroomTab(careerManager: CareerManager) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(careerManager.availableVehicles) { vehicle ->
            val isOwned = careerManager.ownedVehicleIds.contains(vehicle.vehicleId)
            val isSelected = careerManager.selectedVehicleId == vehicle.vehicleId

            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) ApexCyan else CarbonBorder
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Surface(
                                color = ApexCyan.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    vehicle.category.uppercase(),
                                    color = ApexCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(vehicle.name, color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text(vehicle.description, color = TextSecondary, fontSize = 11.sp)
                        }

                        if (isSelected) {
                            Badge(containerColor = ApexCyan) {
                                Text("SELECTED", color = CarbonBlack, fontWeight = FontWeight.Black, fontSize = 10.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SpecMetric("Top Speed", "${vehicle.baseTopSpeedKph.toInt()} km/h", ApexGold)
                        SpecMetric("Accel", "${vehicle.baseAcceleration}/100", ApexRed)
                        SpecMetric("Handling", "${vehicle.baseHandling}/100", ApexCyan)
                        SpecMetric("Power", "${vehicle.baseEnginePowerBhp} BHP", TextPrimary)
                    }

                    HorizontalDivider(color = CarbonBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOwned) {
                            Text("IN GARAGE • OWNED", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Button(
                                onClick = { careerManager.selectedVehicleId = vehicle.vehicleId; careerManager.saveCareerProgress() },
                                enabled = !isSelected,
                                colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) CarbonBorder else ApexCyan),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isSelected) "ACTIVE" else "SELECT", color = if (isSelected) TextSecondary else CarbonBlack, fontWeight = FontWeight.Black)
                            }
                        } else {
                            Text("Price: ${vehicle.priceCredits} CR", color = ApexGold, fontWeight = FontWeight.Black, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            Button(
                                onClick = { careerManager.purchaseVehicle(vehicle) },
                                colors = ButtonDefaults.buttonColors(containerColor = ApexRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("BUY VEHICLE", color = Color.White, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpecMetric(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextSecondary, fontSize = 10.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Black, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun VehicleCustomizationTab(careerManager: CareerManager) {
    val paints = listOf(
        Pair("Apex Red", 0xFF1801L),
        Pair("Carbon Black", 0xFF111111L),
        Pair("Apex Cyan", 0xFF00E5FFL),
        Pair("Titanium Silver", 0xFFB0BEC5L),
        Pair("Neon Gold", 0xFFFFD700L),
        Pair("Royal Blue", 0xFF1565C0L),
        Pair("Electric Purple", 0xFF7B1FA1L)
    )

    val liveries = listOf("Apex Factory", "Carbon Weave", "Vortex Stripe", "Flame Red", "Matrix Minimal")
    val rims = listOf("Forged Carbon", "Magnesium Gold", "Aero Disc", "Diamond Polished")
    val spoilers = listOf("High Downforce Wing", "Low Drag Le Mans", "Active DRS Wing")
    val decals = listOf("Apex Motorsport", "V-Power", "Redline Studio", "Formula Tech")
    val numbers = listOf(44, 1, 55, 16, 33, 7, 24, 77)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CustomizationSection(title = "VEHICLE PAINT COLOR") {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    paints.forEach { paint ->
                        val isSelected = careerManager.currentPaintHex == paint.second
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                careerManager.currentPaintHex = paint.second
                                careerManager.saveCareerProgress()
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(paint.second))
                                    .border(BorderStroke(width = if (isSelected) 3.dp else 1.dp, color = if (isSelected) ApexCyan else CarbonBorder), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(paint.first, color = if (isSelected) ApexCyan else TextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        item {
            CustomizationSection(title = "RACING LIVERY") {
                OptionChipGrid(
                    options = liveries,
                    selectedOption = careerManager.currentLivery,
                    onSelect = { careerManager.currentLivery = it; careerManager.saveCareerProgress() }
                )
            }
        }

        item {
            CustomizationSection(title = "WHEELS & RIMS") {
                OptionChipGrid(
                    options = rims,
                    selectedOption = careerManager.currentRim,
                    onSelect = { careerManager.currentRim = it; careerManager.saveCareerProgress() }
                )
            }
        }

        item {
            CustomizationSection(title = "AERO SPOILERS & WINGS") {
                OptionChipGrid(
                    options = spoilers,
                    selectedOption = careerManager.currentSpoiler,
                    onSelect = { careerManager.currentSpoiler = it; careerManager.saveCareerProgress() }
                )
            }
        }

        item {
            CustomizationSection(title = "BODY DECALS") {
                OptionChipGrid(
                    options = decals,
                    selectedOption = careerManager.currentDecal,
                    onSelect = { careerManager.currentDecal = it; careerManager.saveCareerProgress() }
                )
            }
        }

        item {
            CustomizationSection(title = "DRIVER RACING NUMBER") {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    numbers.forEach { num ->
                        val isSelected = careerManager.currentDriverNumber == num
                        Surface(
                            onClick = { careerManager.currentDriverNumber = num; careerManager.saveCareerProgress() },
                            color = if (isSelected) ApexRed else CarbonSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) ApexRed else CarbonBorder),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("#$num", color = if (isSelected) Color.White else TextPrimary, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomizationSection(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CarbonBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            content()
        }
    }
}

@Composable
fun OptionChipGrid(options: List<String>, selectedOption: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { opt ->
            val isSelected = selectedOption == opt
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(opt) },
                label = { Text(opt, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ApexRed,
                    selectedLabelColor = Color.White,
                    containerColor = CarbonBlack,
                    labelColor = TextPrimary
                )
            )
        }
    }
}

@Composable
fun VehicleUpgradesTab(careerManager: CareerManager) {
    val upgrades = listOf(
        Triple("engine", "Engine Power Unit", careerManager.engineUpgradeStage),
        Triple("turbo", "Turbo / ERS Hybrid Boost", careerManager.turboUpgradeStage),
        Triple("brakes", "Carbon Ceramic Brakes", careerManager.brakeUpgradeStage),
        Triple("tires", "Soft Compound Slick Tires", careerManager.tireUpgradeStage),
        Triple("suspension", "Active Dampers & Springs", careerManager.suspensionUpgradeStage),
        Triple("aero", "Carbon Fiber Downforce Kit", careerManager.aeroUpgradeStage)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "PERFORMANCE UPGRADE LAB • STAGE 0 to 3",
                color = TextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        items(upgrades) { upgrade ->
            val partId = upgrade.first
            val partName = upgrade.second
            val stage = upgrade.third
            val cost = (stage + 1) * 3500

            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CarbonBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(partName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Current Stage: Stage $stage / 3", color = ApexCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (i in 1..3) {
                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (i <= stage) ApexRed else CarbonBorder)
                                )
                            }
                        }
                    }

                    if (stage < 3) {
                        Button(
                            onClick = { careerManager.upgradePart(partId, cost) },
                            colors = ButtonDefaults.buttonColors(containerColor = ApexRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("UPGRADE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                Text("$cost CR", color = ApexGold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    } else {
                        Badge(containerColor = ApexGold) {
                            Text("MAXED OUT", color = CarbonBlack, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetupTuningTab(
    frontWing: Float, onFrontWingChange: (Float) -> Unit,
    rearWing: Float, onRearWingChange: (Float) -> Unit,
    brakeBias: Float, onBrakeBiasChange: (Float) -> Unit,
    fuel: Float, onFuelChange: (Float) -> Unit,
    maxSpeedKph: Float, onMaxSpeedChange: (Float) -> Unit,
    springStiffness: Float, onSpringStiffnessChange: (Float) -> Unit,
    selectedCompound: TireCompound, onCompoundChange: (TireCompound) -> Unit,
    setupAiState: AiOperationState<SetupRecommendation>,
    onRequestAi: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1522)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, ApexCyan),
            modifier = Modifier.fillMaxWidth().testTag("gemini_garage_card")
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(20.dp))
                        Text("GEMINI AI RACE ENGINEER", color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = onRequestAi,
                        colors = ButtonDefaults.buttonColors(containerColor = ApexCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("OPTIMIZE SETUP", color = CarbonBlack, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }

                when (setupAiState) {
                    is AiOperationState.Loading -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ApexCyan)
                        Text("Analyzing telemetry & track downforce requirements...", color = TextSecondary, fontSize = 11.sp)
                    }
                    is AiOperationState.Success -> {
                        val rec = setupAiState.data
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(rec.chiefEngineerAdvice, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(rec.rationale, color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    is AiOperationState.Error -> {
                        Text(setupAiState.message, color = ApexRed, fontSize = 11.sp)
                    }
                    is AiOperationState.Idle -> {
                        Text("Tap 'OPTIMIZE SETUP' for AI-powered wing and suspension recommendations.", color = TextSecondary, fontSize = 11.sp)
                    }
                    is AiOperationState.Streaming -> {
                        Text(setupAiState.partialText, color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        TuningSliderCard(title = "Front Wing Angle", value = frontWing, valueLabel = "${frontWing.toInt()}°", range = 15f..35f, onChange = onFrontWingChange)
        TuningSliderCard(title = "Rear Wing Angle", value = rearWing, valueLabel = "${rearWing.toInt()}°", range = 20f..45f, onChange = onRearWingChange)
        TuningSliderCard(title = "Brake Bias (Front)", value = brakeBias, valueLabel = "${brakeBias.toInt()}%", range = 50f..62f, onChange = onBrakeBiasChange)
        TuningSliderCard(title = "Top Speed Limiter", value = maxSpeedKph, valueLabel = "${maxSpeedKph.toInt()} km/h", range = 220f..375f, onChange = onMaxSpeedChange)
        TuningSliderCard(title = "Fuel Load", value = fuel, valueLabel = "${fuel.toInt()} kg", range = 10f..100f, onChange = onFuelChange)
        TuningSliderCard(title = "Spring Stiffness", value = springStiffness, valueLabel = String.format(Locale.US, "%.2fx", springStiffness), range = 0.7f..1.3f, onChange = onSpringStiffnessChange)

        Card(
            colors = CardDefaults.cardColors(containerColor = CarbonSurface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CarbonBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("TIRE COMPOUND SELECTION", color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TireCompound.values().forEach { compound ->
                        val isSelected = selectedCompound == compound
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCompoundChange(compound) },
                            label = { Text(compound.displayName, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ApexRed,
                                selectedLabelColor = Color.White,
                                containerColor = CarbonBlack,
                                labelColor = TextPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TuningSliderCard(title: String, value: Float, valueLabel: String, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CarbonBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(valueLabel, color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            }
            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = range,
                colors = SliderDefaults.colors(thumbColor = ApexRed, activeTrackColor = ApexRed)
            )
        }
    }
}
