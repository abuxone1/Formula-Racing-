package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ai.GeminiRaceEngineerService
import com.example.career.CareerManager
import com.example.firebase.FirestoreRepository
import com.example.firebase.GoogleAuthManager
import com.example.firebase.configureAppCheck
import com.example.game.RaceSimulationManager
import com.example.model.GameScreen
import com.example.ui.screens.*
import com.example.ui.theme.CarbonBlack
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var simulationManager: RaceSimulationManager
    private lateinit var authManager: GoogleAuthManager
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var geminiService: GeminiRaceEngineerService
    private lateinit var careerManager: CareerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory App Check initialization with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        simulationManager = RaceSimulationManager(applicationContext)
        authManager = GoogleAuthManager(applicationContext)
        firestoreRepository = FirestoreRepository(applicationContext)
        geminiService = GeminiRaceEngineerService()
        careerManager = CareerManager(applicationContext)

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(GameScreen.MAIN_MENU) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CarbonBlack)
                ) {
                    Crossfade(
                        targetState = currentScreen,
                        label = "screen_crossfade"
                    ) { screen ->
                        when (screen) {
                            GameScreen.MAIN_MENU -> {
                                MainMenuScreen(
                                    currentLevel = simulationManager.circuit.currentLevel,
                                    onNavigate = { target -> currentScreen = target }
                                )
                            }
                            GameScreen.LEVEL_SELECT -> {
                                LevelSelectScreen(
                                    currentLevel = simulationManager.circuit.currentLevel,
                                    currentWeather = simulationManager.weather,
                                    onSelectLevel = { level, weather ->
                                        simulationManager.selectTrackLevel(level)
                                        simulationManager.weather = weather
                                    },
                                    onStartRace = { level, weather ->
                                        simulationManager.selectTrackLevel(level)
                                        simulationManager.weather = weather
                                        currentScreen = GameScreen.RACE
                                    },
                                    onNavigateBack = { currentScreen = GameScreen.MAIN_MENU }
                                )
                            }
                            GameScreen.PHYSICS_LAB -> {
                                PhysicsLabScreen(
                                    onNavigateBack = { currentScreen = GameScreen.MAIN_MENU }
                                )
                            }
                            GameScreen.RACE -> {
                                RaceScreen(
                                    sim = simulationManager,
                                    careerManager = careerManager,
                                    onNavigateBack = { currentScreen = GameScreen.MAIN_MENU },
                                    onNavigateToReplay = { currentScreen = GameScreen.REPLAY_PHOTO_MODE },
                                    onNavigateToTelemetry = { currentScreen = GameScreen.TELEMETRY_ANALYSIS }
                                )
                            }
                            GameScreen.GARAGE -> {
                                GarageScreen(
                                    setup = simulationManager.carSetup,
                                    careerManager = careerManager,
                                    geminiService = geminiService,
                                    currentTrack = simulationManager.circuit.currentLevel,
                                    onNavigateBack = { currentScreen = GameScreen.MAIN_MENU },
                                    onStartTestDrive = { currentScreen = GameScreen.RACE }
                                )
                            }
                            GameScreen.CHAMPIONSHIP -> {
                                ChampionshipScreen(
                                    careerManager = careerManager,
                                    onNavigateBack = { currentScreen = GameScreen.MAIN_MENU },
                                    onStartCareerEvent = { event, stage ->
                                        simulationManager.selectTrackLevel(event.track)
                                        simulationManager.totalLaps = event.laps
                                        simulationManager.aiDifficulty = stage.stageId.recommendedDifficulty
                                        simulationManager.startRaceSession()
                                        careerManager.activeEvent = event
                                        careerManager.activeStage = stage.stageId
                                        currentScreen = GameScreen.RACE
                                    }
                                )
                            }
                            GameScreen.SETTINGS -> {
                                SettingsScreen(
                                    sim = simulationManager,
                                    onNavigateBack = { currentScreen = GameScreen.MAIN_MENU }
                                )
                            }
                            GameScreen.REPLAY_PHOTO_MODE -> {
                                ReplayScreen(
                                    sim = simulationManager,
                                    onNavigateBack = { currentScreen = GameScreen.MAIN_MENU }
                                )
                            }
                            GameScreen.TELEMETRY_ANALYSIS -> {
                                TelemetryAnalysisScreen(
                                    sim = simulationManager,
                                    geminiService = geminiService,
                                    onNavigateBack = { currentScreen = GameScreen.MAIN_MENU }
                                )
                            }
                            GameScreen.DRIVER_CUSTOMIZATION -> {
                                CustomizationScreen(
                                    profile = simulationManager.driverProfile,
                                    onNavigateBack = { currentScreen = GameScreen.MAIN_MENU }
                                )
                            }
                            GameScreen.ARCHITECTURE_DOCS -> {
                                ArchitectureDocsScreen(
                                    onNavigateBack = { currentScreen = GameScreen.MAIN_MENU }
                                )
                            }
                            GameScreen.CLOUD_LEADERBOARD -> {
                                CloudLeaderboardScreen(
                                    sim = simulationManager,
                                    authManager = authManager,
                                    repository = firestoreRepository,
                                    onNavigateBack = { currentScreen = GameScreen.MAIN_MENU }
                                )
                            }
                            GameScreen.DAILY_CHALLENGES -> {
                                ChallengesScreen(
                                    careerManager = careerManager,
                                    onNavigateBack = { currentScreen = GameScreen.MAIN_MENU }
                                )
                            }
                            GameScreen.ACHIEVEMENTS, GameScreen.RACE_PREP -> {
                                LevelSelectScreen(
                                    currentLevel = simulationManager.circuit.currentLevel,
                                    currentWeather = simulationManager.weather,
                                    onSelectLevel = { level, weather ->
                                        simulationManager.selectTrackLevel(level)
                                        simulationManager.weather = weather
                                    },
                                    onStartRace = { level, weather ->
                                        simulationManager.selectTrackLevel(level)
                                        simulationManager.weather = weather
                                        currentScreen = GameScreen.RACE
                                    },
                                    onNavigateBack = { currentScreen = GameScreen.MAIN_MENU }
                                )
                            }
                            GameScreen.ONLINE_MULTIPLAYER -> {
                                OnlineMultiplayerScreen(
                                    authManager = authManager,
                                    repository = firestoreRepository,
                                    onNavigateBack = { currentScreen = GameScreen.MAIN_MENU }
                                )
                            }
                            else -> {
                                MainMenuScreen(
                                    currentLevel = simulationManager.circuit.currentLevel,
                                    onNavigate = { target -> currentScreen = target }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (::simulationManager.isInitialized) {
            simulationManager.stopAudio()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::simulationManager.isInitialized) {
            simulationManager.stopAudio()
        }
    }
}
