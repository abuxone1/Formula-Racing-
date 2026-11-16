package com.example.ai

import android.util.Log
import com.example.model.CarSetup
import com.example.model.TrackLevelId
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiRaceEngineer"

data class RaceStrategyReport(
    val callsign: String,
    val situationSummary: String,
    val pitWindowRecommendation: String,
    val undercutPotential: String,
    val tireManagementAdvice: String,
    val ersFuelStrategy: String,
    val engineerRadioQuote: String
)

data class SetupRecommendation(
    val trackName: String,
    val recommendedFrontWingDeg: Float,
    val recommendedRearWingDeg: Float,
    val recommendedBrakeBiasPercent: Float,
    val recommendedFrontSpringNmm: Float,
    val recommendedRearSpringNmm: Float,
    val rationale: String,
    val chiefEngineerAdvice: String
)

sealed interface AiOperationState<out T> {
    data object Idle : AiOperationState<Nothing>
    data object Loading : AiOperationState<Nothing>
    data class Streaming(val partialText: String) : AiOperationState<Nothing>
    data class Success<T>(val data: T) : AiOperationState<T>
    data class Error(val message: String) : AiOperationState<Nothing>
}

class GeminiRaceEngineerService {

    // Structured Strategy Schema per Firebase AI Logic guidelines (Schema.obj)
    private val strategySchema = Schema.obj(
        properties = mapOf(
            "callsign" to Schema.string(description = "Race Engineer callsign, e.g. Apex Pit Wall"),
            "situationSummary" to Schema.string(description = "Brief vehicle condition and pace summary"),
            "pitWindowRecommendation" to Schema.string(description = "Recommended lap to pit or STAY OUT, and tire compound"),
            "undercutPotential" to Schema.string(description = "High, Moderate, or Low undercut threat/opportunity"),
            "tireManagementAdvice" to Schema.string(description = "Guidance on tire temperatures and wear mitigation"),
            "ersFuelStrategy" to Schema.string(description = "Engine mode and ERS battery harvest/deploy instruction"),
            "engineerRadioQuote" to Schema.string(description = "Authentic F1 race engineer radio message")
        )
    )

    // Structured Setup Schema
    private val setupSchema = Schema.obj(
        properties = mapOf(
            "trackName" to Schema.string(description = "Target circuit name"),
            "recommendedFrontWingDeg" to Schema.double(description = "Front wing angle in degrees between 15 and 38"),
            "recommendedRearWingDeg" to Schema.double(description = "Rear wing angle in degrees between 18 and 42"),
            "recommendedBrakeBiasPercent" to Schema.double(description = "Brake bias front percentage between 50 and 65"),
            "recommendedFrontSpringNmm" to Schema.double(description = "Front suspension spring stiffness between 100 and 180"),
            "recommendedRearSpringNmm" to Schema.double(description = "Rear suspension spring stiffness between 90 and 170"),
            "rationale" to Schema.string(description = "Technical justification for aero and suspension balance"),
            "chiefEngineerAdvice" to Schema.string(description = "Direct advice to the driver for the upcoming session")
        )
    )

