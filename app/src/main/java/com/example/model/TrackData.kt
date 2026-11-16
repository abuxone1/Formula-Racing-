package com.example.model

import kotlin.math.*

enum class TrackLevelId(
    val levelNumber: Int,
    val trackTitle: String,
    val country: String,
    val lengthKm: Float,
    val turnsCount: Int,
    val difficulty: String,
    val defaultWeather: WeatherCondition,
    val description: String
) {
    LEVEL_1_APEX(
        1,
        "Apex International Autodrome",
        "Apex Proving Grounds",
        5.2f,
        14,
        "INTERMEDIATE",
        WeatherCondition.DRY_SUNNY,
        "Balanced Grand Prix test circuit featuring high-speed chicanes, Curva Grande, and heavy braking hairpins."
    ),
    LEVEL_2_MONZA(
        2,
        "Monza Temple of Speed",
        "Autodromo Nazionale di Monza (Italy)",
        5.8f,
        11,
        "EXPERT",
        WeatherCondition.SUNSET,
        "Iconic low-drag circuit with 350+ km/h straights, Variante del Rettifilo, Curva Grande, Lesmo, and Parabolica."
    ),
    LEVEL_3_SPA(
        3,
        "Spa-Francorchamps Mountain Circuit",
        "Circuit de Spa-Francorchamps (Belgium)",
        7.0f,
        19,
        "LEGENDARY",
        WeatherCondition.RAIN_WET,
        "Massive 104m elevation climb through Eau Rouge and Raidillon into Kemmel Straight, Pouhon, and Blanchimont in mountain weather."
    ),
    LEVEL_4_MARINA(
        4,
        "Marina Bay Street Arena",
        "Marina Bay Street Circuit (Singapore)",
        5.0f,
        19,
        "MASTER",
        WeatherCondition.NIGHT_GP,
        "High-downforce floodlit night race lined with concrete walls, punishing 90-degree street corners and tight chicanes."
    ),
    LEVEL_5_COASTAL(
        5,
        "Monaco Coastal Seaside Circuit",
        "Monte Carlo Seaside Circuit (Monaco)",
        3.3f,
        18,
        "LEGENDARY",
        WeatherCondition.DRY_SUNNY,
        "Tight, twisting seaside circuit winding past luxurious harbors, iconic tunnel section, and harbor chicane."
    ),
    LEVEL_6_DESERT(
        6,
        "Bahrain Desert Oasis Circuit",
        "Sakhir Desert Circuit (Bahrain)",
        5.4f,
        15,
        "EXPERT",
        WeatherCondition.SUNSET,
        "Breathtaking floodlit desert oasis circuit featuring wide sweeping straights and technical traction zones."
    )
}

data class TrackNode(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val curbLeft: Boolean = false,
    val curbRight: Boolean = false,
    val drsZone: Boolean = false,
    val sector: Int = 1,
    val name: String = ""
)

data class TrackSplinePoint(
    val position: Vector3D,
    val tangent: Vector3D,
    val normal: Vector3D,
    val distanceAlongTrack: Float,
    val curbLeft: Boolean,
    val curbRight: Boolean,
    val drsZone: Boolean,
    val sector: Int
)

