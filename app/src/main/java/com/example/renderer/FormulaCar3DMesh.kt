package com.example.renderer

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.model.CarSetup
import com.example.model.DriverProfile
import com.example.model.GraphicsQuality
import com.example.model.TireCompound
import com.example.model.Vector3D
import com.example.model.VehicleState
import com.example.model.WeatherCondition
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Renders articulated 3D open-wheel Formula vehicles (chassis, sidepods, nosecone,
 * animated DRS rear wing, suspension wishbones, steering/spinning 3D wheels,
 * glowing brake discs, halo, helmet, glossy clearcoat reflections, and cast shadows)
 * as well as the upgraded 3D-perspective Cockpit view.
 */
class FormulaCar3DMesh {

    private class Face3D {
        val wx = FloatArray(4)
        val wy = FloatArray(4)
        val wz = FloatArray(4)
        var nx = 0f
        var ny = 0f
        var nz = 1f
        var baseColor = Color.Red
        var isCarbon = false
        var isGlossy = true
        var depth = 0f
        var active = false
    }

    private val maxFaces = 36
    private val faces = Array(maxFaces) { Face3D() }
    private val faceOrder = IntArray(maxFaces)
    private var faceCount = 0

    private val reusablePath = Path()
    private val tmpXYD = FloatArray(3)
    private val wheelCornerCam = Array(8) { CamPoint3D() }
    private val inCamQuad = Array(4) { CamPoint3D() }
    private val clippedCamQuad = Array(8) { CamPoint3D() }
    private val outSX = FloatArray(8)
    private val outSY = FloatArray(8)

