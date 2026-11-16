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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
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
import com.example.ui.theme.*
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicsLabScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Wheel Friction (Pacejka)", "Suspension Dynamics", "Engine Torque Curve", "UE5 C++ & Blueprint")

    // Interactive Physics Simulation Parameters
    var slipAngleDeg by remember { mutableFloatStateOf(6.5f) }
    var normalLoadN by remember { mutableFloatStateOf(8500f) }
    var camberDeg by remember { mutableFloatStateOf(-2.5f) }

    var suspensionTravelCm by remember { mutableFloatStateOf(2.8f) }
    var damperVelocityMps by remember { mutableFloatStateOf(0.4f) }

    var testRpm by remember { mutableFloatStateOf(11800f) }
    var testThrottle by remember { mutableFloatStateOf(1.0f) }
    var testBoostBar by remember { mutableFloatStateOf(3.8f) }
    var testErsDeploy by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "C++ & BLUEPRINT VEHICLE PHYSICS LAB",
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("physics_lab_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CarbonSurface)
            )
        },
        containerColor = CarbonBlack
    ) { innerPadding ->
        Column(modifier = modifier.fillMaxSize().padding(innerPadding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = CarbonSurface,
                contentColor = ApexCyan,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) ApexCyan else TextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // TAB 1: WHEEL FRICTION (PACEJKA MAGIC FORMULA)
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "PACEJKA MAGIC FORMULA (LATERAL TIRE FRICTION)",
                                        color = ApexCyan,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        "Fy = D · sin(C · arctan(B·α - E·(B·α - arctan(B·α)))) + CamberThrust",
                                        color = ApexGold,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    // Pacejka Force Curve Canvas Graph
                                    val b = 10.0f
                                    val c = 1.65f
                                    val d = 1.75f
                                    val e = -0.80f

                                    // Compute force at current slip angle
                                    val alphaRad = (slipAngleDeg * PI / 180.0).toFloat()
                                    val bx = b * alphaRad
                                    val inner = bx - e * (bx - atan(bx))
                                    val mu = d * sin(c * atan(inner))
                                    val camberThrust = normalLoadN * 0.085f * (camberDeg * PI / 180.0).toFloat()
                                    val lateralForceN = (normalLoadN * mu + camberThrust).toInt()

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0C1018))
                                            .padding(8.dp)
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val w = size.width
                                            val h = size.height
                                            val centerY = h * 0.75f

                                            // Grid lines
                                            drawLine(Color(0xFF1E2538), Offset(0f, centerY), Offset(w, centerY), strokeWidth = 1f)

                                            // Draw Pacejka curve from -15 deg to +15 deg
                                            val path = Path()
                                            val samples = 60
                                            for (i in 0..samples) {
                                                val aDeg = -15f + (30f * i / samples)
                                                val aRad = (aDeg * PI / 180.0).toFloat()
                                                val abx = b * aRad
                                                val aInner = abx - e * (abx - atan(abx))
                                                val aMu = d * sin(c * atan(aInner))
                                                val fy = normalLoadN * aMu

                                                val plotX = (aDeg + 15f) / 30f * w
                                                val plotY = centerY - (fy / 25000f) * (h * 0.65f)

                                                if (i == 0) path.moveTo(plotX, plotY) else path.lineTo(plotX, plotY)
                                            }
                                            drawPath(path, color = ApexCyan, style = Stroke(width = 3.dp.toPx()))

                                            // Marker for current slip angle
                                            val curPlotX = (slipAngleDeg + 15f) / 30f * w
                                            val curPlotY = centerY - (lateralForceN / 25000f) * (h * 0.65f)
                                            drawCircle(ApexRed, radius = 6.dp.toPx(), center = Offset(curPlotX, curPlotY))
                                        }
                                    }

                                    // Calculated Live Readouts
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("LATERAL FORCE", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            Text("$lateralForceN N", color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                        }
                                        Column {
                                            Text("TIRE MU (GRIP COEFF)", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            Text(String.format("%.3f μ", mu), color = ApexGold, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                        }
                                        Column {
                                            Text("CORNERING G", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            Text(String.format("%.2f G", (lateralForceN / (850f * 9.81f))), color = ApexRed, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                        }
                                    }

                                    // Sliders
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Tire Slip Angle (α)", color = TextSecondary, fontSize = 12.sp)
                                            Text(String.format("%.1f°", slipAngleDeg), color = TextPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = slipAngleDeg,
                                            onValueChange = { slipAngleDeg = it },
                                            valueRange = -15f..15f,
                                            colors = SliderDefaults.colors(thumbColor = ApexCyan, activeTrackColor = ApexCyan)
                                        )
                                    }

                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Vertical Normal Load (Fz incl. Aero Downforce)", color = TextSecondary, fontSize = 12.sp)
                                            Text("${normalLoadN.toInt()} N", color = TextPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = normalLoadN,
                                            onValueChange = { normalLoadN = it },
                                            valueRange = 3000f..16000f,
                                            colors = SliderDefaults.colors(thumbColor = ApexGold, activeTrackColor = ApexGold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // TAB 2: SUSPENSION DYNAMICS
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "SUSPENSION SPRING & DAMPER DYNAMICS",
                                        color = ApexCyan,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        "F_total = -k·Δx - c·v + F_bumpstop (Spring Rate: 180,000 N/m)",
                                        color = ApexGold,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    val springK = 180000.0f
                                    val damperC = if (damperVelocityMps >= 0) 7500.0f else 9500.0f
                                    val springForce = springK * (suspensionTravelCm * 0.01f)
                                    val damperForce = damperC * damperVelocityMps
                                    val totalSuspensionForce = (springForce + damperForce).toInt()

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("SPRING FORCE", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            Text("${springForce.toInt()} N", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                        }
                                        Column {
                                            Text("DAMPER FORCE", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            Text("${damperForce.toInt()} N", color = ApexGold, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                        }
                                        Column {
                                            Text("TOTAL WHEEL LOAD", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            Text("$totalSuspensionForce N", color = ApexRed, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                        }
                                    }

                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Suspension Compression Travel (Δx)", color = TextSecondary, fontSize = 12.sp)
                                            Text(String.format("%.2f cm", suspensionTravelCm), color = TextPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = suspensionTravelCm,
                                            onValueChange = { suspensionTravelCm = it },
                                            valueRange = -1.0f..5.5f,
                                            colors = SliderDefaults.colors(thumbColor = ApexCyan, activeTrackColor = ApexCyan)
                                        )
                                    }

                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Damper Velocity (v in m/s)", color = TextSecondary, fontSize = 12.sp)
                                            Text(String.format("%.2f m/s (%s)", damperVelocityMps, if (damperVelocityMps >= 0) "Bump" else "Rebound"), color = TextPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = damperVelocityMps,
                                            onValueChange = { damperVelocityMps = it },
                                            valueRange = -1.5f..1.5f,
                                            colors = SliderDefaults.colors(thumbColor = ApexGold, activeTrackColor = ApexGold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // TAB 3: ENGINE TORQUE CURVE
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "1.6L V6 TURBO HYBRID POWERTRAIN & TORQUE CURVES",
                                        color = ApexCyan,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    // Compute torque
                                    val normRpm = (testRpm - 4000f) / 11000f
                                    val curveFactor = 0.65f + 0.35f * sin(normRpm * PI.toFloat())
                                    val boostMult = 1.0f + (testBoostBar / 4.2f) * 0.65f
                                    val iceTorque = 425f * curveFactor * testThrottle * boostMult
                                    val ersTorque = if (testErsDeploy) 240f else 0f
                                    val totalDriveTorque = (iceTorque + ersTorque).toInt()
                                    val estHorsepower = ((totalDriveTorque * testRpm * 2 * PI / 60) / 745.7).toInt()

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("ICE CRANK TORQUE", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            Text("${iceTorque.toInt()} Nm", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                        }
                                        Column {
                                            Text("MGU-K HYBRID BOOST", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            Text("+${ersTorque.toInt()} Nm", color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                        }
                                        Column {
                                            Text("TOTAL OUTPUT", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            Text("$estHorsepower BHP", color = ApexGold, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                        }
                                    }

                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Engine RPM", color = TextSecondary, fontSize = 12.sp)
                                            Text("${testRpm.toInt()} RPM", color = TextPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = testRpm,
                                            onValueChange = { testRpm = it },
                                            valueRange = 4000f..15000f,
                                            colors = SliderDefaults.colors(thumbColor = ApexRed, activeTrackColor = ApexRed)
                                        )
                                    }

                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Turbocharger Boost Pressure", color = TextSecondary, fontSize = 12.sp)
                                            Text(String.format("%.2f Bar", testBoostBar), color = TextPrimary, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = testBoostBar,
                                            onValueChange = { testBoostBar = it },
                                            valueRange = 0.5f..4.2f,
                                            colors = SliderDefaults.colors(thumbColor = ApexCyan, activeTrackColor = ApexCyan)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("MGU-K Hybrid Overtake (+240 Nm / 120 kW)", color = TextPrimary, fontSize = 13.sp)
                                        Switch(
                                            checked = testErsDeploy,
                                            onCheckedChange = { testErsDeploy = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = ApexCyan, checkedTrackColor = ApexCyan.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // TAB 4: UNREAL ENGINE 5 C++ & BLUEPRINT BASE CLASS CODE
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        "UNREAL ENGINE 5 C++ & BLUEPRINT BASE CLASS",
                                        color = ApexCyan,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0A0D14))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = """
// 1. C++ Class Header: FormulaVehiclePhysics.h
UCLASS(Blueprintable, BlueprintType)
class FORMULARACING_API UFormulaVehiclePhysicsComponent : public UActorComponent
{
    // Pacejka Magic Formula Wheel Friction
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Tire")
    FPacejkaTireCoefficients FrontTirePacejka;

    // Suspension Spring & Damper Dynamics
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Suspension")
    FSuspensionAxleDynamics FrontSuspension;

    // Powertrain & Torque Specs
    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Formula|Engine")
    FPowertrainTorqueSpecs PowertrainSpecs;

    // Blueprint Callables
    UFUNCTION(BlueprintPure, Category = "Formula|Physics")
    float CalculatePacejkaLateralForce(float SlipAngleDeg, float NormalLoadN) const;

    UFUNCTION(BlueprintPure, Category = "Formula|Physics")
    float CalculateSuspensionForce(float DeflectionM, float VelocityMps) const;

    UFUNCTION(BlueprintPure, Category = "Formula|Physics")
    float EvaluateEngineTorque(float Throttle, float Rpm, bool bDeployErs) const;
};

// 2. Blueprint Base Class: BP_FormulaVehicle_Base
// Inherits: AWheeledVehiclePawn + UFormulaVehiclePhysicsComponent
// Graph: Event Tick -> UpdateSuspensionForces -> CalculatePacejkaLateralForce
// Telemetry: Hooks directly into Unreal Engine Chaos Physics Sub-stepping
                                            """.trimIndent(),
                                            color = Color(0xFF80D8FF),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