class CircuitTrack(
    var currentLevel: TrackLevelId = TrackLevelId.LEVEL_1_APEX
) {
    val name: String get() = currentLevel.trackTitle
    val location: String get() = currentLevel.country
    val turnsCount: Int get() = currentLevel.turnsCount
    val widthMeters: Float = 14f

    val splinePoints = mutableListOf<TrackSplinePoint>()
    var totalLengthMeters: Float = 0f

    init {
        loadLevel(currentLevel)
    }

    fun loadLevel(level: TrackLevelId) {
        currentLevel = level
        splinePoints.clear()
        val rawWaypoints = getWaypointsForLevel(level)
        buildDenseSpline(rawWaypoints)
    }

    private fun getWaypointsForLevel(level: TrackLevelId): List<TrackNode> {
        return when (level) {
            TrackLevelId.LEVEL_1_APEX -> listOf(
                TrackNode(0f, 0f, 0f, curbLeft = false, curbRight = false, drsZone = true, sector = 1, name = "Apex Main Straight"),
                TrackNode(250f, 0f, 0f, curbLeft = false, curbRight = false, drsZone = true, sector = 1),
                TrackNode(480f, 0f, 0.5f, curbLeft = false, curbRight = false, drsZone = true, sector = 1, name = "100m Braking Board"),
                TrackNode(600f, 20f, 1.2f, curbLeft = true, curbRight = false, drsZone = false, sector = 1, name = "Turn 1 Chicane Entry"),
                TrackNode(660f, 90f, 2.0f, curbLeft = false, curbRight = true, drsZone = false, sector = 1, name = "Turn 2 Apex"),
                TrackNode(700f, 200f, 2.5f, curbLeft = true, curbRight = false, drsZone = false, sector = 1),
                TrackNode(780f, 380f, 3.2f, curbLeft = false, curbRight = true, drsZone = false, sector = 1, name = "Curva Grande"),
                TrackNode(880f, 600f, 4.0f, curbLeft = false, curbRight = true, drsZone = false, sector = 1),
                TrackNode(920f, 850f, 3.5f, curbLeft = false, curbRight = true, drsZone = false, sector = 1),
                TrackNode(860f, 1100f, 2.8f, curbLeft = true, curbRight = false, drsZone = false, sector = 2, name = "Sector 2 Split"),
                TrackNode(720f, 1280f, 2.0f, curbLeft = false, curbRight = true, drsZone = false, sector = 2, name = "Turn 5 Chicane"),
                TrackNode(580f, 1340f, 1.5f, curbLeft = true, curbRight = false, drsZone = false, sector = 2),
                TrackNode(360f, 1380f, 1.0f, curbLeft = false, curbRight = true, drsZone = false, sector = 2, name = "Lesmo 1"),
                TrackNode(180f, 1350f, 0.5f, curbLeft = false, curbRight = true, drsZone = false, sector = 2, name = "Lesmo 2"),
                TrackNode(-50f, 1260f, 0.0f, curbLeft = false, curbRight = false, drsZone = true, sector = 2, name = "DRS Zone 2"),
                TrackNode(-350f, 1120f, -0.5f, curbLeft = false, curbRight = false, drsZone = true, sector = 2),
                TrackNode(-650f, 950f, -0.8f, curbLeft = false, curbRight = false, drsZone = true, sector = 2),
                TrackNode(-850f, 750f, -1.0f, curbLeft = false, curbRight = false, drsZone = false, sector = 3, name = "Sector 3 Split"),
                TrackNode(-960f, 520f, -0.5f, curbLeft = true, curbRight = false, drsZone = false, sector = 3, name = "150m Hairpin Braking"),
                TrackNode(-920f, 320f, 0.0f, curbLeft = true, curbRight = false, drsZone = false, sector = 3, name = "Apex Hairpin Apex"),
                TrackNode(-750f, 200f, 0.2f, curbLeft = false, curbRight = true, drsZone = false, sector = 3),
                TrackNode(-550f, 100f, 0.0f, curbLeft = false, curbRight = true, drsZone = false, sector = 3, name = "Parabolica Entry"),
                TrackNode(-300f, 30f, 0.0f, curbLeft = false, curbRight = true, drsZone = false, sector = 3, name = "Parabolica Exit"),
                TrackNode(-100f, 5f, 0.0f, curbLeft = false, curbRight = false, drsZone = true, sector = 3, name = "Pit Lane Entry")
            )

            TrackLevelId.LEVEL_2_MONZA -> listOf(
                TrackNode(0f, 0f, 0f, curbLeft = false, curbRight = false, drsZone = true, sector = 1, name = "Rettifilo Straight"),
                TrackNode(400f, 0f, 0f, curbLeft = false, curbRight = false, drsZone = true, sector = 1),
                TrackNode(750f, 0f, 0f, curbLeft = false, curbRight = false, drsZone = true, sector = 1, name = "350 km/h Braking"),
                TrackNode(880f, 40f, 0.2f, curbLeft = true, curbRight = false, drsZone = false, sector = 1, name = "Variante Turn 1"),
                TrackNode(920f, 110f, 0.4f, curbLeft = false, curbRight = true, drsZone = false, sector = 1, name = "Variante Turn 2"),
                TrackNode(980f, 350f, 0.8f, curbLeft = false, curbRight = true, drsZone = false, sector = 1, name = "Curva Biassono"),
                TrackNode(1050f, 750f, 1.2f, curbLeft = false, curbRight = true, drsZone = false, sector = 1),
                TrackNode(980f, 1150f, 1.0f, curbLeft = true, curbRight = false, drsZone = false, sector = 2, name = "Roggia Chicane"),
                TrackNode(880f, 1280f, 0.8f, curbLeft = false, curbRight = true, drsZone = false, sector = 2),
                TrackNode(650f, 1380f, 0.5f, curbLeft = false, curbRight = true, drsZone = false, sector = 2, name = "Prima Variante Lesmo"),
                TrackNode(400f, 1420f, 0.2f, curbLeft = false, curbRight = true, drsZone = false, sector = 2, name = "Seconda Variante Lesmo"),
                TrackNode(0f, 1350f, 0.0f, curbLeft = false, curbRight = false, drsZone = true, sector = 2, name = "Serraglio DRS 2"),
                TrackNode(-450f, 1200f, -0.2f, curbLeft = false, curbRight = false, drsZone = true, sector = 2),
                TrackNode(-850f, 980f, -0.4f, curbLeft = true, curbRight = false, drsZone = false, sector = 3, name = "Variante Ascari Entry"),
                TrackNode(-920f, 820f, -0.2f, curbLeft = false, curbRight = true, drsZone = false, sector = 3, name = "Ascari Exit"),
                TrackNode(-960f, 500f, 0.0f, curbLeft = false, curbRight = false, drsZone = false, sector = 3),
                TrackNode(-850f, 180f, 0.0f, curbLeft = false, curbRight = true, drsZone = false, sector = 3, name = "Curva Parabolica Entry"),
                TrackNode(-550f, 60f, 0.0f, curbLeft = false, curbRight = true, drsZone = false, sector = 3, name = "Parabolica Apex"),
                TrackNode(-200f, 10f, 0.0f, curbLeft = false, curbRight = true, drsZone = true, sector = 3, name = "Pit Straight Launch")
            )

            TrackLevelId.LEVEL_3_SPA -> listOf(
                TrackNode(0f, 0f, 0f, curbLeft = false, curbRight = false, drsZone = true, sector = 1, name = "Downhill Pit Straight"),
                TrackNode(250f, -40f, -8f, curbLeft = false, curbRight = false, drsZone = false, sector = 1),
                TrackNode(480f, -60f, -14f, curbLeft = true, curbRight = false, drsZone = false, sector = 1, name = "Eau Rouge Dip (-14m)"),
                TrackNode(600f, -30f, 15f, curbLeft = false, curbRight = true, drsZone = false, sector = 1, name = "Raidillon Climb (+15m)"),
                TrackNode(720f, 30f, 38f, curbLeft = true, curbRight = false, drsZone = false, sector = 1, name = "Raidillon Crest (+38m)"),
                TrackNode(950f, 200f, 42f, curbLeft = false, curbRight = false, drsZone = true, sector = 1, name = "Kemmel Straight (+42m)"),
                TrackNode(1250f, 450f, 45f, curbLeft = false, curbRight = false, drsZone = true, sector = 1),
                TrackNode(1400f, 650f, 40f, curbLeft = true, curbRight = false, drsZone = false, sector = 2, name = "Les Combes Chicane"),
                TrackNode(1420f, 820f, 34f, curbLeft = false, curbRight = true, drsZone = false, sector = 2),
                TrackNode(1300f, 1050f, 22f, curbLeft = false, curbRight = true, drsZone = false, sector = 2, name = "Bruxelles Hairpin"),
                TrackNode(1050f, 1250f, 10f, curbLeft = true, curbRight = false, drsZone = false, sector = 2, name = "Pouhon Entry (4.8G)"),
                TrackNode(780f, 1380f, 4f, curbLeft = true, curbRight = false, drsZone = false, sector = 2, name = "Pouhon Exit"),
                TrackNode(450f, 1420f, 2f, curbLeft = false, curbRight = true, drsZone = false, sector = 2, name = "Fagnes"),
                TrackNode(150f, 1350f, 0f, curbLeft = true, curbRight = false, drsZone = false, sector = 2),
                TrackNode(-250f, 1180f, -2f, curbLeft = false, curbRight = true, drsZone = true, sector = 3, name = "Curve Paul Frere"),
                TrackNode(-680f, 920f, -4f, curbLeft = true, curbRight = false, drsZone = true, sector = 3, name = "Blanchimont Flat-Out"),
                TrackNode(-950f, 550f, -6f, curbLeft = true, curbRight = false, drsZone = false, sector = 3, name = "Bus Stop Entry"),
                TrackNode(-920f, 320f, -4f, curbLeft = false, curbRight = true, drsZone = false, sector = 3, name = "Bus Stop Apex"),
                TrackNode(-700f, 150f, -2f, curbLeft = false, curbRight = true, drsZone = false, sector = 3, name = "La Source Hairpin"),
                TrackNode(-350f, 40f, 0f, curbLeft = false, curbRight = false, drsZone = true, sector = 3)
            )

            TrackLevelId.LEVEL_4_MARINA -> listOf(
                TrackNode(0f, 0f, 0f, curbLeft = false, curbRight = false, drsZone = true, sector = 1, name = "Marina Pit Straight"),
                TrackNode(260f, 0f, 0f, curbLeft = false, curbRight = false, drsZone = true, sector = 1),
                TrackNode(360f, 30f, 0f, curbLeft = true, curbRight = false, drsZone = false, sector = 1, name = "Turn 1 Left"),
                TrackNode(420f, 90f, 0f, curbLeft = false, curbRight = true, drsZone = false, sector = 1, name = "Turn 2 Right"),
                TrackNode(460f, 180f, 0f, curbLeft = true, curbRight = false, drsZone = false, sector = 1, name = "Turn 3 Left"),
                TrackNode(540f, 380f, 0f, curbLeft = false, curbRight = false, drsZone = true, sector = 1, name = "Republic Blvd DRS"),
                TrackNode(600f, 620f, 0f, curbLeft = false, curbRight = false, drsZone = true, sector = 1),
                TrackNode(580f, 780f, 0f, curbLeft = false, curbRight = true, drsZone = false, sector = 2, name = "Turn 5 90-Deg"),
                TrackNode(420f, 920f, 0f, curbLeft = false, curbRight = false, drsZone = true, sector = 2, name = "Raffles Blvd DRS"),
                TrackNode(150f, 1020f, 0f, curbLeft = false, curbRight = false, drsZone = true, sector = 2),
                TrackNode(-80f, 980f, 0f, curbLeft = true, curbRight = false, drsZone = false, sector = 2, name = "Memorial Corner"),
                TrackNode(-320f, 880f, 0f, curbLeft = false, curbRight = true, drsZone = false, sector = 2),
                TrackNode(-520f, 740f, 0f, curbLeft = true, curbRight = false, drsZone = false, sector = 2, name = "Padang Sweep"),
                TrackNode(-680f, 560f, 0f, curbLeft = true, curbRight = false, drsZone = false, sector = 3, name = "Singapore Sling"),
                TrackNode(-690f, 380f, 0f, curbLeft = false, curbRight = true, drsZone = false, sector = 3),
                TrackNode(-620f, 220f, 0f, curbLeft = true, curbRight = false, drsZone = false, sector = 3, name = "Anderson Bridge"),
                TrackNode(-480f, 120f, 0f, curbLeft = false, curbRight = true, drsZone = false, sector = 3, name = "Grandstand Underpass"),
                TrackNode(-250f, 30f, 0f, curbLeft = true, curbRight = false, drsZone = true, sector = 3, name = "Final Double-Apex")
            )

            TrackLevelId.LEVEL_5_COASTAL -> listOf(
                TrackNode(0f, 0f, 0f, curbLeft = false, curbRight = false, drsZone = true, sector = 1, name = "Seaside Straight"),
                TrackNode(300f, 20f, 1f, curbLeft = true, curbRight = false, drsZone = false, sector = 1, name = "Harbor Chicane Entry"),
                TrackNode(450f, 120f, 2f, curbLeft = false, curbRight = true, drsZone = false, sector = 1, name = "Tabac Corner"),
                TrackNode(380f, 320f, 3f, curbLeft = true, curbRight = false, drsZone = false, sector = 1, name = "Swimming Pool Complex"),
                TrackNode(150f, 500f, 1f, curbLeft = false, curbRight = true, drsZone = false, sector = 2, name = "Rascasse Hairpin"),
                TrackNode(-100f, 420f, 0f, curbLeft = true, curbRight = false, drsZone = false, sector = 2, name = "Antony Noghes"),
                TrackNode(-350f, 250f, 0f, curbLeft = false, curbRight = true, drsZone = true, sector = 3, name = "Tunnel Straight"),
                TrackNode(-200f, 50f, 0f, curbLeft = true, curbRight = false, drsZone = false, sector = 3, name = "Mirabeau Curve")
            )

            TrackLevelId.LEVEL_6_DESERT -> listOf(
                TrackNode(0f, 0f, 0f, curbLeft = false, curbRight = false, drsZone = true, sector = 1, name = "Oasis Start Straight"),
                TrackNode(350f, 0f, 1f, curbLeft = false, curbRight = false, drsZone = true, sector = 1),
                TrackNode(700f, 50f, 2f, curbLeft = true, curbRight = false, drsZone = false, sector = 1, name = "Turn 1 Heavy Braking"),
                TrackNode(850f, 250f, 4f, curbLeft = false, curbRight = true, drsZone = false, sector = 1, name = "Desert Hairpin"),
                TrackNode(700f, 600f, 2f, curbLeft = true, curbRight = false, drsZone = true, sector = 2, name = "Back Straight DRS"),
                TrackNode(300f, 850f, 1f, curbLeft = false, curbRight = true, drsZone = false, sector = 2, name = "Inner Loop"),
                TrackNode(-150f, 700f, 0f, curbLeft = true, curbRight = false, drsZone = false, sector = 3, name = "Oasis Sweeper"),
                TrackNode(-400f, 300f, 0f, curbLeft = false, curbRight = true, drsZone = true, sector = 3, name = "Final Straight")
            )
        }
    }

    private fun buildDenseSpline(rawWaypoints: List<TrackNode>) {
        val n = rawWaypoints.size
        val samplesPerSegment = 25
        var accumulatedDist = 0f

        for (i in 0 until n) {
            val p0 = rawWaypoints[(i - 1 + n) % n]
            val p1 = rawWaypoints[i]
            val p2 = rawWaypoints[(i + 1) % n]
            val p3 = rawWaypoints[(i + 2) % n]

            for (step in 0 until samplesPerSegment) {
                val t = step.toFloat() / samplesPerSegment.toFloat()
                val pos = catmullRom(p0, p1, p2, p3, t)
                val nextPos = catmullRom(p0, p1, p2, p3, t + 0.01f)

                val dx = nextPos.x - pos.x
                val dy = nextPos.y - pos.y
                val dz = nextPos.z - pos.z
                val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.001f)

                val tangent = Vector3D(dx / len, dy / len, dz / len)
                val normal = Vector3D(-tangent.y, tangent.x, 0f)

                if (splinePoints.isNotEmpty()) {
                    val prev = splinePoints.last().position
                    accumulatedDist += pos.distanceTo(prev)
                }

                splinePoints.add(
                    TrackSplinePoint(
                        position = pos,
                        tangent = tangent,
                        normal = normal,
                        distanceAlongTrack = accumulatedDist,
                        curbLeft = p1.curbLeft,
                        curbRight = p1.curbRight,
                        drsZone = p1.drsZone,
                        sector = p1.sector
                    )
                )
            }
        }
        totalLengthMeters = accumulatedDist
    }

    private fun catmullRom(p0: TrackNode, p1: TrackNode, p2: TrackNode, p3: TrackNode, t: Float): Vector3D {
        val t2 = t * t
        val t3 = t2 * t

        fun calc(c0: Float, c1: Float, c2: Float, c3: Float): Float {
            return 0.5f * (
                (2f * c1) +
                (-c0 + c2) * t +
                (2f * c0 - 5f * c1 + 4f * c2 - c3) * t2 +
                (-c0 + 3f * c1 - 3f * c2 + c3) * t3
            )
        }

        return Vector3D(
            x = calc(p0.x, p1.x, p2.x, p3.x),
            y = calc(p0.y, p1.y, p2.y, p3.y),
            z = calc(p0.z, p1.z, p2.z, p3.z)
        )
    }

    fun findNearestSplinePoint(worldX: Float, worldY: Float): TrackSplinePoint {
        var closest = splinePoints.first()
        var minDistSq = Float.MAX_VALUE

        for (i in 0 until splinePoints.size) {
            val pt = splinePoints[i]
            val dx = pt.position.x - worldX
            val dy = pt.position.y - worldY
            val distSq = dx * dx + dy * dy
            if (distSq < minDistSq) {
                minDistSq = distSq
                closest = pt
            }
        }
        return closest
    }

    fun findNearestSplinePointNearDistance(
        worldX: Float,
        worldY: Float,
        hintDistanceMeters: Float
    ): TrackSplinePoint {
        val size = splinePoints.size
        if (size == 0) return getPointAtDistance(0f)
        val s = (hintDistanceMeters % totalLengthMeters + totalLengthMeters) % totalLengthMeters
        val centerIdx = ((s / totalLengthMeters) * size).toInt().coerceIn(0, size - 1)

        var closest = splinePoints[centerIdx]
        var minDistSq = Float.MAX_VALUE
        val window = 28
        for (offset in -window..window) {
            val idx = ((centerIdx + offset) % size + size) % size
            val pt = splinePoints[idx]
            val dx = pt.position.x - worldX
            val dy = pt.position.y - worldY
            val distSq = dx * dx + dy * dy
            if (distSq < minDistSq) {
                minDistSq = distSq
                closest = pt
            }
        }
        return if (minDistSq < 1600f) closest else findNearestSplinePoint(worldX, worldY)
    }

    fun getLateralOffset(worldX: Float, worldY: Float, point: TrackSplinePoint): Float {
        val dx = worldX - point.position.x
        val dy = worldY - point.position.y
        return dx * point.normal.x + dy * point.normal.y
    }

    fun getPointAtDistance(distanceMeters: Float): TrackSplinePoint {
        if (splinePoints.isEmpty()) return TrackSplinePoint(Vector3D(), Vector3D(1f, 0f, 0f), Vector3D(0f, 1f, 0f), 0f, false, false, false, 1)
        val s = (distanceMeters % totalLengthMeters + totalLengthMeters) % totalLengthMeters
        val index = ((s / totalLengthMeters) * splinePoints.size).toInt().coerceIn(0, splinePoints.size - 1)
        return splinePoints[index]
    }
}