    fun drawCar3D(
        drawScope: DrawScope,
        camera: Camera3D,
        textures: FormulaTextures,
        position: Vector3D,
        headingRad: Float,
        pitchRad: Float,
        rollRad: Float,
        bounceZ: Float,
        steerInput: Float,
        wheelSpinRad: Float,
        brakeAmount: Float,
        brakeTempC: Float,
        drsOpen: Boolean,
        frontWingDamage: Float,
        isPitLimiter: Boolean,
        speedKph: Float,
        primaryColor: Color,
        accentColor: Color,
        helmetPrimary: Color,
        helmetVisor: Color,
        tireCompound: TireCompound,
        weather: WeatherCondition,
        quality: GraphicsQuality,
        sunDirX: Float,
        sunDirY: Float,
        sunDirZ: Float,
        width: Float,
        height: Float
    ) {
        val fwdX = cos(headingRad)
        val fwdY = sin(headingRad)
        val sideX = -fwdY
        val sideY = fwdX

        // 1. Directional Ground Contact + Cast Shadow
        if (quality.enableShadows) {
            drawCarShadow(
                drawScope, camera, position, fwdX, fwdY, sideX, sideY,
                sunDirX, sunDirY, sunDirZ, weather, width, height
            )
        } else {
            // Cheap elliptical contact shadow on Low quality
            if (camera.projectFast(position.x, position.y, position.z + 0.03f, width, height, tmpXYD)) {
                val sc = (130f / tmpXYD[2]).coerceIn(0.25f, 3.6f)
                drawScope.drawOval(
                    color = Color(0x66000000),
                    topLeft = Offset(tmpXYD[0] - 42f * sc, tmpXYD[1] - 12f * sc),
                    size = Size(84f * sc, 24f * sc)
                )
            }
        }

        // 2. Build 3D Chassis Faces in Local Space -> World Space
        faceCount = 0
        val wingDroop = frontWingDamage * 0.14f

        // Floor plank & rear diffuser (carbon fiber)
        addLocalQuad(
            position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ,
            -2.05f, -0.72f, 0.12f,
            1.25f, -0.72f, 0.12f,
            1.25f, 0.72f, 0.12f,
            -2.05f, 0.72f, 0.12f,
            0f, 0f, 1f, Color(0xFF151922), isCarbon = true, isGlossy = false
        )

        // Front Wing Main Plane
        addLocalQuad(
            position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ,
            1.88f, -0.96f, 0.14f - wingDroop,
            2.38f, -0.92f, 0.11f - wingDroop,
            2.38f, 0.92f, 0.11f - wingDroop,
            1.88f, 0.96f, 0.14f - wingDroop,
            0.1f, 0f, 0.99f, accentColor, isCarbon = false, isGlossy = true
        )
        // Front Wing Endplates
        addLocalQuad(
            position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ,
            1.84f, -0.97f, 0.12f - wingDroop,
            2.36f, -0.94f, 0.10f - wingDroop,
            2.36f, -0.94f, 0.34f - wingDroop,
            1.84f, -0.97f, 0.38f - wingDroop,
            0f, -1f, 0.1f, primaryColor, isCarbon = false, isGlossy = true
        )
        addLocalQuad(
            position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ,
            1.84f, 0.97f, 0.12f - wingDroop,
            2.36f, 0.94f, 0.10f - wingDroop,
            2.36f, 0.94f, 0.34f - wingDroop,
            1.84f, 0.97f, 0.38f - wingDroop,
            0f, 1f, 0.1f, primaryColor, isCarbon = false, isGlossy = true
        )

        // Tapered Nosecone Top & Side Bevels
        addLocalQuad(
            position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ,
            0.55f, -0.26f, 0.62f,
            2.32f, -0.10f, 0.24f - wingDroop * 0.5f,
            2.32f, 0.10f, 0.24f - wingDroop * 0.5f,
            0.55f, 0.26f, 0.62f,
            0.22f, 0f, 0.97f, primaryColor, isCarbon = false, isGlossy = true
        )
        addLocalQuad(
            position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ,
            0.55f, -0.34f, 0.22f,
            2.30f, -0.13f, 0.16f,
            2.32f, -0.10f, 0.24f,
            0.55f, -0.26f, 0.62f,
            0.1f, -0.92f, 0.36f, primaryColor, isCarbon = false, isGlossy = true
        )
        addLocalQuad(
            position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ,
            0.55f, 0.34f, 0.22f,
            2.30f, 0.13f, 0.16f,
            2.32f, 0.10f, 0.24f,
            0.55f, 0.26f, 0.62f,
            0.1f, 0.92f, 0.36f, primaryColor, isCarbon = false, isGlossy = true
        )

        // Left & Right Sculpted Sidepods (top + outer flanks)
        addLocalQuad(
            position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ,
            -1.45f, -0.68f, 0.52f,
            0.42f, -0.72f, 0.54f,
            0.50f, -0.28f, 0.60f,
            -1.45f, -0.22f, 0.56f,
            0f, -0.3f, 0.95f, primaryColor, isCarbon = false, isGlossy = true
        )
        addLocalQuad(
            position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ,
            -1.45f, 0.22f, 0.56f,
            0.50f, 0.28f, 0.60f,
            0.42f, 0.72f, 0.54f,
            -1.45f, 0.68f, 0.52f,
            0f, 0.3f, 0.95f, primaryColor, isCarbon = false, isGlossy = true
        )
        addLocalQuad(
            position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ,
            -1.45f, -0.72f, 0.15f,
            0.42f, -0.74f, 0.16f,
            0.42f, -0.72f, 0.54f,
            -1.45f, -0.68f, 0.52f,
            0f, -0.98f, 0.18f, accentColor, isCarbon = false, isGlossy = true
        )
        addLocalQuad(
            position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ,
            -1.45f, 0.72f, 0.15f,
            0.42f, 0.74f, 0.16f,
            0.42f, 0.72f, 0.54f,
            -1.45f, 0.68f, 0.52f,
            0f, 0.98f, 0.18f, accentColor, isCarbon = false, isGlossy = true
        )

        // Engine Cover & Airbox Spine
        addLocalQuad(
            position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ,
            -1.82f, -0.18f, 0.68f,
            -0.35f, -0.22f, 0.98f,
            -0.35f, 0.22f, 0.98f,
            -1.82f, 0.18f, 0.68f,
            -0.25f, 0f, 0.96f, primaryColor, isCarbon = false, isGlossy = true
        )
        // Shark Fin (center aero fin)
        addLocalQuad(
            position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ,
            -1.85f, 0f, 0.68f,
            -0.40f, 0f, 0.96f,
            -0.40f, 0f, 1.12f,
            -1.85f, 0f, 0.94f,
            0f, 1f, 0f, accentColor, isCarbon = false, isGlossy = true
        )

        // Rear Wing Endplates + Lower Beam + Animated DRS Flap
        addLocalQuad(
            position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ,
            -2.18f, -0.58f, 0.58f,
            -1.76f, -0.58f, 0.58f,
            -1.76f, -0.58f, 1.08f,
            -2.18f, -0.58f, 1.08f,
            0f, -1f, 0f, primaryColor, isCarbon = false, isGlossy = true
        )
        addLocalQuad(
            position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ,
            -2.18f, 0.58f, 0.58f,
            -1.76f, 0.58f, 0.58f,
            -1.76f, 0.58f, 1.08f,
            -2.18f, 0.58f, 1.08f,
            0f, 1f, 0f, primaryColor, isCarbon = false, isGlossy = true
        )
        // Main Rear Wing Element
        addLocalQuad(
            position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ,
            -2.04f, -0.57f, 0.86f,
            -1.78f, -0.57f, 0.80f,
            -1.78f, 0.57f, 0.80f,
            -2.04f, 0.57f, 0.86f,
            0.22f, 0f, 0.97f, Color(0xFF151820), isCarbon = true, isGlossy = false
        )
        // Animated DRS Upper Flap (opens flat / rises when drsOpen == true)
        val drsRearZ = if (drsOpen) 0.95f else 1.07f
        val drsFrontZ = if (drsOpen) 0.96f else 0.91f
        val drsColor = if (drsOpen) Color(0xFF00E676) else accentColor
        addLocalQuad(
            position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ,
            -2.18f, -0.57f, drsRearZ,
            -2.00f, -0.57f, drsFrontZ,
            -2.00f, 0.57f, drsFrontZ,
            -2.18f, 0.57f, drsRearZ,
            if (drsOpen) 0f else 0.65f, 0f, if (drsOpen) 1f else 0.75f,
            drsColor, isCarbon = !drsOpen, isGlossy = true
        )

        // Sort faces back-to-front by camera Z depth
        for (i in 0 until faceCount) {
            val f = faces[i]
            var sumZ = 0f
            for (v in 0 until 4) {
                camera.toCameraSpace(f.wx[v], f.wy[v], f.wz[v], inCamQuad[v])
                sumZ += inCamQuad[v].z
            }
            f.depth = sumZ * 0.25f
            faceOrder[i] = i
        }
        // Simple insertion sort on <= 24 faces (zero allocation)
        for (i in 1 until faceCount) {
            val key = faceOrder[i]
            val keyDepth = faces[key].depth
            var j = i - 1
            while (j >= 0 && faces[faceOrder[j]].depth < keyDepth) {
                faceOrder[j + 1] = faceOrder[j]
                j--
            }
            faceOrder[j + 1] = key
        }

        // 3. Draw Rear Wheels (if behind chassis in camera space) & Suspension Wishbones
        val steerAngle = steerInput * 0.42f
        val brakeGlow = ((brakeTempC - 480f) / 380f).coerceIn(0f, 1f).coerceAtLeast(brakeAmount * 0.85f)

        // Draw wheels sorted roughly with body: rear wheels first when looking from front, front wheels first when chasing
        camera.toCameraSpace(position.x + fwdX * 1.55f, position.y + fwdY * 1.55f, position.z, inCamQuad[0])
        val frontCamZ = inCamQuad[0].z
        camera.toCameraSpace(position.x - fwdX * 1.55f, position.y - fwdY * 1.55f, position.z, inCamQuad[1])
        val rearCamZ = inCamQuad[1].z

        if (quality.enableSuspensionStruts) {
            drawSuspensionWishbones(
                drawScope, camera, position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ, width, height
            )
        }

        if (frontCamZ >= rearCamZ) {
            // Front wheels are farther (chasing from behind) -> draw front wheels, then body, then rear wheels
            drawWheel3D(drawScope, camera, position, fwdX, fwdY, sideX, sideY, 1.55f, -0.88f, 0.35f, 0.32f, steerAngle, wheelSpinRad, tireCompound, brakeGlow, quality, width, height)
            drawWheel3D(drawScope, camera, position, fwdX, fwdY, sideX, sideY, 1.55f, 0.88f, 0.35f, 0.32f, steerAngle, wheelSpinRad, tireCompound, brakeGlow, quality, width, height)
            drawSortedBodyFaces(drawScope, camera, textures, fwdX, fwdY, sideX, sideY, sunDirX, sunDirY, sunDirZ, weather, quality, width, height)
            drawWheel3D(drawScope, camera, position, fwdX, fwdY, sideX, sideY, -1.55f, -0.92f, 0.37f, 0.40f, 0f, wheelSpinRad, tireCompound, brakeGlow, quality, width, height)
            drawWheel3D(drawScope, camera, position, fwdX, fwdY, sideX, sideY, -1.55f, 0.92f, 0.37f, 0.40f, 0f, wheelSpinRad, tireCompound, brakeGlow, quality, width, height)
        } else {
            // Rear wheels are farther (looking from front/TV broadcast)
            drawWheel3D(drawScope, camera, position, fwdX, fwdY, sideX, sideY, -1.55f, -0.92f, 0.37f, 0.40f, 0f, wheelSpinRad, tireCompound, brakeGlow, quality, width, height)
            drawWheel3D(drawScope, camera, position, fwdX, fwdY, sideX, sideY, -1.55f, 0.92f, 0.37f, 0.40f, 0f, wheelSpinRad, tireCompound, brakeGlow, quality, width, height)
            drawSortedBodyFaces(drawScope, camera, textures, fwdX, fwdY, sideX, sideY, sunDirX, sunDirY, sunDirZ, weather, quality, width, height)
            drawWheel3D(drawScope, camera, position, fwdX, fwdY, sideX, sideY, 1.55f, -0.88f, 0.35f, 0.32f, steerAngle, wheelSpinRad, tireCompound, brakeGlow, quality, width, height)
            drawWheel3D(drawScope, camera, position, fwdX, fwdY, sideX, sideY, 1.55f, 0.88f, 0.35f, 0.32f, steerAngle, wheelSpinRad, tireCompound, brakeGlow, quality, width, height)
        }

        // 4. Driver Helmet & 3D Titanium Halo Arc
        drawHelmetAndHalo(
            drawScope, camera, position, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ,
            helmetPrimary, helmetVisor, width, height
        )

        // 5. Rear Crash-Structure Rain / Braking / Pit LED
        val ledOn = brakeAmount > 0.15f || isPitLimiter || speedKph < 45f || weather.isRain
        if (ledOn) {
            val blink = if (brakeAmount > 0.15f) true else ((System.currentTimeMillis() / 180L) % 2L == 0L)
            if (blink) {
                val ledWx = position.x - fwdX * 2.18f
                val ledWy = position.y - fwdY * 2.18f
                val ledWz = position.z + 0.42f + bounceZ
                if (camera.projectFast(ledWx, ledWy, ledWz, width, height, tmpXYD)) {
                    val r = (14f / tmpXYD[2]).coerceIn(2f, 8f)
                    drawScope.drawCircle(Color(0x88FF1744), radius = r * 2.2f, center = Offset(tmpXYD[0], tmpXYD[1]))
                    drawScope.drawCircle(Color(0xFFFF1744), radius = r, center = Offset(tmpXYD[0], tmpXYD[1]))
                }
            }
        }
    }

