package com.example.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.model.GraphicsQuality
import com.example.model.Vector3D
import com.example.model.VehicleState
import com.example.model.WeatherCondition
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Zero-allocation object-pooled particle engine for tire smoke, wet rooster-tail spray,
 * titanium skid sparks, off-track dust, exhaust flames, and rain streaks.
 */
class FormulaParticleSystem {

    enum class ParticleKind {
        TIRE_SMOKE,
        GRAVEL_DUST,
        WET_SPRAY,
        SKID_SPARK,
        EXHAUST_FLAME
    }

    private class Particle {
        var active = false
        var kind = ParticleKind.TIRE_SMOKE
        var x = 0f
        var y = 0f
        var z = 0f
        var vx = 0f
        var vy = 0f
        var vz = 0f
        var size = 1f
        var life = 0f
        var maxLife = 1f
    }

    private val maxPoolSize = 110
    private val pool = Array(maxPoolSize) { Particle() }
    private val rainDrops = Array(48) { Offset(Random.nextFloat(), Random.nextFloat()) }
    private val projTmp = FloatArray(3)
    private var spawnCursor = 0

    fun updateAndRender(
        drawScope: DrawScope,
        camera: Camera3D,
        state: VehicleState,
        weather: WeatherCondition,
        quality: GraphicsQuality,
        dt: Float,
        width: Float,
        height: Float
    ) {
        val activeBudget = quality.maxParticles.coerceAtMost(maxPoolSize)
        emitVehicleParticles(state, weather, activeBudget)

        val clampedDt = dt.coerceIn(0.005f, 0.05f)
        for (i in 0 until activeBudget) {
            val p = pool[i]
            if (!p.active) continue

            p.life += clampedDt
            if (p.life >= p.maxLife) {
                p.active = false
                continue
            }

            p.x += p.vx * clampedDt
            p.y += p.vy * clampedDt
            p.z += p.vz * clampedDt

            if (p.kind == ParticleKind.SKID_SPARK) {
                p.vz -= 9.5f * clampedDt
            } else if (p.kind == ParticleKind.TIRE_SMOKE || p.kind == ParticleKind.GRAVEL_DUST) {
                p.size += 1.6f * clampedDt
                p.vz += 0.6f * clampedDt
            } else if (p.kind == ParticleKind.WET_SPRAY) {
                p.vz -= 2.0f * clampedDt
                p.size += 1.2f * clampedDt
            }

            if (!camera.projectFast(p.x, p.y, p.z, width, height, projTmp)) continue
            val depth = projTmp[2]
            if (depth > 160f) continue

            val t = (p.life / p.maxLife).coerceIn(0f, 1f)
            val alpha = (1f - t)
            val scale = (95f / depth).coerceIn(0.25f, 3.2f)
            val screenCenter = Offset(projTmp[0], projTmp[1])

            when (p.kind) {
                ParticleKind.TIRE_SMOKE -> {
                    val radius = (10f * p.size * scale).coerceIn(2f, 48f)
                    drawScope.drawCircle(
                        color = Color(0.86f, 0.88f, 0.92f, alpha * 0.36f),
                        radius = radius,
                        center = screenCenter
                    )
                }
                ParticleKind.GRAVEL_DUST -> {
                    val radius = (9f * p.size * scale).coerceIn(2f, 44f)
                    drawScope.drawCircle(
                        color = Color(0.62f, 0.50f, 0.36f, alpha * 0.45f),
                        radius = radius,
                        center = screenCenter
                    )
                }
                ParticleKind.WET_SPRAY -> {
                    val radius = (11f * p.size * scale).coerceIn(2f, 52f)
                    drawScope.drawCircle(
                        color = Color(0.75f, 0.86f, 0.96f, alpha * 0.28f),
                        radius = radius,
                        center = screenCenter
                    )
                }
                ParticleKind.SKID_SPARK -> {
                    val radius = (2.6f * scale).coerceIn(1.2f, 7f)
                    drawScope.drawCircle(
                        color = Color(1f, 0.85f, 0.28f, alpha * 0.95f),
                        radius = radius,
                        center = screenCenter
                    )
                    if (quality == GraphicsQuality.HIGH) {
                        drawScope.drawCircle(
                            color = Color(1f, 0.52f, 0.05f, alpha * 0.45f),
                            radius = radius * 2.1f,
                            center = screenCenter
                        )
                    }
                }
                ParticleKind.EXHAUST_FLAME -> {
                    val radius = (6.5f * p.size * scale).coerceIn(2f, 22f)
                    val flameColor = if (t < 0.45f) {
                        Color(0.35f, 0.82f, 1f, alpha * 0.9f)
                    } else {
                        Color(1f, 0.52f, 0.1f, alpha * 0.85f)
                    }
                    drawScope.drawCircle(color = flameColor, radius = radius, center = screenCenter)
                }
            }
        }

        // Rain visor droplets + high-speed wind streaks (cheap 2D overlay)
        if (weather.isRain) {
            val dropCount = when (quality) {
                GraphicsQuality.LOW -> 18
                GraphicsQuality.MEDIUM -> 34
                GraphicsQuality.HIGH -> rainDrops.size
            }
            val rainTick = (System.currentTimeMillis() % 1000L) * 0.95f
            for (i in 0 until dropCount) {
                val d = rainDrops[i]
                val rx = d.x * width
                val ry = (d.y * height + rainTick) % height
                drawScope.drawLine(
                    color = Color(0x55B8D0E0),
                    start = Offset(rx, ry),
                    end = Offset(rx - 5f, ry + 20f),
                    strokeWidth = 1.8f
                )
            }
        }

        // Acceleration & ERS peripheral speed lines (Medium/High only)
        if (quality != GraphicsQuality.LOW && (state.speedKph > 270f || (state.ersDeploying && state.speedKph > 140f))) {
            val intensity = ((state.speedKph - 250f) / 100f).coerceIn(0.15f, 0.6f)
            val lineColor = if (state.ersDeploying) Color(0xFF00E5FF).copy(alpha = intensity * 0.45f)
            else Color.White.copy(alpha = intensity * 0.30f)
            val cx = width * 0.5f
            val cy = height * 0.45f
            val count = if (quality == GraphicsQuality.HIGH) 12 else 6
            val phase = (System.currentTimeMillis() % 400L) / 400f
            for (i in 0 until count) {
                val angle = (i.toFloat() / count) * 6.283f + phase * 0.35f
                val rInner = width * (0.42f + (i % 3) * 0.04f)
                val rOuter = width * 0.62f
                drawScope.drawLine(
                    color = lineColor,
                    start = Offset(cx + cos(angle) * rInner, cy + sin(angle) * rInner * 0.6f),
                    end = Offset(cx + cos(angle) * rOuter, cy + sin(angle) * rOuter * 0.6f),
                    strokeWidth = 2.0f
                )
            }
        }
    }

