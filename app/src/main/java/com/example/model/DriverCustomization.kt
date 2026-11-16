package com.example.model

enum class HelmetDesign(
    val title: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val visorTintHex: Long
) {
    APEX_CYAN("Apex Aerodynamic", 0xFF00E5FF, 0xFFFF1801, 0xFF1A237E),
    SCARLET_FIRE("Scuderia Inferno", 0xFFFF1801, 0xFFFFB800, 0xFF212121),
    MONACO_GOLD("Monaco Grand Prix", 0xFFFFD700, 0xFF212121, 0xFFFF6F00),
    STEALTH_CARBON("Stealth Matrix", 0xFF263238, 0xFF00E676, 0xFF000000)
}

enum class LiveryTheme(
    val title: String,
    val primaryHex: Long,
    val accentHex: Long,
    val carbonHex: Long
) {
    APEX_FACTORY("Apex Factory Racing", 0xFFFF1801, 0xFF00E5FF, 0xFF0A0D14),
    MIDNIGHT_NEON("Midnight Aero", 0xFF1A237E, 0xFF00E5FF, 0xFF050811),
    RACING_GOLD("Championship Gold", 0xFF212121, 0xFFFFB800, 0xFF12141A),
    SILVER_ARROW("Silver Velocity", 0xFFB0BEC5, 0xFF00B0FF, 0xFF1C2028)
}

data class DriverProfile(
    var driverName: String = "Liam Vance",
    var driverNumber: Int = 44,
    var helmetDesign: HelmetDesign = HelmetDesign.APEX_CYAN,
    var liveryTheme: LiveryTheme = LiveryTheme.APEX_FACTORY
)
