package com.example.model

enum class GraphicsQuality(
    val title: String,
    val subtitle: String,
    val targetFps: Int,
    val lookaheadMeters: Float,
    val trackStepMeters: Float,
    val maxParticles: Int,
    val enableShadows: Boolean,
    val enableReflections: Boolean,
    val enableSpectators: Boolean,
    val enableSuspensionStruts: Boolean,
    val textureResolution: Int
) {
    LOW(
        title = "Low (Battery Saver)",
        subtitle = "30 FPS • Simplified meshes • Low heat",
        targetFps = 30,
        lookaheadMeters = 140f,
        trackStepMeters = 10f,
        maxParticles = 18,
        enableShadows = false,
        enableReflections = false,
        enableSpectators = false,
        enableSuspensionStruts = false,
        textureResolution = 128
    ),
    MEDIUM(
        title = "Medium (Balanced)",
        subtitle = "60 FPS • 3D cars & barriers • Soft shadows",
        targetFps = 60,
        lookaheadMeters = 210f,
        trackStepMeters = 7f,
        maxParticles = 48,
        enableShadows = true,
        enableReflections = false,
        enableSpectators = true,
        enableSuspensionStruts = true,
        textureResolution = 256
    ),
    HIGH(
        title = "High (Ultra 3D)",
        subtitle = "60 FPS • Glossy reflections, crowds & full VFX",
        targetFps = 60,
        lookaheadMeters = 260f,
        trackStepMeters = 5f,
        maxParticles = 96,
        enableShadows = true,
        enableReflections = true,
        enableSpectators = true,
        enableSuspensionStruts = true,
        textureResolution = 512
    )
}

enum class RaceIntroPhase {
    NONE,
    CIRCUIT_FLYOVER,
    GRID_SHOWCASE,
    CAMERA_HANDOFF
}

enum class AIDifficulty(
    val displayName: String,
    val description: String,
    val speedScale: Float,
    val accelScale: Float,
    val cornerGripScale: Float,
    val overtakeAgility: Float,
    val aggression: Float,
    val reactionDelaySec: Float
) {
    BEGINNER(
        displayName = "Beginner",
        description = "Slower pace • Early braking • Easy to overtake",
        speedScale = 0.74f,
        accelScale = 0.72f,
        cornerGripScale = 0.80f,
        overtakeAgility = 0.8f,
        aggression = 0.20f,
        reactionDelaySec = 0.45f
    ),
    INTERMEDIATE(
        displayName = "Intermediate",
        description = "Balanced race pace • Fair midfield battles",
        speedScale = 0.88f,
        accelScale = 0.87f,
        cornerGripScale = 0.91f,
        overtakeAgility = 1.6f,
        aggression = 0.50f,
        reactionDelaySec = 0.25f
    ),
    PROFESSIONAL(
        displayName = "Professional",
        description = "Competitive Grand Prix pace • Defensive lines",
        speedScale = 0.97f,
        accelScale = 0.96f,
        cornerGripScale = 0.98f,
        overtakeAgility = 2.5f,
        aggression = 0.78f,
        reactionDelaySec = 0.14f
    ),
    ELITE(
        displayName = "Elite",
        description = "Maximum apex grip • Aggressive ERS & slipstream overtakes",
        speedScale = 1.04f,
        accelScale = 1.04f,
        cornerGripScale = 1.05f,
        overtakeAgility = 3.5f,
        aggression = 0.95f,
        reactionDelaySec = 0.06f
    )
}

enum class EngineSpec(
    val displayName: String,
    val subtitle: String,
    val peakTorqueNm: Float,
    val redlineRpm: Float,
    val mgukTorqueNm: Float,
    val fuelBurnRate: Float,
    val durability: Float
) {
    APEX_WORKS(
        displayName = "Apex Works V6",
        subtitle = "Balanced Factory Power • 1020 BHP",
        peakTorqueNm = 445f,
        redlineRpm = 15000f,
        mgukTorqueNm = 230f,
        fuelBurnRate = 0.015f,
        durability = 1.00f
    ),
    MODENA_TURBO(
        displayName = "Modena Scuderia V6",
        subtitle = "Peak Top-End Power • 1065 BHP",
        peakTorqueNm = 472f,
        redlineRpm = 15200f,
        mgukTorqueNm = 240f,
        fuelBurnRate = 0.018f,
        durability = 0.92f
    ),
    SILVER_HYBRID(
        displayName = "Silver Arrow HPP",
        subtitle = "High Torque + ERS Boost • 1040 BHP",
        peakTorqueNm = 456f,
        redlineRpm = 14800f,
        mgukTorqueNm = 260f,
        fuelBurnRate = 0.014f,
        durability = 0.98f
    ),
    VORTEX_ECO(
        displayName = "Vortex Endurance",
        subtitle = "Cool & Fuel Efficient • 975 BHP",
        peakTorqueNm = 415f,
        redlineRpm = 14400f,
        mgukTorqueNm = 205f,
        fuelBurnRate = 0.011f,
        durability = 1.12f
    )
}