    private fun emitVehicleParticles(
        state: VehicleState,
        weather: WeatherCondition,
        budget: Int
    ) {
        if (budget <= 0) return
        val fwdX = cos(state.headingRad)
        val fwdY = sin(state.headingRad)
        val rightX = -fwdY
        val rightY = fwdX

        val rearCenterX = state.position.x - fwdX * 1.65f
        val rearCenterY = state.position.y - fwdY * 1.65f
        val wheelHalfTrack = 0.88f

        // 1. Tire Smoke on corner slip, braking lockup, or aggressive launch
        val isSliding = abs(state.lateralG) > 2.3f && state.speedMs > 18f
        val isLockingUp = state.brakeInput > 0.78f && state.speedMs > 15f
        val isWheelspin = state.throttleInput > 0.85f && state.speedMs in 2f..22f

        if ((isSliding || isLockingUp || isWheelspin) && !state.isOffTrack) {
            val side = if (Random.nextBoolean()) 1f else -1f
            val wx = rearCenterX + rightX * wheelHalfTrack * side
            val wy = rearCenterY + rightY * wheelHalfTrack * side
            spawn(
                budget = budget,
                kind = ParticleKind.TIRE_SMOKE,
                x = wx, y = wy, z = state.position.z + 0.18f,
                vx = -fwdX * 1.5f + (Random.nextFloat() - 0.5f) * 1.4f,
                vy = -fwdY * 1.5f + (Random.nextFloat() - 0.5f) * 1.4f,
                vz = 0.7f + Random.nextFloat() * 0.6f,
                size = 0.8f,
                maxLife = 0.55f
            )
        }

        // 2. Off-track gravel / grass dust
        if (state.isOffTrack && state.speedMs > 8f) {
            val side = if (Random.nextBoolean()) 1f else -1f
            spawn(
                budget = budget,
                kind = ParticleKind.GRAVEL_DUST,
                x = rearCenterX + rightX * wheelHalfTrack * side,
                y = rearCenterY + rightY * wheelHalfTrack * side,
                z = state.position.z + 0.15f,
                vx = -fwdX * 2f + (Random.nextFloat() - 0.5f) * 2.0f,
                vy = -fwdY * 2f + (Random.nextFloat() - 0.5f) * 2.0f,
                vz = 1.2f + Random.nextFloat() * 0.9f,
                size = 1.0f,
                maxLife = 0.65f
            )
        }

        // 3. Wet rooster-tail spray behind rear wheels
        if (weather.isRain && state.speedMs > 22f) {
            val side = if (Random.nextBoolean()) 1f else -1f
            spawn(
                budget = budget,
                kind = ParticleKind.WET_SPRAY,
                x = rearCenterX + rightX * wheelHalfTrack * side,
                y = rearCenterY + rightY * wheelHalfTrack * side,
                z = state.position.z + 0.35f,
                vx = -fwdX * 4f + (Random.nextFloat() - 0.5f) * 1.5f,
                vy = -fwdY * 4f + (Random.nextFloat() - 0.5f) * 1.5f,
                vz = 1.8f + Random.nextFloat() * 1.2f,
                size = 1.1f,
                maxLife = 0.45f
            )
        }

        // 4. Titanium floor sparks at high aero downforce or curb strikes
        if ((state.speedKph > 265f || (state.isOnCurb && state.speedKph > 120f)) && Random.nextFloat() > 0.35f) {
            spawn(
                budget = budget,
                kind = ParticleKind.SKID_SPARK,
                x = rearCenterX + (Random.nextFloat() - 0.5f) * 0.5f,
                y = rearCenterY + (Random.nextFloat() - 0.5f) * 0.5f,
                z = state.position.z + 0.08f,
                vx = -fwdX * 8f + (Random.nextFloat() - 0.5f) * 4.5f,
                vy = -fwdY * 8f + (Random.nextFloat() - 0.5f) * 4.5f,
                vz = 1.2f + Random.nextFloat() * 2.2f,
                size = 1.0f,
                maxLife = 0.28f
            )
        }

        // 5. Exhaust flame pop on shifts / rev limiter
        if (state.exhaustPopTimer > 0f) {
            spawn(
                budget = budget,
                kind = ParticleKind.EXHAUST_FLAME,
                x = state.position.x - fwdX * 2.15f,
                y = state.position.y - fwdY * 2.15f,
                z = state.position.z + 0.42f,
                vx = -fwdX * 6f,
                vy = -fwdY * 6f,
                vz = 0.2f,
                size = 0.9f,
                maxLife = 0.14f
            )
        }
    }

    private fun spawn(
        budget: Int,
        kind: ParticleKind,
        x: Float,
        y: Float,
        z: Float,
        vx: Float,
        vy: Float,
        vz: Float,
        size: Float,
        maxLife: Float
    ) {
        val idx = spawnCursor % budget
        spawnCursor++
        val p = pool[idx]
        p.active = true
        p.kind = kind
        p.x = x
        p.y = y
        p.z = z
        p.vx = vx
        p.vy = vy
        p.vz = vz
        p.size = size
        p.life = 0f
        p.maxLife = maxLife
    }
}