    private fun drawSortedBodyFaces(
        drawScope: DrawScope,
        camera: Camera3D,
        textures: FormulaTextures,
        fwdX: Float,
        fwdY: Float,
        sideX: Float,
        sideY: Float,
        sunDirX: Float,
        sunDirY: Float,
        sunDirZ: Float,
        weather: WeatherCondition,
        quality: GraphicsQuality,
        width: Float,
        height: Float
    ) {
        val ambient = when (weather) {
            WeatherCondition.NIGHT_GP -> 0.42f
            WeatherCondition.RAIN_WET, WeatherCondition.HEAVY_RAIN, WeatherCondition.WET_TRACK -> 0.56f
            WeatherCondition.SUNSET -> 0.50f
            WeatherCondition.DRY_SUNNY, WeatherCondition.CLOUDY, WeatherCondition.FOGGY -> 0.55f
            else -> 0.52f
        }

        for (k in 0 until faceCount) {
            val f = faces[faceOrder[k]]
            if (f.depth < -1.0f) continue

            for (v in 0 until 4) {
                camera.toCameraSpace(f.wx[v], f.wy[v], f.wz[v], inCamQuad[v])
            }
            val clippedCount = camera.clipAndProjectPolygon(
                inCamQuad, 4, width, height, clippedCamQuad, outSX, outSY, nearZ = 0.38f
            )
            if (clippedCount < 3) continue

            reusablePath.reset()
            reusablePath.moveTo(outSX[0], outSY[0])
            for (v in 1 until clippedCount) {
                reusablePath.lineTo(outSX[v], outSY[v])
            }
            reusablePath.close()

            if (f.isCarbon) {
                drawScope.drawPath(reusablePath, brush = textures.carbonFiberBrush)
                continue
            }

            // World-space normal for directional lighting + glossy reflection
            val wnx = fwdX * f.nx + sideX * f.ny
            val wny = fwdY * f.nx + sideY * f.ny
            val wnz = f.nz
            val ndotl = abs(wnx * sunDirX + wny * sunDirY + wnz * sunDirZ).coerceIn(0f, 1f)
            val shade = (ambient + ndotl * 0.55f).coerceIn(0.35f, 1.15f)

            val litColor = Color(
                red = (f.baseColor.red * shade).coerceIn(0f, 1f),
                green = (f.baseColor.green * shade).coerceIn(0f, 1f),
                blue = (f.baseColor.blue * shade).coerceIn(0f, 1f),
                alpha = 1f
            )

            if (f.isGlossy && quality.enableReflections) {
                // Glossy automotive clear-coat specular band across polygon
                var minY = outSY[0]
                var maxY = outSY[0]
                for (v in 1 until clippedCount) {
                    if (outSY[v] < minY) minY = outSY[v]
                    if (outSY[v] > maxY) maxY = outSY[v]
                }
                val specBoost = (ndotl.pow(3f) * 0.42f + 0.12f).coerceIn(0f, 0.55f)
                val highlightColor = Color(
                    red = (litColor.red + specBoost).coerceIn(0f, 1f),
                    green = (litColor.green + specBoost).coerceIn(0f, 1f),
                    blue = (litColor.blue + specBoost * 1.05f).coerceIn(0f, 1f),
                    alpha = 1f
                )
                drawScope.drawPath(
                    path = reusablePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(highlightColor, litColor),
                        startY = minY,
                        endY = maxY.coerceAtLeast(minY + 1f)
                    )
                )
            } else {
                drawScope.drawPath(reusablePath, color = litColor)
            }
        }
    }

    private fun drawWheel3D(
        drawScope: DrawScope,
        camera: Camera3D,
        carPos: Vector3D,
        fwdX: Float,
        fwdY: Float,
        sideX: Float,
        sideY: Float,
        lx: Float,
        ly: Float,
        radius: Float,
        tireWidth: Float,
        steerRad: Float,
        spinRad: Float,
        compound: TireCompound,
        brakeGlow: Float,
        quality: GraphicsQuality,
        width: Float,
        height: Float
    ) {
        val cx = carPos.x + fwdX * lx + sideX * ly
        val cy = carPos.y + fwdY * lx + sideY * ly
        val cz = carPos.z + radius

        val cosS = cos(steerRad)
        val sinS = sin(steerRad)
        val wFwdX = fwdX * cosS + sideX * sinS
        val wFwdY = fwdY * cosS + sideY * sinS
        val wSideX = -wFwdY
        val wSideY = wFwdX

        val hw = tireWidth * 0.5f
        val hr = radius * 0.92f

        // 8 vertices of 3D tire bounding prism (inner 0..3, outer 4..7)
        val innerSide = if (ly > 0f) -hw else hw
        val outerSide = -innerSide

        var idx = 0
        for (s in 0..1) {
            val sOff = if (s == 0) innerSide else outerSide
            for (c in 0..3) {
                val dx = if (c == 0 || c == 3) -hr else hr
                val dz = if (c < 2) -hr else hr
                val wx = cx + wFwdX * dx + wSideX * sOff
                val wy = cy + wFwdY * dx + wSideY * sOff
                val wz = cz + dz
                camera.toCameraSpace(wx, wy, wz, wheelCornerCam[idx])
                idx++
            }
        }

        // Quick center check
        if (!camera.projectFast(cx, cy, cz, width, height, tmpXYD)) return
        val depth = tmpXYD[2]
        if (depth > 180f) return

        // Draw outer tire body hull by projecting center & computing apparent width/height
        val scale = (90f / depth).coerceIn(0.25f, 4.2f)
        val px = tmpXYD[0]
        val py = tmpXYD[1]

        // Project tire width & radius vectors for true 3D orientation proportions
        val innerX = cx + wSideX * innerSide
        val innerY = cy + wSideY * innerSide
        val outerX = cx + wSideX * outerSide
        val outerY = cy + wSideY * outerSide
        val pInner = camera.project(Vector3D(innerX, innerY, cz), width, height)
        val pOuter = camera.project(Vector3D(outerX, outerY, cz), width, height)
        val pTop = camera.project(Vector3D(cx, cy, cz + radius), width, height)
        val pBot = camera.project(Vector3D(cx, cy, cz - radius * 0.95f), width, height)
        if (pInner == null || pOuter == null || pTop == null || pBot == null) return

        val screenH = abs(pBot.y - pTop.y).coerceAtLeast(6f)
        val treadW = abs(pOuter.x - pInner.x).coerceAtLeast(screenH * 0.42f)
        val leftX = minOf(pInner.x, pOuter.x, px - treadW * 0.45f)
        val rightX = maxOf(pInner.x, pOuter.x, px + treadW * 0.45f)
        val boxW = (rightX - leftX).coerceAtLeast(6f)
        val topY = minOf(pTop.y, pBot.y)

        // Tire rubber carcass with shaded tread
        drawScope.drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF101216), Color(0xFF282D36), Color(0xFF101216)),
                startX = leftX,
                endX = leftX + boxW
            ),
            topLeft = Offset(leftX, topY),
            size = Size(boxW, screenH),
            cornerRadius = CornerRadius(4f * scale, 4f * scale)
        )

        // Glowing carbon-ceramic brake disc visible from inner side during braking
        if (brakeGlow > 0.12f) {
            val discColor = Color(
                red = 1f,
                green = (0.28f + brakeGlow * 0.25f).coerceIn(0f, 1f),
                blue = 0.05f,
                alpha = (brakeGlow * 0.9f).coerceIn(0f, 0.92f)
            )
            drawScope.drawCircle(
                color = discColor,
                radius = screenH * 0.28f,
                center = Offset(pInner.x, py)
            )
        }

        // Compound color ring & rotating rim spokes
        val compoundColor = Color(compound.colorHex)
        drawScope.drawCircle(
            color = compoundColor,
            radius = screenH * 0.36f,
            center = Offset(pOuter.x, py),
            style = Stroke(width = (2.6f * scale).coerceIn(1.2f, 5f))
        )
        drawScope.drawCircle(
            color = Color(0xFF181C24),
            radius = screenH * 0.24f,
            center = Offset(pOuter.x, py)
        )
        if (quality != GraphicsQuality.LOW) {
            val spokeR = screenH * 0.22f
            for (s in 0 until 4) {
                val ang = spinRad + s * 1.5708f
                drawScope.drawLine(
                    color = Color(0xFF8A95A8),
                    start = Offset(pOuter.x, py),
                    end = Offset(pOuter.x + cos(ang) * spokeR, py + sin(ang) * spokeR),
                    strokeWidth = (1.6f * scale).coerceIn(1f, 3f)
                )
            }
        }
    }

    private fun drawSuspensionWishbones(
        drawScope: DrawScope,
        camera: Camera3D,
        carPos: Vector3D,
        fwdX: Float,
        fwdY: Float,
        sideX: Float,
        sideY: Float,
        pitchRad: Float,
        rollRad: Float,
        bounceZ: Float,
        width: Float,
        height: Float
    ) {
        val pairs = arrayOf(
            // front-left, front-right, rear-left, rear-right
            floatArrayOf(1.55f, -0.24f, 0.42f, 1.55f, -0.84f, 0.35f),
            floatArrayOf(1.55f, 0.24f, 0.42f, 1.55f, 0.84f, 0.35f),
            floatArrayOf(-1.55f, -0.22f, 0.44f, -1.55f, -0.88f, 0.37f),
            floatArrayOf(-1.55f, 0.22f, 0.44f, -1.55f, 0.88f, 0.37f)
        )
        for (p in pairs) {
            val inPos = transformLocal(carPos, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ, p[0], p[1], p[2])
            val hubPos = Vector3D(
                carPos.x + fwdX * p[3] + sideX * p[4],
                carPos.y + fwdY * p[3] + sideY * p[4],
                carPos.z + p[5]
            )
            val p1 = camera.project(inPos, width, height) ?: continue
            val p2 = camera.project(hubPos, width, height) ?: continue
            drawScope.drawLine(Color(0xFF2B313D), Offset(p1.x, p1.y), Offset(p2.x, p2.y), strokeWidth = 3f)
        }
    }

    private fun drawHelmetAndHalo(
        drawScope: DrawScope,
        camera: Camera3D,
        carPos: Vector3D,
        fwdX: Float,
        fwdY: Float,
        sideX: Float,
        sideY: Float,
        pitchRad: Float,
        rollRad: Float,
        bounceZ: Float,
        helmetPrimary: Color,
        helmetVisor: Color,
        width: Float,
        height: Float
    ) {
        val helmetPos = transformLocal(carPos, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ, -0.18f, 0f, 0.78f)
        if (!camera.projectFast(helmetPos.x, helmetPos.y, helmetPos.z, width, height, tmpXYD)) return
        val sc = (110f / tmpXYD[2]).coerceIn(0.2f, 3.8f)
        val hx = tmpXYD[0]
        val hy = tmpXYD[1]

        // Helmet shell + glossy visor strip
        drawScope.drawCircle(helmetPrimary, radius = 8.5f * sc, center = Offset(hx, hy))
        drawScope.drawRoundRect(
            color = helmetVisor,
            topLeft = Offset(hx - 6.5f * sc, hy - 3f * sc),
            size = Size(13f * sc, 4.2f * sc),
            cornerRadius = CornerRadius(2f * sc, 2f * sc)
        )

        // Titanium Halo front pillar + side hoop
        val haloFront = transformLocal(carPos, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ, 0.42f, 0f, 0.66f)
        val haloTop = transformLocal(carPos, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ, 0.05f, 0f, 0.94f)
        val haloLeft = transformLocal(carPos, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ, -0.25f, -0.28f, 0.78f)
        val haloRight = transformLocal(carPos, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ, -0.25f, 0.28f, 0.78f)

        val pHF = camera.project(haloFront, width, height) ?: return
        val pHT = camera.project(haloTop, width, height) ?: return
        val pHL = camera.project(haloLeft, width, height) ?: return
        val pHR = camera.project(haloRight, width, height) ?: return

        val strokeW = (4.2f * sc).coerceIn(1.5f, 8f)
        val haloColor = Color(0xFF1F242D)
        drawScope.drawLine(haloColor, Offset(pHF.x, pHF.y), Offset(pHT.x, pHT.y), strokeWidth = strokeW)
        drawScope.drawLine(haloColor, Offset(pHT.x, pHT.y), Offset(pHL.x, pHL.y), strokeWidth = strokeW)
        drawScope.drawLine(haloColor, Offset(pHT.x, pHT.y), Offset(pHR.x, pHR.y), strokeWidth = strokeW)
    }

    private fun drawCarShadow(
        drawScope: DrawScope,
        camera: Camera3D,
        carPos: Vector3D,
        fwdX: Float,
        fwdY: Float,
        sideX: Float,
        sideY: Float,
        sunDirX: Float,
        sunDirY: Float,
        sunDirZ: Float,
        weather: WeatherCondition,
        width: Float,
        height: Float
    ) {
        val shadowShiftX = -sunDirX / sunDirZ.coerceAtLeast(0.35f) * 0.45f
        val shadowShiftY = -sunDirY / sunDirZ.coerceAtLeast(0.35f) * 0.45f
        val corners = arrayOf(
            Pair(-2.1f, -0.95f),
            Pair(2.3f, -0.95f),
            Pair(2.3f, 0.95f),
            Pair(-2.1f, 0.95f)
        )
        for (i in 0..3) {
            val (lx, ly) = corners[i]
            val wx = carPos.x + fwdX * lx + sideX * ly + shadowShiftX
            val wy = carPos.y + fwdY * lx + sideY * ly + shadowShiftY
            camera.toCameraSpace(wx, wy, carPos.z + 0.03f, inCamQuad[i])
        }
        val count = camera.clipAndProjectPolygon(inCamQuad, 4, width, height, clippedCamQuad, outSX, outSY)
        if (count < 3) return
        reusablePath.reset()
        reusablePath.moveTo(outSX[0], outSY[0])
        for (i in 1 until count) reusablePath.lineTo(outSX[i], outSY[i])
        reusablePath.close()

        val alpha = when (weather) {
            WeatherCondition.NIGHT_GP -> 0.45f
            WeatherCondition.RAIN_WET, WeatherCondition.HEAVY_RAIN, WeatherCondition.WET_TRACK -> 0.35f
            WeatherCondition.SUNSET -> 0.52f
            WeatherCondition.DRY_SUNNY, WeatherCondition.CLOUDY, WeatherCondition.FOGGY -> 0.58f
            else -> 0.50f
        }
        drawScope.drawPath(reusablePath, color = Color(0f, 0f, 0f, alpha))
    }

    fun drawCockpitView(
        drawScope: DrawScope,
        textures: FormulaTextures,
        state: VehicleState,
        setup: CarSetup,
        profile: DriverProfile,
        quality: GraphicsQuality,
        width: Float,
        height: Float
    ) {
        val cx = width * 0.5f
        val primary = Color(profile.liveryTheme.primaryHex)
        val accent = Color(profile.liveryTheme.accentHex)
        val compoundColor = Color(setup.tireCompound.colorHex)
        val vibY = state.suspensionBounceOffset * 180f

        // 1. Front Nosecone & Wing Tips extending forward
        val nosePath = Path().apply {
            moveTo(cx - width * 0.08f, height * 0.63f + vibY)
            lineTo(cx + width * 0.08f, height * 0.63f + vibY)
            lineTo(cx + width * 0.16f, height)
            lineTo(cx - width * 0.16f, height)
            close()
        }
        drawScope.drawPath(
            nosePath,
            brush = Brush.verticalGradient(
                colors = listOf(primary, Color(profile.liveryTheme.carbonHex)),
                startY = height * 0.63f,
                endY = height
            )
        )
        // Glossy specular nose ridge
        if (quality.enableReflections) {
            drawScope.drawLine(
                Color.White.copy(alpha = 0.25f),
                Offset(cx - 8f, height * 0.64f + vibY),
                Offset(cx - 26f, height),
                strokeWidth = 6f
            )
        }
        // Accent pinstripes on nosecone
        drawScope.drawLine(accent, Offset(cx - width * 0.055f, height * 0.66f + vibY), Offset(cx - width * 0.10f, height), strokeWidth = 4f)
        drawScope.drawLine(accent, Offset(cx + width * 0.055f, height * 0.66f + vibY), Offset(cx + width * 0.10f, height), strokeWidth = 4f)

        // 2. Visible Front Suspension Wishbones & Steering Front Tires
        val steerShift = state.steerInput * 24f
        val leftTireX = width * 0.11f + steerShift
        val rightTireX = width * 0.78f + steerShift
        val tireW = width * 0.105f
        val tireH = height * 0.25f
        val tireY = height * 0.54f + vibY * 0.4f

        // Carbon suspension arms
        drawScope.drawLine(Color(0xFF202530), Offset(cx - width * 0.09f, height * 0.72f + vibY), Offset(leftTireX + tireW * 0.85f, tireY + tireH * 0.45f), strokeWidth = 9f)
        drawScope.drawLine(Color(0xFF202530), Offset(cx + width * 0.09f, height * 0.72f + vibY), Offset(rightTireX + tireW * 0.15f, tireY + tireH * 0.45f), strokeWidth = 9f)

        // Brake duct glow inside front wheels
        val brakeGlow = ((state.brakeDiscTemp - 480f) / 380f).coerceIn(0f, 1f).coerceAtLeast(state.brakeInput * 0.85f)
        if (brakeGlow > 0.12f) {
            val glowColor = Color(1f, 0.35f, 0.05f, brakeGlow * 0.75f)
            drawScope.drawCircle(glowColor, radius = tireW * 0.35f, center = Offset(leftTireX + tireW * 0.9f, tireY + tireH * 0.5f))
            drawScope.drawCircle(glowColor, radius = tireW * 0.35f, center = Offset(rightTireX + tireW * 0.1f, tireY + tireH * 0.5f))
        }

        // Left & Right Front Tires with Compound Stripes
        drawScope.drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color(0xFF0C0E12), Color(0xFF252932), Color(0xFF0C0E12)), leftTireX, leftTireX + tireW),
            topLeft = Offset(leftTireX, tireY),
            size = Size(tireW, tireH),
            cornerRadius = CornerRadius(12f, 12f)
        )
        drawScope.drawRoundRect(compoundColor, Offset(leftTireX + 8f, tireY + 12f), Size(4f, tireH - 24f), CornerRadius(2f, 2f))

        drawScope.drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color(0xFF0C0E12), Color(0xFF252932), Color(0xFF0C0E12)), rightTireX, rightTireX + tireW),
            topLeft = Offset(rightTireX, tireY),
            size = Size(tireW, tireH),
            cornerRadius = CornerRadius(12f, 12f)
        )
        drawScope.drawRoundRect(compoundColor, Offset(rightTireX + tireW - 12f, tireY + 12f), Size(4f, tireH - 24f), CornerRadius(2f, 2f))

        // 3. Textured Carbon Titanium Halo Pillar & Side Arches
        val pillarPath = Path().apply {
            moveTo(cx - 10f, 0f)
            lineTo(cx + 10f, 0f)
            lineTo(cx + 18f, height * 0.66f + vibY)
            lineTo(cx - 18f, height * 0.66f + vibY)
            close()
        }
        drawScope.drawPath(pillarPath, brush = textures.carbonFiberBrush)
        // Left & Right upper halo framing
        drawScope.drawLine(Color(0xCC171B24), Offset(0f, height * 0.16f), Offset(cx, height * 0.04f), strokeWidth = 18f)
        drawScope.drawLine(Color(0xCC171B24), Offset(width, height * 0.16f), Offset(cx, height * 0.04f), strokeWidth = 18f)

        // 4. Formula Carbon Steering Wheel & Live OLED Shift Display
        val wheelRadius = width * 0.24f
        val wheelCenter = Offset(cx + state.steerInput * 22f, height * 0.94f + vibY * 0.5f)
        val wheelAngleDeg = state.steerInput * 90f

        drawScope.rotate(wheelAngleDeg, pivot = wheelCenter) {
            val wheelW = wheelRadius * 2.1f
            val wheelH = wheelRadius * 1.25f

            drawRoundRect(
                brush = textures.carbonFiberBrush,
                topLeft = Offset(wheelCenter.x - wheelW * 0.5f, wheelCenter.y - wheelH * 0.65f),
                size = Size(wheelW, wheelH),
                cornerRadius = CornerRadius(24f, 24f)
            )
            // Alcantara hand grips
            drawRoundRect(
                color = Color(0xFF0A0C10),
                topLeft = Offset(wheelCenter.x - wheelW * 0.52f, wheelCenter.y - wheelH * 0.5f),
                size = Size(wheelW * 0.17f, wheelH * 0.85f),
                cornerRadius = CornerRadius(16f, 16f)
            )
            drawRoundRect(
                color = Color(0xFF0A0C10),
                topLeft = Offset(wheelCenter.x + wheelW * 0.35f, wheelCenter.y - wheelH * 0.5f),
                size = Size(wheelW * 0.17f, wheelH * 0.85f),
                cornerRadius = CornerRadius(16f, 16f)
            )

            // Rotary Dials
            drawCircle(Color(0xFFFF1801), radius = wheelW * 0.032f, center = Offset(wheelCenter.x - wheelW * 0.25f, wheelCenter.y - wheelH * 0.06f))
            drawCircle(Color(0xFF00E5FF), radius = wheelW * 0.032f, center = Offset(wheelCenter.x + wheelW * 0.25f, wheelCenter.y - wheelH * 0.06f))

            // OLED Telemetry Display
            val oledW = wheelW * 0.54f
            val oledH = wheelH * 0.50f
            val oledX = wheelCenter.x - oledW * 0.5f
            val oledY = wheelCenter.y - wheelH * 0.56f

            drawRoundRect(Color(0xFF04060A), Offset(oledX, oledY), Size(oledW, oledH), CornerRadius(10f, 10f))
            drawRoundRect(Color(0xFF2D3548), Offset(oledX, oledY), Size(oledW, oledH), CornerRadius(10f, 10f), style = Stroke(2.5f))

            val totalLeds = 15
            val ledSpacing = oledW / (totalLeds + 1)
            val rpmRatio = ((state.rpm - 6000f) / 9000f).coerceIn(0f, 1f)
            val litLeds = (rpmRatio * totalLeds).toInt()
            for (led in 0 until totalLeds) {
                val ledColor = when {
                    led < 5 -> Color(0xFF00E676)
                    led < 10 -> Color(0xFFFF1801)
                    else -> Color(0xFFD500F9)
                }
                val isLit = led < litLeds
                val lcx = oledX + (led + 1) * ledSpacing
                val lcy = oledY + 12f
                drawCircle(if (isLit) ledColor else Color(0xFF181E2B), radius = 4.2f, center = Offset(lcx, lcy))
                if (isLit && led >= 10) {
                    drawCircle(Color(0x77D500F9), radius = 8f, center = Offset(lcx, lcy))
                }
            }

            // ERS & DRS Status Bars on Wheel OLED
            drawRoundRect(
                color = Color(0xFF141B29),
                topLeft = Offset(oledX + 12f, oledY + oledH * 0.55f),
                size = Size(oledW - 24f, 8f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawRoundRect(
                color = if (state.ersDeploying) Color(0xFFFFB800) else Color(0xFF00E5FF),
                topLeft = Offset(oledX + 12f, oledY + oledH * 0.55f),
                size = Size((oledW - 24f) * state.ersEnergy.coerceIn(0f, 1f), 8f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }

    private fun addLocalQuad(
        carPos: Vector3D,
        fwdX: Float, fwdY: Float,
        sideX: Float, sideY: Float,
        pitchRad: Float, rollRad: Float, bounceZ: Float,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        nx: Float, ny: Float, nz: Float,
        color: Color,
        isCarbon: Boolean,
        isGlossy: Boolean
    ) {
        if (faceCount >= maxFaces) return
        val f = faces[faceCount++]
        setLocalVertex(f, 0, carPos, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ, x0, y0, z0)
        setLocalVertex(f, 1, carPos, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ, x1, y1, z1)
        setLocalVertex(f, 2, carPos, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ, x2, y2, z2)
        setLocalVertex(f, 3, carPos, fwdX, fwdY, sideX, sideY, pitchRad, rollRad, bounceZ, x3, y3, z3)
        f.nx = nx; f.ny = ny; f.nz = nz
        f.baseColor = color
        f.isCarbon = isCarbon
        f.isGlossy = isGlossy
        f.active = true
    }

    private fun setLocalVertex(
        face: Face3D, idx: Int,
        carPos: Vector3D,
        fwdX: Float, fwdY: Float,
        sideX: Float, sideY: Float,
        pitchRad: Float, rollRad: Float, bounceZ: Float,
        lx: Float, ly: Float, lz: Float
    ) {
        val cosR = cos(rollRad)
        val sinR = sin(rollRad)
        val ry = ly * cosR - lz * sinR
        val rz = ly * sinR + lz * cosR

        val cosP = cos(pitchRad)
        val sinP = sin(pitchRad)
        val px = lx * cosP + rz * sinP
        val pz = -lx * sinP + rz * cosP + bounceZ

        face.wx[idx] = carPos.x + fwdX * px + sideX * ry
        face.wy[idx] = carPos.y + fwdY * px + sideY * ry
        face.wz[idx] = carPos.z + pz
    }

    private fun transformLocal(
        carPos: Vector3D,
        fwdX: Float, fwdY: Float,
        sideX: Float, sideY: Float,
        pitchRad: Float, rollRad: Float, bounceZ: Float,
        lx: Float, ly: Float, lz: Float
    ): Vector3D {
        val cosR = cos(rollRad)
        val sinR = sin(rollRad)
        val ry = ly * cosR - lz * sinR
        val rz = ly * sinR + lz * cosR
        val cosP = cos(pitchRad)
        val sinP = sin(pitchRad)
        val px = lx * cosP + rz * sinP
        val pz = -lx * sinP + rz * cosP + bounceZ
        return Vector3D(
            carPos.x + fwdX * px + sideX * ry,
            carPos.y + fwdY * px + sideY * ry,
            carPos.z + pz
        )
    }
}
