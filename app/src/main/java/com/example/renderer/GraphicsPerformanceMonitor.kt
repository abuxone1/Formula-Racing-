package com.example.renderer

import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.example.model.GraphicsQuality

/**
 * Monitors frame render times, Power Saver mode, and device thermal status (API 29+)
 * to keep frame pacing stable and prevent thermal throttling / excessive battery drain.
 */
class GraphicsPerformanceMonitor(context: Context? = null) {

    private val powerManager: PowerManager? = try {
        context?.applicationContext?.getSystemService(Context.POWER_SERVICE) as? PowerManager
    } catch (_: Exception) {
        null
    }

    private var emaFrameTimeMs = 12f
    private var lastThermalCheckMs = 0L
    private var isDeviceHot = false
    private var isPowerSave = false

    fun recordFrameDurationMs(durationMs: Float) {
        emaFrameTimeMs = emaFrameTimeMs * 0.9f + durationMs.coerceIn(1f, 80f) * 0.1f
    }

    fun getEffectiveQuality(
        requested: GraphicsQuality,
        adaptiveThermalEnabled: Boolean
    ): GraphicsQuality {
        if (!adaptiveThermalEnabled) return requested

        val now = System.currentTimeMillis()
        if (now - lastThermalCheckMs > 2000L) {
            lastThermalCheckMs = now
            refreshSystemFlags()
        }

        return when {
            isPowerSave -> GraphicsQuality.LOW
            isDeviceHot && requested == GraphicsQuality.HIGH -> GraphicsQuality.MEDIUM
            isDeviceHot && requested == GraphicsQuality.MEDIUM -> GraphicsQuality.LOW
            emaFrameTimeMs > 28f && requested == GraphicsQuality.HIGH -> GraphicsQuality.MEDIUM
            emaFrameTimeMs > 42f && requested == GraphicsQuality.MEDIUM -> GraphicsQuality.LOW
            else -> requested
        }
    }

    fun getTargetFrameIntervalNs(
        requested: GraphicsQuality,
        adaptiveThermalEnabled: Boolean
    ): Long {
        val effective = getEffectiveQuality(requested, adaptiveThermalEnabled)
        return when (effective.targetFps) {
            30 -> 33_333_333L
            else -> 16_666_667L
        }
    }

    private fun refreshSystemFlags() {
        val pm = powerManager ?: return
        try {
            isPowerSave = pm.isPowerSaveMode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val status = pm.currentThermalStatus
                isDeviceHot = status >= PowerManager.THERMAL_STATUS_MODERATE
            }
        } catch (_: Exception) {
            isPowerSave = false
            isDeviceHot = false
        }
    }
}
