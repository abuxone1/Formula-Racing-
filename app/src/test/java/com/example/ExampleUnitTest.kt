package com.example

import com.example.ai.RaceStrategyReport
import com.example.ai.SetupRecommendation
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testRaceStrategyReportJsonParsing() {
        val sampleJson = """
            {
                "callsign": "Apex Pit Wall",
                "situationSummary": "Pace is strong on mediums, tire wear 35%.",
                "pitWindowRecommendation": "Box Lap 5 for Hard tires",
                "undercutPotential": "High",
                "tireManagementAdvice": "Protect rear-left on traction zones.",
                "ersFuelStrategy": "Mode Push to Pass into Turn 1.",
                "engineerRadioQuote": "Box box, box box, confirm box."
            }
        """.trimIndent()

        val json = JSONObject(sampleJson)
        val report = RaceStrategyReport(
            callsign = json.getString("callsign"),
            situationSummary = json.getString("situationSummary"),
            pitWindowRecommendation = json.getString("pitWindowRecommendation"),
            undercutPotential = json.getString("undercutPotential"),
            tireManagementAdvice = json.getString("tireManagementAdvice"),
            ersFuelStrategy = json.getString("ersFuelStrategy"),
            engineerRadioQuote = json.getString("engineerRadioQuote")
        )

        assertEquals("Apex Pit Wall", report.callsign)
        assertEquals("High", report.undercutPotential)
        assertTrue(report.pitWindowRecommendation.contains("Lap 5"))
    }

    @Test
    fun testSetupRecommendationJsonParsing() {
        val sampleJson = """
            {
                "trackName": "Autodromo Nazionale Monza",
                "recommendedFrontWingDeg": 18.5,
                "recommendedRearWingDeg": 22.0,
                "recommendedBrakeBiasPercent": 57.0,
                "recommendedFrontSpringNmm": 150.0,
                "recommendedRearSpringNmm": 130.0,
                "rationale": "Low drag Monza aero package for maximum 350+ km/h top speed.",
                "chiefEngineerAdvice": "Late braking into Variante del Rettifilo."
            }
        """.trimIndent()

        val json = JSONObject(sampleJson)
        val rec = SetupRecommendation(
            trackName = json.getString("trackName"),
            recommendedFrontWingDeg = json.getDouble("recommendedFrontWingDeg").toFloat(),
            recommendedRearWingDeg = json.getDouble("recommendedRearWingDeg").toFloat(),
            recommendedBrakeBiasPercent = json.getDouble("recommendedBrakeBiasPercent").toFloat(),
            recommendedFrontSpringNmm = json.getDouble("recommendedFrontSpringNmm").toFloat(),
            recommendedRearSpringNmm = json.getDouble("recommendedRearSpringNmm").toFloat(),
            rationale = json.getString("rationale"),
            chiefEngineerAdvice = json.getString("chiefEngineerAdvice")
        )

        assertEquals("Autodromo Nazionale Monza", rec.trackName)
        assertEquals(18.5f, rec.recommendedFrontWingDeg, 0.01f)
        assertEquals(22.0f, rec.recommendedRearWingDeg, 0.01f)
    }
}
