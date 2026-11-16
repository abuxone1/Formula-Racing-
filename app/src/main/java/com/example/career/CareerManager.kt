package com.example.career

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.model.AIDifficulty
import com.example.model.TrackLevelId
import org.json.JSONArray
import org.json.JSONObject

data class ChampionshipEvent(
    val eventId: String,
    val title: String,
    val subtitle: String,
    val track: TrackLevelId,
    val laps: Int,
    val prizeCredits: Int,
    val rewardXp: Int,
    var isCompleted: Boolean = false,
    var playerFinishedPosition: Int = 0,
    var earnedPoints: Int = 0,
    var earnedCredits: Int = 0,
    var earnedXp: Int = 0
)

enum class ChampionshipStageId(
    val stageNumber: Int,
    val title: String,
    val subtitle: String,
    val requiredXpToUnlock: Int,
    val recommendedDifficulty: AIDifficulty
) {
    STAGE_1_ROOKIE(
        1,
        "Rookie Championship",
        "Apex Proving Grounds • Entry Formula Training",
        0,
        AIDifficulty.BEGINNER
    ),
    STAGE_2_AMATEUR(
        2,
        "Amateur Championship",
        "Temple of Speed (Monza) • Medium Downforce",
        1200,
        AIDifficulty.INTERMEDIATE
    ),
    STAGE_3_PROFESSIONAL(
        3,
        "Professional Grand Prix",
        "Spa-Francorchamps • Rollercoaster Weather",
        3500,
        AIDifficulty.PROFESSIONAL
    ),
    STAGE_4_ELITE(
        4,
        "Elite Motorsport Cup",
        "Marina Bay Street Circuit • Night Floodlights",
        7500,
        AIDifficulty.PROFESSIONAL
    ),
    STAGE_5_WORLD(
        5,
        "World Racing Championship",
        "Apex & World Grand Finale • Ultimate Glory",
        14000,
        AIDifficulty.ELITE
    )
}

data class ChampionshipStage(
    val stageId: ChampionshipStageId,
    val events: List<ChampionshipEvent>,
    var isUnlocked: Boolean = false,
    var isTrophyAwarded: Boolean = false
) {
    val totalEvents: Int get() = events.size
    val completedEventsCount: Int get() = events.count { it.isCompleted }
    val isStageCompleted: Boolean get() = completedEventsCount >= totalEvents
    val totalStagePoints: Int get() = events.sumOf { it.earnedPoints }
}

data class GarageVehicleDef(
    val vehicleId: String,
    val name: String,
    val category: String,
    val description: String,
    val priceCredits: Int,
    val baseTopSpeedKph: Float,
    val baseAcceleration: Int,
    val baseHandling: Int,
    val baseBraking: Int,
    val baseEnginePowerBhp: Int
)

class CareerManager(private val context: Context? = null) {

    var driverName by mutableStateOf("Liam Vance")
    var driverLevel by mutableIntStateOf(1)
    var experiencePoints by mutableIntStateOf(0)
    var credits by mutableIntStateOf(5000) // Generous starting credits for garage purchases
    var careerTrophiesCount by mutableIntStateOf(0)

    val stages = mutableStateListOf<ChampionshipStage>()

    var activeEvent: ChampionshipEvent? by mutableStateOf(null)
    var activeStage: ChampionshipStageId? by mutableStateOf(null)

    // Garage & Vehicle Customization State
    var selectedVehicleId by mutableStateOf("apex_f1_pro")
    val ownedVehicleIds = mutableStateListOf("apex_f1_pro")

    var currentPaintHex by mutableLongStateOf(0xFF1801L) // Apex Red default
    var currentLivery by mutableStateOf("Apex Factory")
    var currentRim by mutableStateOf("Forged Carbon")
    var currentSpoiler by mutableStateOf("High Downforce Wing")
    var currentDecal by mutableStateOf("Apex Motorsport")
    var currentDriverNumber by mutableIntStateOf(44)

    // Performance Upgrade Stages (0 to 3)
    var engineUpgradeStage by mutableIntStateOf(0)
    var turboUpgradeStage by mutableIntStateOf(0)
    var brakeUpgradeStage by mutableIntStateOf(0)
    var tireUpgradeStage by mutableIntStateOf(0)
    var suspensionUpgradeStage by mutableIntStateOf(0)
    var aeroUpgradeStage by mutableIntStateOf(0)

