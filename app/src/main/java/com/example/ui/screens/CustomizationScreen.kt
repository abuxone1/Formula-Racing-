package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DriverProfile
import com.example.model.HelmetDesign
import com.example.model.LiveryTheme
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(
    profile: DriverProfile,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    var name by remember { mutableStateOf(profile.driverName) }
    var number by remember { mutableIntStateOf(profile.driverNumber) }
    var selectedHelmet by remember { mutableStateOf(profile.helmetDesign) }
    var selectedLivery by remember { mutableStateOf(profile.liveryTheme) }

    fun save() {
        profile.driverName = name
        profile.driverNumber = number
        profile.helmetDesign = selectedHelmet
        profile.liveryTheme = selectedLivery
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "DRIVER & LIVERY CUSTOMIZATION",
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            save()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("customization_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            save()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("save_customization_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = ApexCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CarbonSurface)
            )
        },
        containerColor = CarbonBlack
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Driver & Livery Badge Preview
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(selectedLivery.carbonHex)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(selectedLivery.accentHex))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Helmet Circle Avatar
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(selectedHelmet.primaryHex))
                                .border(2.dp, Color(selectedHelmet.secondaryHex), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // Visor line
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(selectedHelmet.visorTintHex))
                            )
                        }

                        Column {
                            Text(
                                text = name.ifEmpty { "Driver" }.uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${selectedLivery.title} • ${selectedHelmet.title}",
                                color = Color(selectedLivery.accentHex),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Racing Number Badge
                    Text(
                        text = "#$number",
                        color = Color(selectedLivery.primaryHex),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Driver Name & Number Config
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("DRIVER IDENTITY", color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 13.sp, fontFamily = FontFamily.Monospace)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Driver Name") },
                        modifier = Modifier.fillMaxWidth().testTag("driver_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ApexCyan,
                            unfocusedBorderColor = CarbonBorder
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Race Number (#$number)", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Slider(
                            value = number.toFloat(),
                            onValueChange = { number = it.toInt() },
                            valueRange = 1f..99f,
                            modifier = Modifier.width(220.dp),
                            colors = SliderDefaults.colors(thumbColor = ApexRed, activeTrackColor = ApexRed)
                        )
                    }
                }
            }

            // Helmet Design Selection
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("AERODYNAMIC HELMET DESIGNS", color = ApexGold, fontWeight = FontWeight.Black, fontSize = 13.sp, fontFamily = FontFamily.Monospace)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HelmetDesign.entries.forEach { design ->
                            val isSelected = (selectedHelmet == design)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(design.primaryHex).copy(alpha = 0.25f) else Color(0xFF141926))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(design.primaryHex) else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedHelmet = design }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(design.primaryHex))
                                            .border(2.dp, Color(design.secondaryHex), CircleShape)
                                    )
                                    Text(
                                        text = design.name.take(6),
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Car Livery Themes
            Card(
                colors = CardDefaults.cardColors(containerColor = CarbonSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CarbonBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("OFFICIAL CAR LIVERY THEMES", color = ApexCyan, fontWeight = FontWeight.Black, fontSize = 13.sp, fontFamily = FontFamily.Monospace)

                    LiveryTheme.entries.forEach { livery ->
                        val isSelected = (selectedLivery == livery)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(livery.primaryHex).copy(alpha = 0.25f) else Color(0xFF141824))
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) Color(livery.accentHex) else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedLivery = livery }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).background(Color(livery.primaryHex)))
                                Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)).background(Color(livery.accentHex)))
                                Text(livery.title, color = if (isSelected) Color.White else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedLivery = livery },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(livery.accentHex))
                            )
                        }
                    }
                }
            }
        }
    }
}
