package com.example.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.model.CircuitTrack
import com.example.model.GraphicsQuality
import com.example.model.TrackLevelId
import com.example.model.TrackSplinePoint
import com.example.model.Vector3D
import com.example.model.WeatherCondition
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/**
 * Renders the 3D racing environment: dynamic weather skybox & parallax skyline,
 * textured asphalt ribbon with near-plane clipping and back-to-front Z-sorting,
 * raised 3D kerbs, chequered grid, Armco/TecPro/street barriers, 3D grandstands with
 * animated spectators, braking boards, floodlight towers, trees, and start gantry.
 */
class FormulaEnvironmentRenderer {

    private val reusablePath = Path()
    private val horizonPath = Path()

    private val inCamQuad = Array(4) { CamPoint3D() }
    private val clippedCamQuad = Array(8) { CamPoint3D() }
    private val outSX = FloatArray(8)
    private val outSY = FloatArray(8)
    private val projTmp = FloatArray(3)
    private val projTmp2 = FloatArray(3)
    private val projTmp3 = FloatArray(3)
    private val projTmp4 = FloatArray(3)

    private val maxSegments = 96
    private val segIndices = IntArray(maxSegments)
    private val segOrder = IntArray(maxSegments)
    private val segDepth = FloatArray(maxSegments)

    private val stars = Array(36) {
        Triple(Random.nextFloat(), Random.nextFloat() * 0.42f, 1.0f + Random.nextFloat() * 1.5f)
    }

    private val skylineHeights = FloatArray(28) { i ->
        0.06f + ((sin(i * 1.7f) + 1f) * 0.055f) + ((i % 3) * 0.025f)
    }

    private val spectatorPalette = arrayOf(
        Color(0xFFFF1801),
        Color(0xFF00E5FF),
        Color(0xFFFFD600),
        Color(0xFFFFFFFF),
        Color(0xFF00E676),
        Color(0xFFFF6D00)
    )