    val availableVehicles = listOf(
        GarageVehicleDef("apex_f1_pro", "Apex F1 Pro", "Formula 1", "Factory Championship Contender • Unmatched Aero Agility", 0, 345f, 94, 95, 92, 1020),
        GarageVehicleDef("vortex_hypergt", "Vortex HyperGT", "Hypercar GT", "High Downforce Ground-Effect GT • Supreme Cornering Grip", 15000, 360f, 96, 96, 94, 1080),
        GarageVehicleDef("thunder_electric", "Thunder Electric X", "Formula E / EV", "Instant Electric Torque Monster • Blistering Acceleration", 28000, 375f, 99, 93, 97, 1150),
        GarageVehicleDef("retro_vintage", "Retro Vintage GP", "Historic Turbo Era", "1980s Raw Mechanical Beast • Wild Turbo Lag & Pure Power", 8500, 310f, 88, 89, 84, 890)
    )

    private val prefsName = "apex_formula_career_prefs"
    private val prefsKey = "career_save_json"

    init {
        initializeDefaultStages()
        loadCareerProgress()
    }

    fun purchaseVehicle(vehicle: GarageVehicleDef): Boolean {
        if (ownedVehicleIds.contains(vehicle.vehicleId)) {
            selectedVehicleId = vehicle.vehicleId
            saveCareerProgress()
            return true
        }
        if (credits >= vehicle.priceCredits) {
            credits -= vehicle.priceCredits
            ownedVehicleIds.add(vehicle.vehicleId)
            selectedVehicleId = vehicle.vehicleId
            saveCareerProgress()
            return true
        }
        return false
    }

    fun upgradePart(partName: String, cost: Int): Boolean {
        if (credits >= cost) {
            when (partName) {
                "engine" -> if (engineUpgradeStage < 3) { engineUpgradeStage++; credits -= cost }
                "turbo" -> if (turboUpgradeStage < 3) { turboUpgradeStage++; credits -= cost }
                "brakes" -> if (brakeUpgradeStage < 3) { brakeUpgradeStage++; credits -= cost }
                "tires" -> if (tireUpgradeStage < 3) { tireUpgradeStage++; credits -= cost }
                "suspension" -> if (suspensionUpgradeStage < 3) { suspensionUpgradeStage++; credits -= cost }
                "aero" -> if (aeroUpgradeStage < 3) { aeroUpgradeStage++; credits -= cost }
            }
            saveCareerProgress()
            return true
        }
        return false
    }

    private fun initializeDefaultStages() {
        stages.clear()
        stages.add(
            ChampionshipStage(
                stageId = ChampionshipStageId.STAGE_1_ROOKIE,
                isUnlocked = true,
                events = listOf(
                    ChampionshipEvent("rookie_1", "Apex Sprint 1", "5 Laps • Dry Clear Skies", TrackLevelId.LEVEL_1_APEX, 5, 800, 350),
                    ChampionshipEvent("rookie_2", "Apex Chicane Cup", "6 Laps • Technical Corners", TrackLevelId.LEVEL_1_APEX, 6, 950, 420),
                    ChampionshipEvent("rookie_3", "Apex Grand Finale", "8 Laps • Full Feature Race", TrackLevelId.LEVEL_1_APEX, 8, 1400, 600)
                )
            )
        )
        stages.add(
            ChampionshipStage(
                stageId = ChampionshipStageId.STAGE_2_AMATEUR,
                isUnlocked = false,
                events = listOf(
                    ChampionshipEvent("amateur_1", "Monza Sprint GP", "6 Laps • Sunset Golden Hour", TrackLevelId.LEVEL_2_MONZA, 6, 1500, 700),
                    ChampionshipEvent("amateur_2", "Rettifilo Chicane Challenge", "8 Laps • Heavy Braking Zones", TrackLevelId.LEVEL_2_MONZA, 8, 1900, 850),
                    ChampionshipEvent("amateur_3", "Monza Trophy Final", "10 Laps • Temple of Speed", TrackLevelId.LEVEL_2_MONZA, 10, 2600, 1150)
                )
            )
        )
        stages.add(
            ChampionshipStage(
                stageId = ChampionshipStageId.STAGE_3_PROFESSIONAL,
                isUnlocked = false,
                events = listOf(
                    ChampionshipEvent("pro_1", "Spa Wet Rollercoaster", "7 Laps • Heavy Rain Wet Track", TrackLevelId.LEVEL_3_SPA, 7, 3200, 1400),
                    ChampionshipEvent("pro_2", "Eau Rouge Endurance", "9 Laps • Elevation Climb", TrackLevelId.LEVEL_3_SPA, 9, 3900, 1750),
                    ChampionshipEvent("pro_3", "Belgian GP Championship", "12 Laps • Legendary Grand Prix", TrackLevelId.LEVEL_3_SPA, 12, 5200, 2300)
                )
            )
        )
        stages.add(
            ChampionshipStage(
                stageId = ChampionshipStageId.STAGE_4_ELITE,
                isUnlocked = false,
                events = listOf(
                    ChampionshipEvent("elite_1", "Marina Bay Night Sprint", "8 Laps • Night GP Floodlit", TrackLevelId.LEVEL_4_MARINA, 8, 6000, 2600),
                    ChampionshipEvent("elite_2", "Singapore Street Battle", "10 Laps • Wall-Lined 90-Deg Turns", TrackLevelId.LEVEL_4_MARINA, 10, 7500, 3200),
                    ChampionshipEvent("elite_3", "Marina Bay Elite Cup", "14 Laps • Championship Showdown", TrackLevelId.LEVEL_4_MARINA, 14, 9800, 4200)
                )
            )
        )
        stages.add(
            ChampionshipStage(
                stageId = ChampionshipStageId.STAGE_5_WORLD,
                isUnlocked = false,
                events = listOf(
                    ChampionshipEvent("world_1", "World Series Round 1", "10 Laps • Monza Sunset", TrackLevelId.LEVEL_2_MONZA, 10, 11000, 4800),
                    ChampionshipEvent("world_2", "World Series Round 2", "12 Laps • Spa Wet Rain", TrackLevelId.LEVEL_3_SPA, 12, 13500, 5800),
                    ChampionshipEvent("world_3", "Apex World Grand Finale", "18 Laps • Ultimate World Championship", TrackLevelId.LEVEL_1_APEX, 18, 22000, 9500)
                )
            )
        )
    }

