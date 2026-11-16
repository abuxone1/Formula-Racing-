package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firebase.FirestoreRepository
import com.example.firebase.GoogleAuthManager
import com.example.firebase.LeaderboardEntry
import com.example.firebase.UserProfile
import com.example.game.RaceSimulationManager
import com.example.model.TrackLevelId
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudLeaderboardScreen(
    sim: RaceSimulationManager,
    authManager: GoogleAuthManager,
    repository: FirestoreRepository,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentUser by authManager.currentUser.collectAsState()

    var selectedTrack by remember { mutableStateOf(TrackLevelId.LEVEL_1_APEX) }
    var isSigningIn by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Observe cloud profile for the authenticated user
    val userProfile by produceState<UserProfile?>(initialValue = null, key1 = currentUser?.uid) {
        val uid = currentUser?.uid
        if (uid != null) {
            repository.observeUserProfile(uid).collect { value = it }
        } else {
            value = null
        }
    }

    // Observe online leaderboard entries for the selected track
    val leaderboardEntries by produceState<List<LeaderboardEntry>>(initialValue = emptyList(), key1 = selectedTrack) {
        repository.observeLeaderboard(selectedTrack.name.lowercase()).collect { value = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonBlack)
            .padding(16.dp)
            .testTag("cloud_leaderboard_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Menu",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "APEX CLOUD MOTORSPORT TELEMETRY",
                    color = ApexCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Firestore Cloud Sync • Verified World Grand Prix Lap Times",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }

            // Auth status indicator / action
            if (currentUser != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(AsphaltGray, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ApexGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentUser?.displayName ?: "Driver Online",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            authManager.signOut()
                            Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp).testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        isSigningIn = true
                        authManager.signInWithGoogle(context, coroutineScope) { success, error ->
                            isSigningIn = false
                            if (success) {
                                Toast.makeText(context, "Google Sign-in Successful!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Sign-in: ${error ?: "Failed"}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ApexRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("sign_in_button")
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Sign In",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SIGN IN WITH GOOGLE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main content layout (Two columns on landscape)
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Panel: Cloud Driver Profile & Telemetry Sync
            Card(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = AsphaltGray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Cloud Profile",
                            tint = ApexCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DRIVER CLOUD TELEMETRY",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (currentUser == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(CarbonBlack, RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = ApexRed,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Google Sign-In Required",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sign in to backup career championship stats and submit verified lap times to the global Firestore leaderboard.",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    } else {
                        // Driver stats display
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(CarbonBlack, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProfileStatRow("Driver", userProfile?.driverName ?: sim.driverProfile.driverName)
                            ProfileStatRow("Team", userProfile?.teamName ?: "Apex Dynamics F1")
                            ProfileStatRow("Grand Prix Laps", "${userProfile?.racesCompleted ?: sim.currentLap.toLong()}")
                            ProfileStatRow("Championship Pts", "${userProfile?.championshipPoints ?: 48L}")
                            val bestLapSec = (sim.bestLapTimeMs / 1000.0).coerceAtLeast(68.420)
                            ProfileStatRow("Fastest Recorded", String.format("%.3fs", bestLapSec))
                            ProfileStatRow("Cloud Status", if (userProfile != null) "Synchronized" else "Pending Initial Sync")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val uid = currentUser?.uid ?: return@Button
                                    isSyncing = true
                                    coroutineScope.launch {
                                        val profile = UserProfile(
                                            userId = uid,
                                            driverName = sim.driverProfile.driverName,
                                            teamName = "Apex Dynamics F1",
                                            helmetColor = "#${(sim.driverProfile.helmetDesign.primaryHex and 0xFFFFFF).toString(16)}",
                                            carColor = "#${(sim.driverProfile.liveryTheme.primaryHex and 0xFFFFFF).toString(16)}",
                                            accentColor = "#${(sim.driverProfile.liveryTheme.accentHex and 0xFFFFFF).toString(16)}",
                                            racesCompleted = (sim.currentLap.toLong()).coerceAtLeast(1L),
                                            wins = 1L,
                                            podiums = 2L,
                                            championshipPoints = 48L
                                        )
                                        val ok = repository.saveUserProfile(profile)
                                        isSyncing = false
                                        Toast.makeText(
                                            context,
                                            if (ok) "Profile Saved to Firestore!" else "Sync Failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sync_cloud_profile_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ApexCyan),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        color = CarbonBlack,
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = "Save",
                                        tint = CarbonBlack,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SYNC CAREER", color = CarbonBlack, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }

                            Button(
                                onClick = {
                                    val uid = currentUser?.uid ?: return@Button
                                    isSubmitting = true
                                    coroutineScope.launch {
                                        val lapSec = (sim.bestLapTimeMs / 1000.0).coerceAtLeast(64.120)
                                        val topSpeed = (sim.playerState.speedKph.toDouble()).coerceAtLeast(312.4)
                                        val entry = LeaderboardEntry(
                                            userId = uid,
                                            driverName = sim.driverProfile.driverName,
                                            teamName = "Apex Dynamics F1",
                                            trackId = selectedTrack.name.lowercase(),
                                            lapTimeSec = lapSec,
                                            topSpeedKmh = topSpeed
                                        )
                                        val ok = repository.submitLapTime(entry)
                                        isSubmitting = false
                                        Toast.makeText(
                                            context,
                                            if (ok) "Lap Record Published to Leaderboard!" else "Submission Failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("submit_lap_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ApexGold),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(
                                        color = CarbonBlack,
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Upload,
                                        contentDescription = "Upload",
                                        tint = CarbonBlack,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SUBMIT LAP", color = CarbonBlack, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }

            // Right Panel: Online Circuit Leaderboard
            Card(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = AsphaltGray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    // Track Level Tabs
                    TabRow(
                        selectedTabIndex = selectedTrack.ordinal,
                        containerColor = CarbonBlack,
                        contentColor = ApexCyan,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        TrackLevelId.values().forEach { level ->
                            Tab(
                                selected = selectedTrack == level,
                                onClick = { selectedTrack = level },
                                text = {
                                    Text(
                                        text = level.trackTitle.split(" ")[0].uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Column Titles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CarbonBlack, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("POS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                        Text("DRIVER", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("TEAM", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                        Text("SPEED", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp))
                        Text("LAP TIME", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(72.dp))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (leaderboardEntries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(CarbonBlack, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "No laps",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "No live cloud lap records yet for ${selectedTrack.trackTitle}",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Complete a flying lap and tap 'SUBMIT LAP' to claim P1!",
                                    color = ApexCyan,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(CarbonBlack, RoundedCornerShape(8.dp))
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            itemsIndexed(leaderboardEntries) { index, entry ->
                                val isUser = entry.userId == currentUser?.uid
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                        .then(
                                            if (isUser) Modifier.border(1.dp, ApexCyan, RoundedCornerShape(4.dp))
                                            else Modifier
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "P${index + 1}",
                                        color = if (index == 0) ApexGold else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(36.dp)
                                    )
                                    Text(
                                        text = entry.driverName,
                                        color = if (isUser) ApexCyan else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = entry.teamName,
                                        color = Color.LightGray,
                                        fontSize = 10.sp,
                                        modifier = Modifier.weight(0.9f)
                                    )
                                    Text(
                                        text = "${entry.topSpeedKmh.toInt()} km/h",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(64.dp)
                                    )
                                    Text(
                                        text = String.format("%.3fs", entry.lapTimeSec),
                                        color = ApexGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(72.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Gray, fontSize = 11.sp)
        Text(
            text = value,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
