package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class FormulaAudioEngine {

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var audioJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Real-time audio parameters controlled by physics
    @Volatile var targetRpm: Float = 4200f
    @Volatile var throttle: Float = 0f
    @Volatile var isBraking: Boolean = false
    @Volatile var tireSlip: Float = 0f
    @Volatile var isGearShifting: Boolean = false
    @Volatile var masterVolume: Float = 0.85f
    @Volatile var isMuted: Boolean = false

    private val sampleRate = 44100
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(2048)

    fun start() {
        if (isPlaying) return
        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            isPlaying = true

            audioJob = scope.launch {
                streamEngineAudio()
            }
        } catch (e: Exception) {
            Log.e("FormulaAudioEngine", "Failed to start AudioTrack", e)
        }
    }

    private suspend fun streamEngineAudio() = withContext(Dispatchers.Default) {
        val audioData = ShortArray(bufferSize)
        var phaseMain = 0.0
        var phaseHarmonic1 = 0.0
        var phaseHarmonic2 = 0.0
        var phaseTurbo = 0.0
        var smoothedRpm = targetRpm

        while (isPlaying && isActive) {
            smoothedRpm += (targetRpm - smoothedRpm) * 0.15f

            // V6 4-stroke engine pulse frequency = 3 * (RPM / 60)
            val fundamentalFreq = (3f * (smoothedRpm / 60f)).coerceIn(120f, 900f)
            val turboFreq = 2600f + (smoothedRpm / 15000f) * 1200f

            val dPhaseMain = (2.0 * PI * fundamentalFreq) / sampleRate
            val dPhaseH1 = dPhaseMain * 2.0
            val dPhaseH2 = dPhaseMain * 3.0
            val dPhaseTurbo = (2.0 * PI * turboFreq) / sampleRate

            val volume = if (isMuted) 0f else masterVolume
            val isRevLimiterHit = smoothedRpm >= 14750f

            for (i in 0 until bufferSize) {
                if (isRevLimiterHit && (i / 400) % 2 == 0) {
                    // Stutter cut for rev limiter
                    audioData[i] = 0
                    continue
                }

                if (isGearShifting && (i / 800) % 2 == 0) {
                    // Ignition cut pop during gearshift
                    val pop = (Random.nextFloat() * 2f - 1f) * 0.4f
                    audioData[i] = (pop * Short.MAX_VALUE * volume).toInt().toShort()
                    continue
                }

                // Complex harmonic synthesis for Formula V6 Turbo scream
                val wave1 = sin(phaseMain)
                val wave2 = sin(phaseHarmonic1) * 0.55
                val wave3 = sin(phaseHarmonic2) * 0.35
                val distortion = wave1 * wave1 * wave1 * 0.2 // subtle tube saturation

                // High-pitched turbo spool whistling
                val turboWhistle = sin(phaseTurbo) * (0.05 + throttle * 0.12)

                // Tire screech noise when high lateral slip occurs
                val tireNoise = if (tireSlip > 0.35f) {
                    (Random.nextFloat() * 2f - 1f) * (tireSlip * 0.25f)
                } else 0f

                val mixed = (wave1 * 0.5 + wave2 + wave3 + distortion + turboWhistle + tireNoise) * 0.35

                audioData[i] = (mixed.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * volume).toInt().toShort()

                phaseMain += dPhaseMain
                if (phaseMain > 2.0 * PI) phaseMain -= 2.0 * PI

                phaseHarmonic1 += dPhaseH1
                if (phaseHarmonic1 > 2.0 * PI) phaseHarmonic1 -= 2.0 * PI

                phaseHarmonic2 += dPhaseH2
                if (phaseHarmonic2 > 2.0 * PI) phaseHarmonic2 -= 2.0 * PI

                phaseTurbo += dPhaseTurbo
                if (phaseTurbo > 2.0 * PI) phaseTurbo -= 2.0 * PI
            }

            audioTrack?.write(audioData, 0, bufferSize)
        }
    }

    fun stop() {
        isPlaying = false
        audioJob?.cancel()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