    fun recordRaceResult(
        eventId: String,
        position: Int,
        isChampionshipEvent: Boolean
    ) {
        val pointsAwarded = when (position) {
            1 -> 25
            2 -> 18
            3 -> 15
            4 -> 12
            5 -> 10
            6 -> 8
            7 -> 6
            8 -> 4
            9 -> 2
            10 -> 1
            else -> 0
        }

        if (isChampionshipEvent) {
            for (stage in stages) {
                val ev = stage.events.find { it.eventId == eventId }
                if (ev != null) {
                    ev.isCompleted = true
                    ev.playerFinishedPosition = position
                    ev.earnedPoints = pointsAwarded
                    val mult = when (position) {
                        1 -> 1.5f
                        2 -> 1.3f
                        3 -> 1.1f
                        else -> 1.0f
                    }
                    ev.earnedCredits = (ev.prizeCredits * mult).toInt()
                    ev.earnedXp = (ev.rewardXp * mult).toInt()

                    credits += ev.earnedCredits
                    addExperience(ev.earnedXp)
                    break
                }
            }
        } else {
            val quickCredits = (600 / position).coerceAtLeast(150)
            val quickXp = (280 / position).coerceAtLeast(80)
            credits += quickCredits
            addExperience(quickXp)
        }

        checkStageUnlocksAndTrophies()
        saveCareerProgress()
    }

    private fun addExperience(amount: Int) {
        experiencePoints += amount
        val newLevel = 1 + (experiencePoints / 1500)
        if (newLevel > driverLevel) {
            driverLevel = newLevel
            credits += 1500
        }
    }

    private fun checkStageUnlocksAndTrophies() {
        var trophies = 0
        for (i in 0 until stages.size) {
            val stage = stages[i]
            if (stage.isStageCompleted) {
                if (!stage.isTrophyAwarded) {
                    stage.isTrophyAwarded = true
                    trophies++
                    credits += 5000
                }
                if (i + 1 < stages.size) {
                    val nextStage = stages[i + 1]
                    if (experiencePoints >= nextStage.stageId.requiredXpToUnlock) {
                        nextStage.isUnlocked = true
                    }
                }
            } else {
                if (i == 0 || (stages[i - 1].isStageCompleted && experiencePoints >= stage.stageId.requiredXpToUnlock)) {
                    stage.isUnlocked = true
                }
            }
        }
        careerTrophiesCount = stages.count { it.isTrophyAwarded }
    }