    fun drawSkyAndAtmosphere(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        weather: WeatherCondition,
        trackLevel: TrackLevelId,
        camera: Camera3D,
        quality: GraphicsQuality
    ) {
        val horizonY = height * 0.50f
        val (skyTop, skyMid, skyHorizon, groundBase) = when (weather) {
            WeatherCondition.DRY_SUNNY -> QuadColors(
                Color(0xFF0D47A1), Color(0xFF1E88E5), Color(0xFF90CAF9), Color(0xFF1B3A1D)
            )
            WeatherCondition.CLOUDY -> QuadColors(
                Color(0xFF37474F), Color(0xFF546E7A), Color(0xFF90A4AE), Color(0xFF263238)
            )
            WeatherCondition.HEAVY_RAIN, WeatherCondition.RAIN_WET -> QuadColors(
                Color(0xFF1C2833), Color(0xFF2E4053), Color(0xFF5D6D7E), Color(0xFF17202A)
            )
            WeatherCondition.WET_TRACK -> QuadColors(
                Color(0xFF2C3E50), Color(0xFF34495E), Color(0xFF85929E), Color(0xFF1F2A38)
            )
            WeatherCondition.FOGGY -> QuadColors(
                Color(0xFF7F8C8D), Color(0xFF95A5A6), Color(0xFFBDC3C7), Color(0xFF5D6D7E)
            )
            WeatherCondition.SUNSET -> QuadColors(
                Color(0xFF1A1040), Color(0xFF8E24AA), Color(0xFFFF7043), Color(0xFF23180E)
            )
            WeatherCondition.NIGHT_GP -> QuadColors(
                Color(0xFF03050C), Color(0xFF0A1120), Color(0xFF132238), Color(0xFF080B12)
            )
        }

        // 1. Multi-stop Sky Gradient
        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(skyTop, skyMid, skyHorizon),
                startY = 0f,
                endY = horizonY + 24f
            ),
            topLeft = Offset(0f, 0f),
            size = Size(width, horizonY + 24f)
        )

        // Heading-based parallax scroll factor
        val camYaw = atan2(camera.forward.y, camera.forward.x)
        val yawNorm = (camYaw / (2f * PI.toFloat()))

        // 2. Celestial Bodies & Weather Atmosphere
        when (weather) {
            WeatherCondition.DRY_SUNNY -> {
                val sunX = (((0.68f - yawNorm * 0.6f) % 1.2f + 1.2f) % 1.2f - 0.1f) * width
                val sunY = height * 0.19f
                drawScope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x66FFF59D), Color(0x00FFF59D)),
                        center = Offset(sunX, sunY),
                        radius = height * 0.22f
                    ),
                    radius = height * 0.22f,
                    center = Offset(sunX, sunY)
                )
                drawScope.drawCircle(Color(0xFFFFFDE7), radius = 18f, center = Offset(sunX, sunY))
            }
            WeatherCondition.CLOUDY, WeatherCondition.WET_TRACK -> {
                if (quality != GraphicsQuality.LOW) {
                    for (c in 0 until 4) {
                        val cx = (((c * 0.3f - yawNorm * 0.25f) % 1.2f + 1.2f) % 1.2f - 0.1f) * width
                        val cy = height * (0.12f + (c % 2) * 0.06f)
                        drawScope.drawOval(Color(0x33B0BEC5), Offset(cx - 100f, cy), Size(200f, 36f))
                    }
                }
            }
            WeatherCondition.HEAVY_RAIN, WeatherCondition.RAIN_WET -> {
                if (quality != GraphicsQuality.LOW) {
                    for (c in 0 until 6) {
                        val cx = (((c * 0.22f - yawNorm * 0.2f) % 1.2f + 1.2f) % 1.2f - 0.1f) * width
                        val cy = height * (0.08f + (c % 2) * 0.08f)
                        drawScope.drawOval(Color(0x381B252B), Offset(cx - 120f, cy), Size(240f, 44f))
                    }
                }
            }
            WeatherCondition.FOGGY -> {
                drawScope.drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x00BDC3C7), Color(0x66BDC3C7)),
                        startY = horizonY * 0.6f,
                        endY = horizonY + 40f
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(width, horizonY + 40f)
                )
            }
            WeatherCondition.SUNSET -> {
                val sunX = (((0.55f - yawNorm * 0.6f) % 1.2f + 1.2f) % 1.2f - 0.1f) * width
                val sunY = horizonY - height * 0.07f
                drawScope.drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xAAFFAB40), Color(0x33FF5722), Color(0x00FF5722)),
                        center = Offset(sunX, sunY),
                        radius = height * 0.30f
                    ),
                    radius = height * 0.30f,
                    center = Offset(sunX, sunY)
                )
                drawScope.drawCircle(Color(0xFFFFCC80), radius = 26f, center = Offset(sunX, sunY))
            }
            WeatherCondition.NIGHT_GP -> {
                val starCount = if (quality == GraphicsQuality.LOW) 16 else stars.size
                for (i in 0 until starCount) {
                    val (sxNorm, syNorm, sr) = stars[i]
                    val sx = (((sxNorm - yawNorm * 0.35f) % 1f + 1f) % 1f) * width
                    val sy = syNorm * height
                    drawScope.drawCircle(Color(0xBBDCE8FF), radius = sr, center = Offset(sx, sy))
                }
            }
        }

        // 3. Circuit-Specific Parallax Horizon Silhouettes
        if (trackLevel == TrackLevelId.LEVEL_4_MARINA || weather == WeatherCondition.NIGHT_GP) {
            // Marina Bay Illuminated City Skyline
            val bCount = skylineHeights.size
            val bWidth = width / 16f
            val scrollOffset = ((yawNorm * 2.2f) % 1f + 1f) % 1f
            for (i in 0 until bCount) {
                val bx = (((i.toFloat() / bCount) - scrollOffset + 1f) % 1f) * (width + bWidth * 2f) - bWidth
                val bh = height * skylineHeights[i]
                val bTop = horizonY - bh
                val bColor = if (weather == WeatherCondition.NIGHT_GP) Color(0xFF0E1624) else Color(0xFF283548)
                drawScope.drawRect(
                    color = bColor,
                    topLeft = Offset(bx, bTop),
                    size = Size(bWidth * 0.88f, bh + 8f)
                )
                if (quality != GraphicsQuality.LOW && i % 2 == 0) {
                    // Glowing skyline accent strip / illuminated windows
                    val winColor = if (i % 4 == 0) Color(0x5500E5FF) else Color(0x44FFD54F)
                    drawScope.drawRect(
                        color = winColor,
                        topLeft = Offset(bx + bWidth * 0.18f, bTop + 6f),
                        size = Size(bWidth * 0.52f, 3f)
                    )
                }
            }
        }

        // Distant Hills / Treeline Silhouette + Ground Plane
        horizonPath.reset()
        horizonPath.moveTo(0f, horizonY)
        val steps = 8
        val stepW = width / steps
        for (i in 0 until steps) {
            val curX = i * stepW
            val nextX = curX + stepW
            val wave = sin((i * 0.9f) + camYaw * 3.0f)
            val hillAmp = if (trackLevel == TrackLevelId.LEVEL_3_SPA) 28f else 15f
            val peakY = horizonY - hillAmp - wave * 11f
            horizonPath.quadraticTo(curX + stepW * 0.5f, peakY, nextX, horizonY)
        }
        horizonPath.lineTo(width, height)
        horizonPath.lineTo(0f, height)
        horizonPath.close()

        val hillTint = when (weather) {
            WeatherCondition.NIGHT_GP -> Color(0xFF0A0F1A)
            WeatherCondition.SUNSET -> Color(0xFF2B1C14)
            WeatherCondition.RAIN_WET, WeatherCondition.HEAVY_RAIN, WeatherCondition.WET_TRACK -> Color(0xFF1D2824)
            WeatherCondition.DRY_SUNNY, WeatherCondition.CLOUDY, WeatherCondition.FOGGY -> Color(0xFF1A361C)
            else -> Color(0xFF1A361C)
        }
        drawScope.drawPath(horizonPath, color = hillTint)

        // Lower Ground Backdrop
        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(groundBase, Color(0xFF0B0E14)),
                startY = horizonY,
                endY = height
            ),
            topLeft = Offset(0f, horizonY),
            size = Size(width, height - horizonY)
        )
    }

    fun drawTrackAndStructures(
        drawScope: DrawScope,
        camera: Camera3D,
        textures: FormulaTextures,
        circuit: CircuitTrack,
        focusDistanceMeters: Float,
        weather: WeatherCondition,
        quality: GraphicsQuality,
        startLightsState: Int,
        width: Float,
        height: Float
    ) {
        val totalLen = circuit.totalLengthMeters
        if (totalLen <= 1f || circuit.splinePoints.isEmpty()) return

        val stepDist = quality.trackStepMeters
        val startOffset = -28f
        val endOffset = quality.lookaheadMeters
        val startStep = floor(startOffset / stepDist).toInt()
        val endStep = floor(endOffset / stepDist).toInt()
        val count = (endStep - startStep + 1).coerceIn(0, maxSegments)

        // Snap segment index to world-anchored grid so textures/kerbs/props don't shimmer
        val baseStepIdx = floor(focusDistanceMeters / stepDist).toInt()

        for (i in 0 until count) {
            val worldStepIdx = baseStepIdx + startStep + i
            val dMid = (worldStepIdx + 0.5f) * stepDist
            val ptMid = circuit.getPointAtDistance(dMid)
            camera.toCameraSpace(ptMid.position.x, ptMid.position.y, ptMid.position.z, inCamQuad[0])
            segIndices[i] = worldStepIdx
            segDepth[i] = inCamQuad[0].z
            segOrder[i] = i
        }

        // Depth sort track segments back-to-front (supports chase, TV broadcast & aerial intro!)
        for (i in 1 until count) {
            val key = segOrder[i]
            val keyD = segDepth[key]
            var j = i - 1
            while (j >= 0 && segDepth[segOrder[j]] < keyD) {
                segOrder[j + 1] = segOrder[j]
                j--
            }
            segOrder[j + 1] = key
        }

        val halfW = circuit.widthMeters * 0.5f
        val isStreetTrack = (circuit.currentLevel == TrackLevelId.LEVEL_4_MARINA)
        val roadBrush = if (weather.isRain) textures.asphaltWetBrush else textures.asphaltDryBrush
        val shoulderBrush = if (isStreetTrack) textures.streetShoulderBrush else textures.turfGrassBrush

        for (k in 0 until count) {
            val slot = segOrder[k]
            if (segDepth[slot] < -12f) continue

            val worldStepIdx = segIndices[slot]
            val d1 = worldStepIdx * stepDist
            val d2 = (worldStepIdx + 1) * stepDist
            val pt1 = circuit.getPointAtDistance(d1)
            val pt2 = circuit.getPointAtDistance(d2)
            val isEven = ((worldStepIdx and 1) == 0)
            val distMod = (d1 % totalLen + totalLen) % totalLen

            // Outer road edge points
            val l1x = pt1.position.x - pt1.normal.x * halfW
            val l1y = pt1.position.y - pt1.normal.y * halfW
            val r1x = pt1.position.x + pt1.normal.x * halfW
            val r1y = pt1.position.y + pt1.normal.y * halfW

            val l2x = pt2.position.x - pt2.normal.x * halfW
            val l2y = pt2.position.y - pt2.normal.y * halfW
            val r2x = pt2.position.x + pt2.normal.x * halfW
            val r2y = pt2.position.y + pt2.normal.y * halfW

            // 1. Trackside Grass / Urban Runoff Shoulders
            val shoulderW = 13f
            drawClippedQuad(
                drawScope, camera,
                l1x - pt1.normal.x * shoulderW, l1y - pt1.normal.y * shoulderW, pt1.position.z - 0.04f,
                l1x, l1y, pt1.position.z - 0.02f,
                l2x, l2y, pt2.position.z - 0.02f,
                l2x - pt2.normal.x * shoulderW, l2y - pt2.normal.y * shoulderW, pt2.position.z - 0.04f,
                width, height,
                brush = if (quality != GraphicsQuality.LOW) shoulderBrush else null,
                solidColor = if (isStreetTrack) Color(0xFF141820) else Color(0xFF18361B)
            )
            drawClippedQuad(
                drawScope, camera,
                r1x, r1y, pt1.position.z - 0.02f,
                r1x + pt1.normal.x * shoulderW, r1y + pt1.normal.y * shoulderW, pt1.position.z - 0.04f,
                r2x + pt2.normal.x * shoulderW, r2y + pt2.normal.y * shoulderW, pt2.position.z - 0.04f,
                r2x, r2y, pt2.position.z - 0.02f,
                width, height,
                brush = if (quality != GraphicsQuality.LOW) shoulderBrush else null,
                solidColor = if (isStreetTrack) Color(0xFF141820) else Color(0xFF18361B)
            )

            // 2. Textured Asphalt Surface Quad (with near-plane Sutherland-Hodgman clipping!)
            drawClippedQuad(
                drawScope, camera,
                l1x, l1y, pt1.position.z,
                r1x, r1y, pt1.position.z,
                r2x, r2y, pt2.position.z,
                l2x, l2y, pt2.position.z,
                width, height,
                brush = roadBrush,
                solidColor = Color(0xFF252A33)
            )

            // Subtle segment contrast & wet specular sheen on HIGH quality
            if (quality == GraphicsQuality.HIGH && (weather.isRain || weather == WeatherCondition.NIGHT_GP)) {
                val specularW = halfW * 0.42f
                drawClippedQuad(
                    drawScope, camera,
                    pt1.position.x - pt1.normal.x * specularW, pt1.position.y - pt1.normal.y * specularW, pt1.position.z + 0.01f,
                    pt1.position.x + pt1.normal.x * specularW, pt1.position.y + pt1.normal.y * specularW, pt1.position.z + 0.01f,
                    pt2.position.x + pt2.normal.x * specularW, pt2.position.y + pt2.normal.y * specularW, pt2.position.z + 0.01f,
                    pt2.position.x - pt2.normal.x * specularW, pt2.position.y - pt2.normal.y * specularW, pt2.position.z + 0.01f,
                    width, height,
                    brush = null,
                    solidColor = if (weather.isRain) Color(0x1C80D8FF) else Color(0x14FFE082)
                )
            }

            // 3. White Track Boundary Lines & DRS Zone Stripes
            if (camera.projectFast(l1x, l1y, pt1.position.z + 0.02f, width, height, projTmp) &&
                camera.projectFast(l2x, l2y, pt2.position.z + 0.02f, width, height, projTmp2)
            ) {
                val lineColor = if (pt1.drsZone) Color(0xFF00E5FF) else Color(0xDDE8ECF2)
                drawScope.drawLine(lineColor, Offset(projTmp[0], projTmp[1]), Offset(projTmp2[0], projTmp2[1]), strokeWidth = 2.6f)
            }
            if (camera.projectFast(r1x, r1y, pt1.position.z + 0.02f, width, height, projTmp) &&
                camera.projectFast(r2x, r2y, pt2.position.z + 0.02f, width, height, projTmp2)
            ) {
                val lineColor = if (pt1.drsZone) Color(0xFF00E5FF) else Color(0xDDE8ECF2)
                drawScope.drawLine(lineColor, Offset(projTmp[0], projTmp[1]), Offset(projTmp2[0], projTmp2[1]), strokeWidth = 2.6f)
            }

            // 4. Chequered Start/Finish Line & Staggered Grid Boxes
            if (distMod < stepDist * 1.1f) {
                drawChequeredFinishStrip(drawScope, camera, pt1, pt2, halfW, width, height)
            } else if (distMod in 10f..45f && isEven) {
                drawGridSlotMark(drawScope, camera, pt1, pt2, offsetM = 2.6f, width, height)
            } else if (distMod > totalLen - 35f && !isEven) {
                drawGridSlotMark(drawScope, camera, pt1, pt2, offsetM = -2.6f, width, height)
            }

            // 5. Raised 3D Red/White Apex Kerbs
            val curbW = 1.45f
            if (pt1.curbLeft) {
                val curbColor = if (isEven) Color(0xFFD50000) else Color(0xFFF5F5F5)
                drawClippedQuad(
                    drawScope, camera,
                    l1x - pt1.normal.x * curbW, l1y - pt1.normal.y * curbW, pt1.position.z + 0.08f,
                    l1x, l1y, pt1.position.z + 0.02f,
                    l2x, l2y, pt2.position.z + 0.02f,
                    l2x - pt2.normal.x * curbW, l2y - pt2.normal.y * curbW, pt2.position.z + 0.08f,
                    width, height,
                    brush = null,
                    solidColor = curbColor
                )
            }
            if (pt1.curbRight) {
                val curbColor = if (isEven) Color(0xFFD50000) else Color(0xFFF5F5F5)
                drawClippedQuad(
                    drawScope, camera,
                    r1x, r1y, pt1.position.z + 0.02f,
                    r1x + pt1.normal.x * curbW, r1y + pt1.normal.y * curbW, pt1.position.z + 0.08f,
                    r2x + pt2.normal.x * curbW, r2y + pt2.normal.y * curbW, pt2.position.z + 0.08f,
                    r2x, r2y, pt2.position.z + 0.02f,
                    width, height,
                    brush = null,
                    solidColor = curbColor
                )
            }

            // 6. Trackside Barriers (Armco, TecPro corner blocks, Street Concrete Walls)
            if (quality != GraphicsQuality.LOW || isStreetTrack) {
                drawBarriersForSegment(
                    drawScope, camera, pt1, pt2, halfW, isStreetTrack, isEven, quality, width, height
                )
            }

            // 7. Roadside Grandstands, Spectators, Braking Boards, Floodlights & Trees
            val segMod = ((worldStepIdx % 16) + 16) % 16
            val isMainStraight = distMod < 95f || distMod > totalLen - 65f

            if (isMainStraight && (segMod == 0 || segMod == 4 || segMod == 8 || segMod == 12)) {
                drawGrandstand3D(drawScope, camera, pt1, pt2, halfW + 7.5f, side = -1f, quality, weather, width, height)
            } else if ((pt1.curbLeft || pt1.curbRight) && segMod == 2 && quality != GraphicsQuality.LOW) {
                val gSide = if (pt1.curbRight) 1f else -1f
                drawGrandstand3D(drawScope, camera, pt1, pt2, halfW + 8.5f, side = gSide, quality, weather, width, height)
            }

            // Trees along non-street circuits
            if (!isStreetTrack && quality != GraphicsQuality.LOW && !isMainStraight && (segMod == 1 || segMod == 7 || segMod == 13)) {
                drawTree3D(drawScope, camera, pt1, halfW + 11.5f, side = if ((worldStepIdx and 2) == 0) 1f else -1f, weather, width, height)
            }

            // Stadium Floodlights in Night GP or Marina Bay
            if ((weather == WeatherCondition.NIGHT_GP || isStreetTrack) && (segMod == 3 || segMod == 11)) {
                drawFloodlightTower(drawScope, camera, pt1, halfW + 4.8f, side = if (segMod == 3) -1f else 1f, quality, width, height)
            }

            // Corner Approach Braking Boards (150m / 100m / 50m markers before curbed zones)
            if (!pt1.curbLeft && !pt1.curbRight && (pt2.curbLeft || pt2.curbRight)) {
                drawBrakingBoard(drawScope, camera, pt1, halfW + 2.8f, side = if (pt2.curbLeft) -1f else 1f, width, height)
            }

            // 8. Start/Finish Gantry & Starting Lights
            if (distMod < stepDist * 1.1f) {
                drawStartFinishGantry(drawScope, camera, pt1, halfW, startLightsState, width, height)
            }
        }
    }

    private fun drawBarriersForSegment(
        drawScope: DrawScope,
        camera: Camera3D,
        pt1: TrackSplinePoint,
        pt2: TrackSplinePoint,
        halfW: Float,
        isStreetTrack: Boolean,
        isEven: Boolean,
        quality: GraphicsQuality,
        width: Float,
        height: Float
    ) {
        val barrierDist = if (isStreetTrack) halfW + 1.8f else halfW + 4.2f
        val wallHeight = if (isStreetTrack) 1.55f else 1.15f
        val isCorner = pt1.curbLeft || pt1.curbRight

        val leftColor = when {
            isStreetTrack -> if (isEven) Color(0xFF343B48) else Color(0xFF282E3A)
            isCorner -> if (isEven) Color(0xFFD32F2F) else Color(0xFFE0E4EC)
            else -> if (isEven) Color(0xFF546070) else Color(0xFF45505E)
        }
        val rightColor = when {
            isStreetTrack -> if (isEven) Color(0xFF343B48) else Color(0xFF282E3A)
            isCorner -> if (isEven) Color(0xFFE0E4EC) else Color(0xFFD32F2F)
            else -> if (isEven) Color(0xFF546070) else Color(0xFF45505E)
        }

        // Left Barrier Wall
        val bl1x = pt1.position.x - pt1.normal.x * barrierDist
        val bl1y = pt1.position.y - pt1.normal.y * barrierDist
        val bl2x = pt2.position.x - pt2.normal.x * barrierDist
        val bl2y = pt2.position.y - pt2.normal.y * barrierDist
        drawClippedQuad(
            drawScope, camera,
            bl1x, bl1y, pt1.position.z,
            bl2x, bl2y, pt2.position.z,
            bl2x, bl2y, pt2.position.z + wallHeight,
            bl1x, bl1y, pt1.position.z + wallHeight,
            width, height,
            brush = null,
            solidColor = leftColor
        )

        // Right Barrier Wall
        val br1x = pt1.position.x + pt1.normal.x * barrierDist
        val br1y = pt1.position.y + pt1.normal.y * barrierDist
        val br2x = pt2.position.x + pt2.normal.x * barrierDist
        val br2y = pt2.position.y + pt2.normal.y * barrierDist
        drawClippedQuad(
            drawScope, camera,
            br1x, br1y, pt1.position.z,
            br2x, br2y, pt2.position.z,
            br2x, br2y, pt2.position.z + wallHeight,
            br1x, br1y, pt1.position.z + wallHeight,
            width, height,
            brush = null,
            solidColor = rightColor
        )

        // Top Neon/Metallic Guardrail Highlight on Medium/High
        if (quality != GraphicsQuality.LOW) {
            val topTint = if (isStreetTrack) Color(0xFF00E5FF) else Color(0xFF90A4AE)
            if (camera.projectFast(bl1x, bl1y, pt1.position.z + wallHeight, width, height, projTmp) &&
                camera.projectFast(bl2x, bl2y, pt2.position.z + wallHeight, width, height, projTmp2)
            ) {
                drawScope.drawLine(topTint, Offset(projTmp[0], projTmp[1]), Offset(projTmp2[0], projTmp2[1]), strokeWidth = 2f)
            }
            if (camera.projectFast(br1x, br1y, pt1.position.z + wallHeight, width, height, projTmp) &&
                camera.projectFast(br2x, br2y, pt2.position.z + wallHeight, width, height, projTmp2)
            ) {
                drawScope.drawLine(topTint, Offset(projTmp[0], projTmp[1]), Offset(projTmp2[0], projTmp2[1]), strokeWidth = 2f)
            }
        }
    }

    private fun drawGrandstand3D(
        drawScope: DrawScope,
        camera: Camera3D,
        pt1: TrackSplinePoint,
        pt2: TrackSplinePoint,
        offsetDist: Float,
        side: Float,
        quality: GraphicsQuality,
        weather: WeatherCondition,
        width: Float,
        height: Float
    ) {
        val frontDist = offsetDist
        val backDist = offsetDist + 10.5f
        val d = side

        val f1x = pt1.position.x + pt1.normal.x * frontDist * d
        val f1y = pt1.position.y + pt1.normal.y * frontDist * d
        val f2x = pt2.position.x + pt2.normal.x * frontDist * d
        val f2y = pt2.position.y + pt2.normal.y * frontDist * d

        val b1x = pt1.position.x + pt1.normal.x * backDist * d
        val b1y = pt1.position.y + pt1.normal.y * backDist * d
        val b2x = pt2.position.x + pt2.normal.x * backDist * d
        val b2y = pt2.position.y + pt2.normal.y * backDist * d

        val baseZ = pt1.position.z
        val midZ = baseZ + 3.4f
        val topZ = baseZ + 6.8f
        val roofFrontZ = baseZ + 8.6f
        val roofBackZ = baseZ + 9.2f

        val standTone = if (weather == WeatherCondition.NIGHT_GP) Color(0xFF1A2230) else Color(0xFF2D3648)
        val canopyTone = if (weather == WeatherCondition.NIGHT_GP) Color(0xFF253044) else Color(0xFF3E4A60)

        // Lower Concrete Retaining Wall
        drawClippedQuad(
            drawScope, camera,
            f1x, f1y, baseZ,
            f2x, f2y, baseZ,
            f2x, f2y, baseZ + 1.4f,
            f1x, f1y, baseZ + 1.4f,
            width, height, null, Color(0xFF1E2430)
        )

        // Sloped Tiered Spectator Seating Deck
        drawClippedQuad(
            drawScope, camera,
            f1x, f1y, baseZ + 1.4f,
            b1x, b1y, topZ,
            b2x, b2y, topZ,
            f2x, f2y, baseZ + 1.4f,
            width, height, null, standTone
        )

        // Roof Canopy
        drawClippedQuad(
            drawScope, camera,
            f1x, f1y, roofFrontZ,
            b1x, b1y, roofBackZ,
            b2x, b2y, roofBackZ,
            f2x, f2y, roofFrontZ,
            width, height, null, canopyTone
        )

        // Support Pillars
        if (camera.projectFast(b1x, b1y, baseZ, width, height, projTmp) &&
            camera.projectFast(b1x, b1y, roofBackZ, width, height, projTmp2)
        ) {
            drawScope.drawLine(Color(0xFF526078), Offset(projTmp[0], projTmp[1]), Offset(projTmp2[0], projTmp2[1]), strokeWidth = 4f)
        }

        // Animated Multi-Colored Spectators in the Grandstand Tiers
        if (quality.enableSpectators) {
            val animPhase = (System.currentTimeMillis() / 160L)
            val rows = if (quality == GraphicsQuality.HIGH) 3 else 2
            val cols = if (quality == GraphicsQuality.HIGH) 6 else 4
            for (r in 0 until rows) {
                val rt = 0.2f + r * 0.30f
                val sz = (baseZ + 1.7f) + rt * (topZ - (baseZ + 1.7f))
                for (c in 0 until cols) {
                    val ct = (c + 0.5f) / cols
                    val sx = (f1x + (b1x - f1x) * rt) * (1f - ct) + (f2x + (b2x - f2x) * rt) * ct
                    val sy = (f1y + (b1y - f1y) * rt) * (1f - ct) + (f2y + (b2y - f2y) * rt) * ct
                    val waveBob = if (((r + c + animPhase.toInt()) and 3) == 0) 0.25f else 0f
                    if (camera.projectFast(sx, sy, sz + waveBob, width, height, projTmp)) {
                        val sc = (110f / projTmp[2]).coerceIn(0.4f, 2.2f)
                        val fanColor = spectatorPalette[(r * 5 + c) % spectatorPalette.size]
                        drawScope.drawCircle(
                            color = fanColor,
                            radius = (2.6f * sc).coerceIn(1.5f, 6.5f),
                            center = Offset(projTmp[0], projTmp[1])
                        )
                    }
                }
            }
        }
    }

    private fun drawTree3D(
        drawScope: DrawScope,
        camera: Camera3D,
        pt: TrackSplinePoint,
        offsetDist: Float,
        side: Float,
        weather: WeatherCondition,
        width: Float,
        height: Float
    ) {
        val tx = pt.position.x + pt.normal.x * offsetDist * side
        val ty = pt.position.y + pt.normal.y * offsetDist * side
        val tz = pt.position.z

        if (!camera.projectFast(tx, ty, tz, width, height, projTmp)) return
        if (!camera.projectFast(tx, ty, tz + 6.5f, width, height, projTmp2)) return

        val depth = projTmp[2]
        if (depth > 220f) return

        val baseX = projTmp[0]
        val baseY = projTmp[1]
        val topY = projTmp2[1]
        val treeH = abs(baseY - topY).coerceIn(14f, 160f)
        val crownW = treeH * 0.42f

        // Trunk
        drawScope.drawLine(
            color = Color(0xFF4E342E),
            start = Offset(baseX, baseY),
            end = Offset(baseX, baseY - treeH * 0.35f),
            strokeWidth = (treeH * 0.08f).coerceIn(2f, 8f)
        )

        // Layered Evergreen Foliage
        val leafColor = when (weather) {
            WeatherCondition.NIGHT_GP -> Color(0xFF0E2614)
            WeatherCondition.SUNSET -> Color(0xFF284820)
            WeatherCondition.RAIN_WET, WeatherCondition.HEAVY_RAIN, WeatherCondition.WET_TRACK -> Color(0xFF193822)
            WeatherCondition.DRY_SUNNY, WeatherCondition.CLOUDY, WeatherCondition.FOGGY -> Color(0xFF2E7D32)
            else -> Color(0xFF2E7D32)
        }
        reusablePath.reset()
        reusablePath.moveTo(baseX, topY)
        reusablePath.lineTo(baseX - crownW, baseY - treeH * 0.25f)
        reusablePath.lineTo(baseX + crownW, baseY - treeH * 0.25f)
        reusablePath.close()
        drawScope.drawPath(reusablePath, color = leafColor)
    }

    private fun drawFloodlightTower(
        drawScope: DrawScope,
        camera: Camera3D,
        pt: TrackSplinePoint,
        offsetDist: Float,
        side: Float,
        quality: GraphicsQuality,
        width: Float,
        height: Float
    ) {
        val fx = pt.position.x + pt.normal.x * offsetDist * side
        val fy = pt.position.y + pt.normal.y * offsetDist * side
        val fz = pt.position.z

        if (!camera.projectFast(fx, fy, fz, width, height, projTmp)) return
        if (!camera.projectFast(fx, fy, fz + 11f, width, height, projTmp2)) return

        val depth = projTmp[2]
        if (depth > 230f) return

        drawScope.drawLine(
            color = Color(0xFF455A64),
            start = Offset(projTmp[0], projTmp[1]),
            end = Offset(projTmp2[0], projTmp2[1]),
            strokeWidth = (180f / depth).coerceIn(2f, 6f)
        )

        val headX = projTmp2[0]
        val headY = projTmp2[1]
        val r = (130f / depth).coerceIn(3f, 14f)
        drawScope.drawCircle(Color(0x55FFF9C4), radius = r * 2.3f, center = Offset(headX, headY))
        drawScope.drawCircle(Color(0xFFFFFDE7), radius = r, center = Offset(headX, headY))

        if (quality == GraphicsQuality.HIGH) {
            if (camera.projectFast(pt.position.x, pt.position.y, pt.position.z, width, height, projTmp3)) {
                reusablePath.reset()
                reusablePath.moveTo(headX, headY)
                reusablePath.lineTo(projTmp3[0] - r * 3.5f, projTmp3[1])
                reusablePath.lineTo(projTmp3[0] + r * 3.5f, projTmp3[1])
                reusablePath.close()
                drawScope.drawPath(reusablePath, color = Color(0x16FFF8E1))
            }
        }
    }

    private fun drawBrakingBoard(
        drawScope: DrawScope,
        camera: Camera3D,
        pt: TrackSplinePoint,
        offsetDist: Float,
        side: Float,
        width: Float,
        height: Float
    ) {
        val bx = pt.position.x + pt.normal.x * offsetDist * side
        val by = pt.position.y + pt.normal.y * offsetDist * side
        val bz = pt.position.z

        if (!camera.projectFast(bx, by, bz + 1.4f, width, height, projTmp)) return
        val depth = projTmp[2]
        if (depth > 150f) return
        val sc = (110f / depth).coerceIn(0.35f, 2.5f)
        val bw = 18f * sc
        val bh = 12f * sc

        // Sign post + high-contrast braking board
        drawScope.drawLine(Color(0xFF78909C), Offset(projTmp[0], projTmp[1]), Offset(projTmp[0], projTmp[1] + bh * 1.4f), strokeWidth = 2f * sc)
        drawScope.drawRect(Color.White, Offset(projTmp[0] - bw * 0.5f, projTmp[1] - bh * 0.5f), Size(bw, bh))
        drawScope.drawRect(Color(0xFFD50000), Offset(projTmp[0] - bw * 0.42f, projTmp[1] - bh * 0.25f), Size(bw * 0.84f, bh * 0.5f))
    }

    private fun drawChequeredFinishStrip(
        drawScope: DrawScope,
        camera: Camera3D,
        pt1: TrackSplinePoint,
        pt2: TrackSplinePoint,
        halfW: Float,
        width: Float,
        height: Float
    ) {
        val squares = 8
        val stepW = (halfW * 2f) / squares
        for (s in 0 until squares) {
            val u0 = -halfW + s * stepW
            val u1 = u0 + stepW
            val sqColor = if ((s and 1) == 0) Color(0xFFF5F5F5) else Color(0xFF101216)
            drawClippedQuad(
                drawScope, camera,
                pt1.position.x + pt1.normal.x * u0, pt1.position.y + pt1.normal.y * u0, pt1.position.z + 0.03f,
                pt1.position.x + pt1.normal.x * u1, pt1.position.y + pt1.normal.y * u1, pt1.position.z + 0.03f,
                pt2.position.x + pt2.normal.x * u1, pt2.position.y + pt2.normal.y * u1, pt2.position.z + 0.03f,
                pt2.position.x + pt2.normal.x * u0, pt2.position.y + pt2.normal.y * u0, pt2.position.z + 0.03f,
                width, height, null, sqColor
            )
        }
    }

    private fun drawGridSlotMark(
        drawScope: DrawScope,
        camera: Camera3D,
        pt1: TrackSplinePoint,
        pt2: TrackSplinePoint,
        offsetM: Float,
        width: Float,
        height: Float
    ) {
        val hw = 1.05f
        val x1L = pt1.position.x + pt1.normal.x * (offsetM - hw)
        val y1L = pt1.position.y + pt1.normal.y * (offsetM - hw)
        val x1R = pt1.position.x + pt1.normal.x * (offsetM + hw)
        val y1R = pt1.position.y + pt1.normal.y * (offsetM + hw)
        val x2L = pt2.position.x + pt2.normal.x * (offsetM - hw)
        val y2L = pt2.position.y + pt2.normal.y * (offsetM - hw)
        val x2R = pt2.position.x + pt2.normal.x * (offsetM + hw)
        val y2R = pt2.position.y + pt2.normal.y * (offsetM + hw)

        if (camera.projectFast(x1L, y1L, pt1.position.z + 0.03f, width, height, projTmp) &&
            camera.projectFast(x1R, y1R, pt1.position.z + 0.03f, width, height, projTmp2) &&
            camera.projectFast(x2L, y2L, pt2.position.z + 0.03f, width, height, projTmp3) &&
            camera.projectFast(x2R, y2R, pt2.position.z + 0.03f, width, height, projTmp4)
        ) {
            val boxColor = Color(0xCCFFFFFF)
            drawScope.drawLine(boxColor, Offset(projTmp3[0], projTmp3[1]), Offset(projTmp4[0], projTmp4[1]), strokeWidth = 2.5f)
            drawScope.drawLine(boxColor, Offset(projTmp[0], projTmp[1]), Offset(projTmp3[0], projTmp3[1]), strokeWidth = 2.5f)
            drawScope.drawLine(boxColor, Offset(projTmp2[0], projTmp2[1]), Offset(projTmp4[0], projTmp4[1]), strokeWidth = 2.5f)
        }
    }

    private fun drawStartFinishGantry(
        drawScope: DrawScope,
        camera: Camera3D,
        pt: TrackSplinePoint,
        halfWidth: Float,
        startLightsState: Int,
        width: Float,
        height: Float
    ) {
        val postDist = halfWidth + 2.2f
        val pLx = pt.position.x - pt.normal.x * postDist
        val pLy = pt.position.y - pt.normal.y * postDist
        val pRx = pt.position.x + pt.normal.x * postDist
        val pRy = pt.position.y + pt.normal.y * postDist
        val zBot = pt.position.z
        val zTop = zBot + 7.2f

        if (!camera.projectFast(pLx, pLy, zBot, width, height, projTmp) ||
            !camera.projectFast(pRx, pRy, zBot, width, height, projTmp2) ||
            !camera.projectFast(pLx, pLy, zTop, width, height, projTmp3) ||
            !camera.projectFast(pRx, pRy, zTop, width, height, projTmp4)
        ) return

        val avgDepth = (projTmp3[2] + projTmp4[2]) * 0.5f
        val sc = (110f / avgDepth).coerceIn(0.4f, 3.0f)

        // Truss columns
        drawScope.drawLine(Color(0xFF37474F), Offset(projTmp[0], projTmp[1]), Offset(projTmp3[0], projTmp3[1]), strokeWidth = 6f * sc)
        drawScope.drawLine(Color(0xFF37474F), Offset(projTmp2[0], projTmp2[1]), Offset(projTmp4[0], projTmp4[1]), strokeWidth = 6f * sc)

        // Overhead Bridge Panel + Racing Red Accent
        drawScope.drawLine(Color(0xFF151922), Offset(projTmp3[0], projTmp3[1]), Offset(projTmp4[0], projTmp4[1]), strokeWidth = 18f * sc)
        drawScope.drawLine(Color(0xFFFF1801), Offset(projTmp3[0], projTmp3[1] - 6f * sc), Offset(projTmp4[0], projTmp4[1] - 6f * sc), strokeWidth = 3f * sc)

        // 5 Red Formula Starting Lights
        for (lightIdx in 0 until 5) {
            val t = 0.34f + lightIdx * 0.08f
            val lx = projTmp3[0] + (projTmp4[0] - projTmp3[0]) * t
            val ly = projTmp3[1] + (projTmp4[1] - projTmp3[1]) * t + 10f * sc

            val isLit = (startLightsState in (lightIdx + 1)..5)
            val lightColor = if (isLit) Color(0xFFFF1801) else Color(0xFF1D222C)
            if (isLit) {
                drawScope.drawCircle(Color(0x66FF1801), radius = 12f * sc, center = Offset(lx, ly))
            }
            drawScope.drawCircle(lightColor, radius = 5.5f * sc, center = Offset(lx, ly))
        }
    }

    private fun drawClippedQuad(
        drawScope: DrawScope,
        camera: Camera3D,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        width: Float,
        height: Float,
        brush: Brush?,
        solidColor: Color
    ) {
        camera.toCameraSpace(x0, y0, z0, inCamQuad[0])
        camera.toCameraSpace(x1, y1, z1, inCamQuad[1])
        camera.toCameraSpace(x2, y2, z2, inCamQuad[2])
        camera.toCameraSpace(x3, y3, z3, inCamQuad[3])

        val clippedCount = camera.clipAndProjectPolygon(
            inCamQuad, 4, width, height, clippedCamQuad, outSX, outSY, nearZ = 0.45f
        )
        if (clippedCount < 3) return

        reusablePath.reset()
        reusablePath.moveTo(outSX[0], outSY[0])
        for (i in 1 until clippedCount) {
            reusablePath.lineTo(outSX[i], outSY[i])
        }
        reusablePath.close()

        if (brush != null) {
            drawScope.drawPath(reusablePath, brush = brush)
        } else {
            drawScope.drawPath(reusablePath, color = solidColor)
        }
    }

    private data class QuadColors(
        val top: Color,
        val mid: Color,
        val horizon: Color,
        val ground: Color
    )
}