enum class TireCompound(
    val displayName: String,
    val gripMultiplier: Float,
    val wearRatePerKm: Float,
    val optimumTempC: Float,
    val colorHex: Long
) {
    SOFT("Soft (C5)", 1.08f, 0.045f, 95f, 0xFFFF2A2A),
    MEDIUM("Medium (C3)", 1.00f, 0.025f, 102f, 0xFFFFD600),
    HARD("Hard (C1)", 0.94f, 0.012f, 110f, 0xFFFFFFFF),
    WET("Intermediate / Wet", 0.90f, 0.030f, 75f, 0xFF2979FF)
}

data class CarSetup(
    var frontWingAngle: Float = 26f, // degrees (15 - 35)
    var rearWingAngle: Float = 32f,  // degrees (20 - 45)
    var brakeBias: Float = 56f,      // % front (50 - 62)
    var tireCompound: TireCompound = TireCompound.SOFT,
    var fuelLiters: Float = 45f,     // kg/liters
    var maxSpeedKph: Float = 345f,   // Adjustable top speed limiter (220 - 375 km/h)
    var engineSpec: EngineSpec = EngineSpec.APEX_WORKS,
    var steeringSensitivity: Float = 1.0f, // 0.5x to 1.6x
    var springStiffness: Float = 1.0f      // 0.7x to 1.3x
) {
    // Aerodynamic downforce scalar based on wing angles
    val downforceScalar: Float
        get() = 0.85f + (frontWingAngle / 35f) * 0.25f + (rearWingAngle / 45f) * 0.35f

    // Aerodynamic drag scalar
    val dragScalar: Float
        get() = 0.80f + (frontWingAngle / 35f) * 0.20f + (rearWingAngle / 45f) * 0.40f
}

enum class CameraMode(val label: String) {
    COCKPIT("Cockpit Cam"),
    CHASE("Chase Cam"),
    T_CAM("T-Cam (Onboard)"),
    TV_BROADCAST("TV Broadcast")
}

enum class WeatherCondition(
    val title: String,
    val ambientGrip: Float,
    val isRain: Boolean,
    val lightIntensity: Float
) {
    DRY_SUNNY("Sunny Daytime", 1.0f, false, 1.0f),
    CLOUDY("Cloudy Conditions", 0.98f, false, 0.85f),
    HEAVY_RAIN("Heavy Rainfall", 0.68f, true, 0.55f),
    WET_TRACK("Wet Racing Surfaces", 0.80f, false, 0.80f),
    FOGGY("Foggy Conditions", 0.92f, false, 0.60f),
    SUNSET("Sunset Racing", 0.98f, false, 0.75f),
    NIGHT_GP("Night Racing", 0.95f, false, 0.40f),
    RAIN_WET("Heavy Rain - Wet Track", 0.72f, true, 0.70f)
}

data class Vector3D(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f
) {
    fun distanceTo(other: Vector3D): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }
}