    // Lazy initialization of Gemini 3.8 Flash models
    private val strategyModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = strategySchema
            }
        )
    }

    private val setupModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = setupSchema
            }
        )
    }

    private val conversationalModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    /**
     * Request a comprehensive AI Strategy Analysis using Gemini 3.8 Flash.
     */
    suspend fun analyzeStrategy(
        track: TrackLevelId,
        currentLap: Int,
        totalLaps: Int,
        position: Int,
        tireCompound: String,
        tireWearPercent: Float,
        frontLeftWear: Float,
        frontRightWear: Float,
        rearLeftWear: Float,
        rearRightWear: Float,
        fuelPercent: Float,
        ersPercent: Float,
        weather: String,
        lastLapTimeFormatted: String,
        bestLapTimeFormatted: String,
        gapAheadSec: Float,
        gapBehindSec: Float
    ): RaceStrategyReport {
        val prompt = """
            You are the Chief Race Engineer on the Apex Dynamics Formula Racing pit wall.
            Analyze the following live race telemetry and provide a tactical Grand Prix strategy recommendation:
            
            Circuit: ${track.trackTitle} (${track.country}, ${track.lengthKm} km)
            Current Lap: $currentLap of $totalLaps
            Current Race Position: P$position
            Tire Compound: $tireCompound
            Overall Tire Wear: ${"%.1f".format(tireWearPercent)}%
            Wheel Wear Breakdown: FL=${"%.1f".format(frontLeftWear)}%, FR=${"%.1f".format(frontRightWear)}%, RL=${"%.1f".format(rearLeftWear)}%, RR=${"%.1f".format(rearRightWear)}%
            Fuel Remaining: ${"%.1f".format(fuelPercent)}%
            ERS Energy Store: ${"%.1f".format(ersPercent)}%
            Weather Conditions: $weather
            Last Lap: $lastLapTimeFormatted (Personal Best: $bestLapTimeFormatted)
            Gap to Car Ahead: ${"%.2f".format(gapAheadSec)}s
            Gap to Car Behind: ${"%.2f".format(gapBehindSec)}s
            
            Provide actionable, realistic motorsport tactical recommendations.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                strategyModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Model returned empty strategy response.")
            val json = JSONObject(jsonText)
            RaceStrategyReport(
                callsign = json.optString("callsign", "Apex Pit Wall"),
                situationSummary = json.optString("situationSummary", "Telemetry assessed."),
                pitWindowRecommendation = json.optString("pitWindowRecommendation", "Maintain stint"),
                undercutPotential = json.optString("undercutPotential", "Moderate"),
                tireManagementAdvice = json.optString("tireManagementAdvice", "Monitor surface degradation."),
                ersFuelStrategy = json.optString("ersFuelStrategy", "Standard deployment."),
                engineerRadioQuote = json.optString("engineerRadioQuote", "Copy driver, keep your head down.")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Strategy inference timed out after 60s", e)
            throw IllegalStateException("Pit wall connection timed out. Please retry.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze race strategy with Gemini", e)
            throw e
        }
    }

    /**
     * Request aerodynamic and suspension setup recommendations for a specific track.
     */
    suspend fun recommendSetup(
        track: TrackLevelId,
        currentSetup: CarSetup,
        topSpeedRecordedKph: Float,
        handlingFeedback: String
    ): SetupRecommendation {
        val prompt = """
            You are the Chief Technical Director for Apex Dynamics Formula Racing.
            Recommend optimal aerodynamic wing angles, suspension spring rates, and brake bias for:
            
            Circuit: ${track.trackTitle} (${track.country}, Type: ${track.difficulty})
            Top Speed Achieved in Testing: ${"%.1f".format(topSpeedRecordedKph)} km/h
            Driver Handling Feedback: $handlingFeedback
            
            Current Setup:
            - Front Wing: ${"%.1f".format(currentSetup.frontWingAngle)}°
            - Rear Wing: ${"%.1f".format(currentSetup.rearWingAngle)}°
            - Brake Bias Front: ${"%.1f".format(currentSetup.brakeBias)}%
            - Tire Compound: ${currentSetup.tireCompound.name}
            
            Optimize for peak cornering grip, high-speed stability, and top-end straight line speed.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                setupModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Model returned empty setup response.")
            val json = JSONObject(jsonText)
            SetupRecommendation(
                trackName = json.optString("trackName", track.trackTitle),
                recommendedFrontWingDeg = json.optDouble("recommendedFrontWingDeg", currentSetup.frontWingAngle.toDouble()).toFloat(),
                recommendedRearWingDeg = json.optDouble("recommendedRearWingDeg", currentSetup.rearWingAngle.toDouble()).toFloat(),
                recommendedBrakeBiasPercent = json.optDouble("recommendedBrakeBiasPercent", currentSetup.brakeBias.toDouble()).toFloat(),
                recommendedFrontSpringNmm = json.optDouble("recommendedFrontSpringNmm", 140.0).toFloat(),
                recommendedRearSpringNmm = json.optDouble("recommendedRearSpringNmm", 125.0).toFloat(),
                rationale = json.optString("rationale", "Aero balanced for track characteristics."),
                chiefEngineerAdvice = json.optString("chiefEngineerAdvice", "Push on corner exit.")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Setup recommendation timed out", e)
            throw IllegalStateException("Setup recommendation timed out. Please retry.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recommend setup with Gemini", e)
            throw e
        }
    }

    /**
     * Interactive streaming Pit Wall Radio dialogue for immediate low-latency responses.
     */
    fun askPitWallStream(
        driverQuestion: String,
        telemetryContext: String
    ): Flow<String> = flow {
        val fullPrompt = """
            You are the senior Formula 1 Race Engineer on the radio for Apex Dynamics Motorsport.
            Speak directly to your driver in authentic, professional F1 pit-wall radio style (concise, sharp, tactical).
            
            Telemetry Context:
            $telemetryContext
            
            Driver asks: "$driverQuestion"
            
            Respond as the race engineer speaking over team radio. Keep it concise (2-4 sentences max).
        """.trimIndent()

        withTimeout(60_000L) {
            conversationalModel.generateContentStream(fullPrompt).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    emit(text)
                }
            }
        }
    }
}