    fun saveCareerProgress() {
        val ctx = context ?: return
        try {
            val root = JSONObject().apply {
                put("driverName", driverName)
                put("driverLevel", driverLevel)
                put("experiencePoints", experiencePoints)
                put("credits", credits)
                put("careerTrophiesCount", careerTrophiesCount)

                // Garage state
                put("selectedVehicleId", selectedVehicleId)
                val ownedArr = JSONArray()
                for (vId in ownedVehicleIds) ownedArr.put(vId)
                put("ownedVehicleIds", ownedArr)

                put("currentPaintHex", currentPaintHex)
                put("currentLivery", currentLivery)
                put("currentRim", currentRim)
                put("currentSpoiler", currentSpoiler)
                put("currentDecal", currentDecal)
                put("currentDriverNumber", currentDriverNumber)

                put("engineUpgradeStage", engineUpgradeStage)
                put("turboUpgradeStage", turboUpgradeStage)
                put("brakeUpgradeStage", brakeUpgradeStage)
                put("tireUpgradeStage", tireUpgradeStage)
                put("suspensionUpgradeStage", suspensionUpgradeStage)
                put("aeroUpgradeStage", aeroUpgradeStage)

                val stagesArray = JSONArray()
                for (stage in stages) {
                    val stageObj = JSONObject().apply {
                        put("stageId", stage.stageId.name)
                        put("isUnlocked", stage.isUnlocked)
                        put("isTrophyAwarded", stage.isTrophyAwarded)

                        val eventsArray = JSONArray()
                        for (ev in stage.events) {
                            val evObj = JSONObject().apply {
                                put("eventId", ev.eventId)
                                put("isCompleted", ev.isCompleted)
                                put("playerFinishedPosition", ev.playerFinishedPosition)
                                put("earnedPoints", ev.earnedPoints)
                                put("earnedCredits", ev.earnedCredits)
                                put("earnedXp", ev.earnedXp)
                            }
                            eventsArray.put(evObj)
                        }
                        put("events", eventsArray)
                    }
                    stagesArray.put(stageObj)
                }
                put("stages", stagesArray)
            }

            val prefs = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.edit().putString(prefsKey, root.toString()).apply()
        } catch (_: Exception) {}
    }

    private fun loadCareerProgress() {
        val ctx = context ?: return
        try {
            val prefs = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val jsonStr = prefs.getString(prefsKey, null) ?: return
            val root = JSONObject(jsonStr)

            driverName = root.optString("driverName", driverName)
            driverLevel = root.optInt("driverLevel", driverLevel)
            experiencePoints = root.optInt("experiencePoints", experiencePoints)
            credits = root.optInt("credits", credits)
            careerTrophiesCount = root.optInt("careerTrophiesCount", careerTrophiesCount)

            selectedVehicleId = root.optString("selectedVehicleId", selectedVehicleId)
            val ownedArr = root.optJSONArray("ownedVehicleIds")
            if (ownedArr != null) {
                ownedVehicleIds.clear()
                for (i in 0 until ownedArr.length()) {
                    val vId = ownedArr.optString(i)
                    if (vId.isNotEmpty()) ownedVehicleIds.add(vId)
                }
            }

            currentPaintHex = root.optLong("currentPaintHex", currentPaintHex)
            currentLivery = root.optString("currentLivery", currentLivery)
            currentRim = root.optString("currentRim", currentRim)
            currentSpoiler = root.optString("currentSpoiler", currentSpoiler)
            currentDecal = root.optString("currentDecal", currentDecal)
            currentDriverNumber = root.optInt("currentDriverNumber", currentDriverNumber)

            engineUpgradeStage = root.optInt("engineUpgradeStage", engineUpgradeStage)
            turboUpgradeStage = root.optInt("turboUpgradeStage", turboUpgradeStage)
            brakeUpgradeStage = root.optInt("brakeUpgradeStage", brakeUpgradeStage)
            tireUpgradeStage = root.optInt("tireUpgradeStage", tireUpgradeStage)
            suspensionUpgradeStage = root.optInt("suspensionUpgradeStage", suspensionUpgradeStage)
            aeroUpgradeStage = root.optInt("aeroUpgradeStage", aeroUpgradeStage)

            val stagesArray = root.optJSONArray("stages") ?: return
            for (i in 0 until stagesArray.length()) {
                val stageObj = stagesArray.optJSONObject(i) ?: continue
                val sIdName = stageObj.optString("stageId", "")
                val stage = stages.find { it.stageId.name == sIdName } ?: continue

                stage.isUnlocked = stageObj.optBoolean("isUnlocked", stage.isUnlocked)
                stage.isTrophyAwarded = stageObj.optBoolean("isTrophyAwarded", stage.isTrophyAwarded)

                val eventsArray = stageObj.optJSONArray("events") ?: continue
                for (j in 0 until eventsArray.length()) {
                    val evObj = eventsArray.optJSONObject(j) ?: continue
                    val eId = evObj.optString("eventId", "")
                    val ev = stage.events.find { it.eventId == eId } ?: continue

                    ev.isCompleted = evObj.optBoolean("isCompleted", false)
                    ev.playerFinishedPosition = evObj.optInt("playerFinishedPosition", 0)
                    ev.earnedPoints = evObj.optInt("earnedPoints", 0)
                    ev.earnedCredits = evObj.optInt("earnedCredits", 0)
                    ev.earnedXp = evObj.optInt("earnedXp", 0)
                }
            }
            checkStageUnlocksAndTrophies()
        } catch (_: Exception) {}
    }
}
