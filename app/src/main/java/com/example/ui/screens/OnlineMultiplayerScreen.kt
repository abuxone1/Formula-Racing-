package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.firebase.UserProfile
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class OnlineFriend(
    val driverId: String,
    val name: String,
    val team: String,
    val isOnline: Boolean,
    val currentActivity: String
)

data class MultiplayerRoom(
    val roomId: String,
    val hostName: String,
    val trackName: String,
    val playerCount: Int,
    val maxPlayers: Int,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineMultiplayerScreen(
    authManager: GoogleAuthManager,
    repository: FirestoreRepository,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by authManager.currentUser.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Matchmaking, 1: Rooms, 2: Friends, 3: Seasons
    var isSearchingMatch by remember { mutableStateOf(false) }
    var searchTimer by remember { mutableIntStateOf(0) }
    var roomCodeInput by remember { mutableStateOf("") }
    var friendCodeInput by remember { mutableStateOf("") }

    val friendsList = remember {
        mutableStateListOf(
            OnlineFriend("apex_driver_07", "K. Raikkonen", "Scuderia Apex", true, "In Lobby"),
            OnlineFriend("speed_king_99", "M. Verstappen", "Red Bull Racing", true, "Racing Monza"),
            OnlineFriend("apex_pro_42", "L. Hamilton", "Mercedes AMG", false, "Offline")
        )
    }

    val activeRooms = remember {
        mutableStateListOf(
            MultiplayerRoom("APEX-7789", "Apex Grand Prix Host", "Monza Speed Oval", 6, 8, "Qualifying"),
            MultiplayerRoom("APEX-4412", "Veloce Racing Club", "Marina Bay City", 4, 8, "Practice"),
            MultiplayerRoom("APEX-9921", "World Championship Elite", "Grand Prix Circuit", 8, 8, "Starting Grid")
        )
    }

    // Matchmaking timer effect
    LaunchedEffect(isSearchingMatch) {
        if (isSearchingMatch) {
            searchTimer = 0
            while (isSearchingMatch) {
                delay(1000L)
                searchTimer++
                if (searchTimer >= 6) {
                    isSearchingMatch = false
                    Toast.makeText(context, "Match Found! Connecting to dedicated relay server...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ApexCyan, modifier = Modifier.size(24.dp))
                        Column {
                            Text(
                                text = "APEX GLOBAL MULTIPLEX",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "Secure Server Matchmaking • FIA Season 2026",
                                color = ApexCyan,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CarbonBlack)
            )
        },
        containerColor = CarbonBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = AsphaltGray,
                contentColor = ApexCyan
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Matchmaking", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.FlashOn, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Custom Rooms", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.MeetingRoom, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Friends", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Group, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Seasons", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Matchmaking Tab
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AsphaltGray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsMotorsports,
                                contentDescription = null,
                                tint = ApexRed,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "GLOBAL RANKED MATCHMAKING",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Queues drivers by skill bracket, telemetry response, and ping quality to ensure fair competitive racing.",
                                color = Color.LightGray,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            if (isSearchingMatch) {
                                CircularProgressIndicator(color = ApexCyan)
                                Text(
                                    text = "Searching for grid opponents... (${searchTimer}s)",
                                    color = ApexCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = { isSearchingMatch = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = ApexRed)
                                ) {
                                    Text("Cancel Search")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (currentUser == null) {
                                            Toast.makeText(context, "Please sign in with Google to join ranked matchmaking", Toast.LENGTH_SHORT).show()
                                        } else {
                                            isSearchingMatch = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ApexRed),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("start_matchmaking_button")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("FIND RANKED RACE", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Custom Rooms Tab
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = roomCodeInput,
                                onValueChange = { roomCodeInput = it },
                                label = { Text("Enter Room Code (e.g. APEX-7789)") },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ApexCyan,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedLabelColor = ApexCyan,
                                    unfocusedLabelColor = Color.Gray
                                )
                            )
                            Button(
                                onClick = {
                                    if (roomCodeInput.isNotBlank()) {
                                        Toast.makeText(context, "Joining room $roomCodeInput...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ApexCyan),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text("Join", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text("Active Private & Public Lobbies", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                            items(activeRooms) { room ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = AsphaltGray),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = room.trackName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text(text = "Host: ${room.hostName} • Code: ${room.roomId}", color = Color.LightGray, fontSize = 12.sp)
                                            Text(text = "Status: ${room.status}", color = ApexCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = { Toast.makeText(context, "Connecting to lobby ${room.roomId}...", Toast.LENGTH_SHORT).show() },
                                            colors = ButtonDefaults.buttonColors(containerColor = ApexRed)
                                        ) {
                                            Text("Join (${room.playerCount}/${room.maxPlayers})")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Friends Tab
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = friendCodeInput,
                                onValueChange = { friendCodeInput = it },
                                label = { Text("Add Friend by Driver ID") },
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (friendCodeInput.isNotBlank()) {
                                        friendsList.add(OnlineFriend(friendCodeInput, "Driver " + friendCodeInput, "Apex Racing", true, "Online"))
                                        friendCodeInput = ""
                                        Toast.makeText(context, "Friend request sent successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ApexCyan),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text("Add", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text("Apex Drivers Network", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                            items(friendsList) { friend ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = AsphaltGray),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(if (friend.isOnline) Color.Green else Color.Gray)
                                            )
                                            Column {
                                                Text(text = friend.name, color = Color.White, fontWeight = FontWeight.Bold)
                                                Text(text = "${friend.team} • ${friend.currentActivity}", color = Color.LightGray, fontSize = 12.sp)
                                            }
                                        }
                                        Button(
                                            onClick = { Toast.makeText(context, "Invite sent to ${friend.name}", Toast.LENGTH_SHORT).show() },
                                            enabled = friend.isOnline,
                                            colors = ButtonDefaults.buttonColors(containerColor = ApexRed)
                                        ) {
                                            Text("Invite")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Seasons Tab
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AsphaltGray)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = ApexGold)
                                Text("FIA WORLD CHAMPIONSHIP SEASON 2026", color = ApexGold, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                            Text("Current Standing: 3rd in Global Platinum Tier", color = Color.White, fontSize = 14.sp)
                            Text("Season Points: 1,420 PTS • Wins: 14 • Podiums: 26", color = Color.LightGray, fontSize = 12.sp)
                            LinearProgressIndicator(progress = { 0.78f }, color = ApexCyan, trackColor = CarbonBlack, modifier = Modifier.fillMaxWidth().height(8.dp))
                            Text("Season Reset in: 14 Days 06 Hours", color = ApexCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
