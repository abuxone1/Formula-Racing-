package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchitectureDocsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Level 2: Formula Car", "Task 1: UE5 Setup", "Overview", "UE5 C++ Core", "Android AAB & PAD", "Google Play Release")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "APEX STUDIOS ARCHITECTURE SPEC",
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("arch_back_button")) {
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
                        item {
                            DocCard(
                                title = "LEVEL 2: BP_FORMULACAR_01 & CHAOS VEHICLES ARCHITECTURE",
                                content = """
LEVEL 2 STEP-BY-STEP IMPLEMENTATION GUIDE:

1. Blueprint Creation:
   • In Content Browser: Content/FormulaRacing/Blueprints/
   • Right click -> Blueprint Class -> Parent: AFormulaVehiclePawn.
   • Name: BP_FormulaCar_01.

2. Component Hierarchy:
   [BP_FormulaCar_01] (Self)
   ├── Mesh (SkeletalMesh: SK_FormulaCar_01, AnimClass: ABP_FormulaCar_01)
   │   ├── ChaseSpringArm (Length: 650cm, Offset Z: 180cm, Cam Lag: 18.0)
   │   │   └── ChaseCamera (Third Person Follow View)
   │   ├── CockpitCamera (Inside Halo, X=-25, Z=75)
   │   ├── TCamera (Onboard Roll-Hoop Airbox, X=-60, Z=125)
   │   └── EngineSoundComponent (V6 Turbo Hybrid MetaSound / Sound Cue)
   └── VehicleMovementComponent (ChaosWheeledVehicleMovementComponent)
       ├── Wheel Setups:
       │   ├── [0] WheelClass: UFormulaWheelFront | Bone: wheel_fl
       │   ├── [1] WheelClass: UFormulaWheelFront | Bone: wheel_fr
       │   ├── [2] WheelClass: UFormulaWheelRear  | Bone: wheel_rl
       │   └── [3] WheelClass: UFormulaWheelRear  | Bone: wheel_rr
       ├── Mechanical Engine:
       │   ├── Max RPM: 15,000 | Idle RPM: 4,200 | Max Torque: 425 Nm
       │   └── Torque Curve: FloatCurve peak at 11,500 RPM
       ├── 8-Speed Seamless Transmission:
       │   ├── Automatic Gears: Checked (Toggleable in C++)
       │   ├── Gear Ratios: [3.25, 2.50, 2.02, 1.66, 1.41, 1.23, 1.09, 0.97]
       │   └── Final Drive: 3.85 | Gear Change Time: 0.04s
       └── Aerodynamic Downforce:
           ├── Base Cl: 1.35 (Generates 1,400 kg downforce at 250 km/h)
           └── DRS Wing Flap: 30% drag reduction down straightaways

3. Animation Blueprint (ABP_FormulaCar_01):
   • Parent: UFormulaCarAnimationInstance
   • AnimGraph:
     Mesh Ref Pose ➔ Wheel Controller ➔ Transform (Modify) Bone [steering_wheel] ➔ Output Pose.

4. Physics Asset (PHYS_FormulaCar_01):
   • Chassis Body: Single Convex Hull (Simulated).
   • 4 Wheels: Kinematic Spheres (Collision disabled, Chaos sweeps them).

5. Testing & Validation:
   • Place BP_FormulaCar_01 into LV_ApexAutodrome_Empty at PlayerStart.
   • Set Auto Possess Player = Player 0.
   • Press Play (PIE):
     ✓ W / Right Trigger: Accelerates through 8 gears to 340+ km/h.
     ✓ S / Left Trigger: Decelerates at 5.0G braking.
     ✓ A / D / Left Stick: High-speed steered cornering without rollover.
     ✓ C / Gamepad Select: Toggles Chase Cam ➔ Cockpit View ➔ T-Cam.
     ✓ Space / Top Button: Actuates DRS wing flap.
                                """.trimIndent()
                            )
                        }
                    }
                    1 -> {
                        item {
                            DocCard(
                                title = "TASK 1: COMPLETE UNREAL ENGINE 5 & ANDROID SETUP",
                                content = """
TASK 1 STEP-BY-STEP IMPLEMENTATION GUIDE:

1. Create Project:
   • Open Unreal Engine 5.4+ -> New Project -> C++ -> Blank (No Starter Content).
   • Project Name: FormulaRacing
   • Target Platform: Mobile | Quality Preset: Scalable.

2. Enable Core Plugins (FormulaRacing.uproject):
   • Chaos Vehicles Plugin (for 4-wheel open-wheel physics).
   • Enhanced Input (for mobile touch steering, tilt, and gamepad).
   • Android Device Profile Selector & Android Permission.

3. Modular Project Folder Architecture:
   FormulaRacing/
   ├── Config/
   │   ├── DefaultEngine.ini (Landscape, Vulkan, Mobile MSAA)
   │   ├── DefaultGame.ini (Studio branding & metadata)
   │   ├── DefaultInput.ini (Touch steering & pedals)
   │   └── DefaultScalability.ini (30/60 FPS mobile targets)
   ├── Content/FormulaRacing/
   │   ├── Blueprints/ (Vehicles, GameModes, Controllers)
   │   ├── Maps/ (LV_ApexAutodrome_Empty)
   │   ├── Materials/ (Asphalt, Curbs, Carbon Fiber)
   │   └── UI/ (Touchscreen HUD, Telemetry Widgets)
   └── Source/FormulaRacing/
       ├── GameModes/ (FormulaGameModeBase.h / .cpp)
       ├── Vehicles/ (FormulaVehiclePawn.h / .cpp)
       └── Physics/ (FormulaVehiclePhysics.h / .cpp)

4. Main Game Mode (FormulaGameModeBase):
   • Implements 5 red starting lights countdown ("Lights out and away we go!").
   • Tracks session time, race states (PreGrid, Countdown, Racing, Finished).

5. Empty Racing Environment (LV_ApexAutodrome_Empty):
   • Add Directional Light (Sun) + SkyAtmosphere + ExponentialHeightFog.
   • Add SkyLight (Real-time capture).
   • Add 200m x 20m flat road mesh / landscape plane.
   • Place PlayerStart actor at (X=0, Y=0, Z=100) facing +X.

6. Android Compilation & Testing:
   • Command Line:
     RunUAT.bat BuildCookRun -project="FormulaRacing.uproject" -platform=Android -cook -build -stage -pak -package
   • Editor: Platforms -> Android -> Package Project (or Quick Launch to connected device).
   • Verify: App launches in horizontal landscape with 60 FPS and 5 starting lights!
                                """.trimIndent()
                            )
                        }
                    }
                    2 -> {
                        item {
                            DocCard(
                                title = "APEX DYNAMICS MOTORSPORT ARCHITECTURE",
                                content = """
Project: Car Racing: FORMULA
Studio: Apex Dynamics Motorsport Studios
Slogan: Dream. Build. Race.
Target Engine: Unreal Engine 5.4+ / Android Native NDK & Jetpack

Core Architecture Modules:
1. Vehicle Simulation Subsystem (Chaos Vehicles / Custom Pacejka C++ Core)
2. Aerodynamics Module (Surface area, downforce vectors, DRS flap animation)
3. Powertrain & Seamless Gearbox (1.6L V6 Turbo + 120kW MGU-K Hybrid)
4. Dynamic Surface & Tire Degradation (Thermal diffusion, carcass wear)
5. Spline-Driven Racing AI (Lookahead cornering, slipstreaming, overtaking)
6. Telemetry & HUD Layer (LED shift lights, live delta, brake bias, ERS)
7. Audio Synthesizer (Harmonic engine synthesis, turbo whistle, downshift pops)
                                """.trimIndent()
                            )
                        }
                    }
                    3 -> {
                        item {
                            DocCard(
                                title = "UNREAL ENGINE 5 C++ CORE INTERFACE",
                                content = """
// Source/FormulaRacing/Public/Vehicles/FormulaVehiclePawn.h
#pragma once
#include "CoreMinimal.h"
#include "WheeledVehiclePawn.h"
#include "FormulaVehiclePawn.generated.h"

UCLASS()
class FORMULARACING_API AFormulaVehiclePawn : public AWheeledVehiclePawn
{
    GENERATED_BODY()
public:
    AFormulaVehiclePawn();

    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Aero")
    float FrontWingAngleDegrees = 26.0f;

    UPROPERTY(EditAnywhere, BlueprintReadWrite, Category = "Aero")
    float RearWingAngleDegrees = 32.0f;

    UPROPERTY(BlueprintReadOnly, Category = "DRS")
    bool bIsDRSActive = false;

    UPROPERTY(BlueprintReadOnly, Category = "Hybrid")
    float ERSEnergyRemaining = 1.0f;

    UFUNCTION(BlueprintCallable, Category = "Simulation")
    void DeployERSBoost(bool bActivate);

    UFUNCTION(BlueprintCallable, Category = "Simulation")
    void ToggleDRSFlap();
};
                                """.trimIndent(),
                                isCode = true
                            )
                        }
                    }
                    4 -> {
                        item {
                            DocCard(
                                title = "ANDROID APP BUNDLE (.AAB) & ASSET DELIVERY",
                                content = """
Google Play Asset Delivery (PAD) Structure:
• Base Module: Core executable (.so / dex), initial track ('Apex Autodrome')
• On-Demand Fast-Follow Pack: 'Monza_HighSpeed_Pack.pak' (Vulkan PBR assets)
• Install-Time Asset Pack: High-resolution vehicle liveries & 3D sound banks

Packaging Command:
gradle :app:bundleRelease

Target Architectures:
• arm64-v8a (Primary high-performance target)
• Vulkan 1.2+ API minimum with ASTC texture compression
                                """.trimIndent()
                            )
                        }
                    }
                    5 -> {
                        item {
                            DocCard(
                                title = "GOOGLE PLAY CONSOLE PRODUCTION COMPLIANCE",
                                content = """
Google Play Store Deliverables:
• App Name: Car Racing: FORMULA (Under 30 chars limit)
• Icon: 512x512 px PNG (configured in mipmap folders)
• Feature Graphic: 1024x500 px (Hero banner render ready)
• Target SDK: Android 36 (Compliant with latest Google Play requirements)
• Permissions: Minimal least-privilege (VIBRATE, INTERNET)
• Version Code: 2 (Version Name 2.0)
• Google Play Games Services: Leaderboards & Cloud Save schema
• Google Play Billing: In-App Purchases for Team Liveries & Seasons
                                """.trimIndent()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocCard(title: String, content: String, isCode: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CarbonSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                title,
                color = ApexCyan,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0C0F17))
                    .padding(12.dp)
            ) {
                Text(
                    text = content,
                    color = if (isCode) Color(0xFF81D4FA) else TextPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