data class VehicleState(
    var position: Vector3D = Vector3D(),
    var headingRad: Float = 0f,      // Yaw heading in radians
    var pitchRad: Float = 0f,        // Suspension dive/squat
    var rollRad: Float = 0f,         // Body roll under lateral G
    var speedMs: Float = 0f,         // Speed in meters/sec
    var rpm: Float = 4200f,          // 4000 - 15200 RPM
    var gear: Int = 1,               // 1 to 8 (0 = N, -1 = R)
    var steerInput: Float = 0f,      // -1.0 (full left) to +1.0 (full right)
    var effectiveSteer: Float = 0f,  // Smoothed steering actuator output
    var throttleInput: Float = 0f,   // 0.0 to 1.0
    var brakeInput: Float = 0f,      // 0.0 to 1.0
    var drsAvailable: Boolean = false,
    var drsOpen: Boolean = false,
    var ersEnergy: Float = 0.95f,    // 0.0 to 1.0 (Battery state of charge)
    var ersDeploying: Boolean = false,
    var tireWear: FloatArray = floatArrayOf(0.02f, 0.02f, 0.02f, 0.02f), // FL, FR, RL, RR
    var tireTemps: FloatArray = floatArrayOf(92f, 92f, 95f, 95f),        // Celsius
    var brakeDiscTemp: Float = 480f, // Celsius
    var lateralG: Float = 0f,
    var longitudinalG: Float = 0f,
    var frontAxleLoadN: Float = 3850f,
    var rearAxleLoadN: Float = 4350f,
    var lateralLoadRatio: Float = 0.5f, // 0 = full left load, 1 = full right load
    var damageFrontWing: Float = 0f, // 0.0 (pristine) to 1.0 (destroyed)
    var damageAeroFloor: Float = 0f, // 0.0 to 1.0
    var damageEngine: Float = 0f,    // 0.0 to 1.0
    var impactCooldownTimer: Float = 0f,
    var isOffTrack: Boolean = false,
    var isOnCurb: Boolean = false,
    var inPitLane: Boolean = false,
    var isPitLimiterOn: Boolean = false,
    var currentTrackDistance: Float = 0f,
    var wheelSpinAngleRad: Float = 0f,
    var exhaustPopTimer: Float = 0f,
    var suspensionBounceOffset: Float = 0f,
    var offTrackTimeAccumSec: Float = 0f,
    var recoveryCooldownSec: Float = 0f
) {
    val speedKph: Float get() = speedMs * 3.6f

    val overallDamage: Float
        get() = (damageFrontWing * 0.45f + damageAeroFloor * 0.30f + damageEngine * 0.25f).coerceIn(0f, 1f)

    fun reset() {
        position = Vector3D()
        headingRad = 0f
        pitchRad = 0f
        rollRad = 0f
        speedMs = 0f
        rpm = 4200f
        gear = 1
        steerInput = 0f
        effectiveSteer = 0f
        throttleInput = 0f
        brakeInput = 0f
        drsAvailable = false
        drsOpen = false
        ersDeploying = false
        tireWear = floatArrayOf(0.02f, 0.02f, 0.02f, 0.02f)
        tireTemps = floatArrayOf(92f, 92f, 95f, 95f)
        damageFrontWing = 0f
        damageAeroFloor = 0f
        damageEngine = 0f
        impactCooldownTimer = 0f
        isOffTrack = false
        isOnCurb = false
        inPitLane = false
        isPitLimiterOn = false
        currentTrackDistance = 0f
        wheelSpinAngleRad = 0f
        exhaustPopTimer = 0f
        suspensionBounceOffset = 0f
        lateralG = 0f
        longitudinalG = 0f
        frontAxleLoadN = 3850f
        rearAxleLoadN = 4350f
        lateralLoadRatio = 0.5f
        brakeDiscTemp = 480f
        offTrackTimeAccumSec = 0f
        recoveryCooldownSec = 0f
    }
}

data class RaceStandingEntry(
    val position: Int,
    val driverName: String,
    val shortCode: String,
    val teamName: String,
    val number: Int,
    val isPlayer: Boolean,
    val completedLaps: Int,
    val totalProgressMeters: Float,
    val finished: Boolean,
    val completionTimeMs: Long,
    val penaltySeconds: Float
) {
    val totalAdjustedTimeMs: Long
        get() = completionTimeMs + (penaltySeconds * 1000f).toLong()
}

data class Driver(
    val name: String,
    val shortCode: String,
    val team: String,
    val number: Int,
    val teamColorHex: Long,
    val aiSkill: Float = 0.90f,
    var championshipPoints: Int = 0,
    var bestLapMs: Long = 0L,
    var currentGapSeconds: Float = 0f
)

enum class GameScreen {
    MAIN_MENU,
    RACE,
    LEVEL_SELECT,
    PHYSICS_LAB,
    GARAGE,
    CHAMPIONSHIP,
    SETTINGS,
    REPLAY_PHOTO_MODE,
    TELEMETRY_ANALYSIS,
    DRIVER_CUSTOMIZATION,
    ARCHITECTURE_DOCS,
    CLOUD_LEADERBOARD,
    DAILY_CHALLENGES,
    ACHIEVEMENTS,
    RACE_PREP,
    ONLINE_MULTIPLAYER
}

enum class ControlType {
    VIRTUAL_WHEEL,
    LEFT_RIGHT_BUTTONS,
    TILT_SENSOR
}

data class TelemetrySample(
    val distanceM: Float,
    val speedKph: Float,
    val throttle: Float,
    val brake: Float,
    val gear: Int,
    val lateralG: Float,
    val rpm: Float
)
