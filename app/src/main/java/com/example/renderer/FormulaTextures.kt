package com.example.renderer

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates and caches lightweight procedural GPU-friendly textures (asphalt grain,
 * carbon fiber weave, turf shoulder) once per quality tier, eliminating per-frame allocations.
 */
class FormulaTextures {

    private var cachedResolution = -1

    lateinit var asphaltDryBrush: ShaderBrush
        private set
    lateinit var asphaltWetBrush: ShaderBrush
        private set
    lateinit var carbonFiberBrush: ShaderBrush
        private set
    lateinit var turfGrassBrush: ShaderBrush
        private set
    lateinit var streetShoulderBrush: ShaderBrush
        private set

    fun ensureInitialized(resolution: Int) {
        val clampedRes = resolution.coerceIn(64, 512)
        if (cachedResolution == clampedRes) return
        cachedResolution = clampedRes

        asphaltDryBrush = buildAsphaltBrush(clampedRes, isWet = false)
        asphaltWetBrush = buildAsphaltBrush(clampedRes, isWet = true)
        carbonFiberBrush = buildCarbonWeaveBrush((clampedRes / 4).coerceIn(16, 64))
        turfGrassBrush = buildTurfBrush(clampedRes, isUrban = false)
        streetShoulderBrush = buildTurfBrush(clampedRes, isUrban = true)
    }

    private fun buildAsphaltBrush(size: Int, isWet: Boolean): ShaderBrush {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        val rng = Random(if (isWet) 9012 else 4512)

        val baseR = if (isWet) 26 else 36
        val baseG = if (isWet) 31 else 40
        val baseB = if (isWet) 40 else 48

        for (y in 0 until size) {
            val vNorm = y.toFloat() / size
            // Subtle racing groove darkness in the center & tire tracks
            val grooveWave = sin(vNorm * Math.PI * 2.0).toFloat() * 3f
            for (x in 0 until size) {
                val uNorm = x.toFloat() / size
                val grain = rng.nextInt(-9, 10)
                val pebble = if (rng.nextFloat() > 0.94f) 10 else 0
                val streak = (sin((uNorm * 8f + vNorm * 2f) * Math.PI).toFloat() * 2f).toInt()
                val sheen = if (isWet && pebble > 0) 14 else 0

                val r = (baseR + grain + pebble + streak + sheen + grooveWave.toInt()).coerceIn(10, 95)
                val g = (baseG + grain + pebble + streak + sheen + grooveWave.toInt()).coerceIn(12, 105)
                val b = (baseB + grain + pebble + streak + sheen + (if (isWet) 8 else 0)).coerceIn(16, 125)
                pixels[y * size + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return ShaderBrush(ImageShader(bmp.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }

    private fun buildCarbonWeaveBrush(size: Int): ShaderBrush {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        val cell = (size / 8).coerceAtLeast(2)

        val lightCarbon = Color(0xFF252B36).toArgb()
        val darkCarbon = Color(0xFF10131A).toArgb()
        val midCarbon = Color(0xFF1A1F28).toArgb()

        for (y in 0 until size) {
            for (x in 0 until size) {
                val diag = ((x + y) / cell) % 4
                pixels[y * size + x] = when (diag) {
                    0 -> lightCarbon
                    2 -> darkCarbon
                    else -> midCarbon
                }
            }
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return ShaderBrush(ImageShader(bmp.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }

    private fun buildTurfBrush(size: Int, isUrban: Boolean): ShaderBrush {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        val rng = Random(if (isUrban) 771 else 331)

        val baseR = if (isUrban) 20 else 25
        val baseG = if (isUrban) 24 else 58
        val baseB = if (isUrban) 32 else 28

        for (y in 0 until size) {
            for (x in 0 until size) {
                val n = rng.nextInt(-7, 8)
                val r = (baseR + n).coerceIn(8, 80)
                val g = (baseG + n + (if (!isUrban) rng.nextInt(0, 6) else 0)).coerceIn(10, 95)
                val b = (baseB + n).coerceIn(10, 80)
                pixels[y * size + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return ShaderBrush(ImageShader(bmp.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }
}
