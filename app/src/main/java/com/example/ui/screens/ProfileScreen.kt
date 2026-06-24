package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfile
import com.example.ui.viewmodel.NutriScanViewModel
import com.example.ui.viewmodel.AiCoachState

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: NutriScanViewModel,
    modifier: Modifier = Modifier
) {
    val activeProfile by viewModel.userProfile.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val coachState by viewModel.aiCoachState.collectAsState()

    // Screen tab selection state
    var selectedTabIsCoach by remember { mutableStateOf(true) }

    // Coach Form state
    var customUserMessage by remember { mutableStateOf("") }

    // Form states
    var age by remember(activeProfile) { mutableStateOf(activeProfile.age.toString()) }
    var height by remember(activeProfile) { mutableStateOf(activeProfile.heightCm.toInt().toString()) }
    var weight by remember(activeProfile) { mutableStateOf(activeProfile.weightKg.toInt().toString()) }
    
    // Dropdown choices
    var gender by remember(activeProfile) { mutableStateOf(activeProfile.gender) }
    var activityLevel by remember(activeProfile) { mutableStateOf(activeProfile.activityLevel) }
    var fitnessGoal by remember(activeProfile) { mutableStateOf(activeProfile.fitnessGoal) }

    val genders = listOf("Male", "Female", "Other")
    val activityLevels = listOf("Sedentary", "Light", "Moderate", "Active", "Very Active")
    val fitnessGoals = listOf("Weight Loss", "Weight Gain", "Maintain Weight", "Muscle Gain")

    var genderExpanded by remember { mutableStateOf(false) }
    var activityExpanded by remember { mutableStateOf(false) }
    var fitnessExpanded by remember { mutableStateOf(false) }

    // Instant/Reactive targets calculations based on dynamic input variables
    val calculatedProfile = remember(age, gender, height, weight, activityLevel, fitnessGoal) {
        UserProfile(
            age = age.toIntOrNull() ?: activeProfile.age,
            gender = gender,
            heightCm = height.toFloatOrNull() ?: activeProfile.heightCm,
            weightKg = weight.toFloatOrNull() ?: activeProfile.weightKg,
            activityLevel = activityLevel,
            fitnessGoal = fitnessGoal
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- High Level Segmented Tab Switcher ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedTabIsCoach = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTabIsCoach) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (selectedTabIsCoach) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI Coach", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = { selectedTabIsCoach = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!selectedTabIsCoach) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (!selectedTabIsCoach) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("BMR Goals", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        if (selectedTabIsCoach) {
            // =========================================================================
            // TAB 1: PREMIUM AI COACH CHAMBER
            // =========================================================================
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Personal Wellness Advisor",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Chat interactively about customized fat burning methods, meal ideas, or daily metabolic targets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isPremium) {
                    // LOCKED COACH PAYWALL DESIGN (Monetization Point 2)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF6A0DAD).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = Color(0xFF6A0DAD),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                "PRO AI COACH LOCKED",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6A0DAD),
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                "Unlock your personal nutrition expert, powered by Google Gemini, capable of giving personalized recommendations according to your unique daily calorie intakes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Button(
                                onClick = { 
                                    viewModel.togglePremiumStatus() // Activates premium instantly to allow easy feedback logging and playtesting!
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A0DAD)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Unlock Premium Chat & Ad-Free", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // UNLOCKED PREMIUM AI COACH INTERACTIVE CHAT PANEL
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFF6A0DAD).copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        ) {
                            // Active Message History bubble window
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(bottom = 10.dp)
                            ) {
                                when (coachState) {
                                    AiCoachState.Idle -> {
                                        Column(
                                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SupportAgent,
                                                contentDescription = null,
                                                tint = Color(0xFF6A0DAD).copy(alpha = 0.6f),
                                                modifier = Modifier.size(44.dp)
                                            )
                                            Text(
                                                text = "Your Coach is Ready!",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF6A0DAD)
                                            )
                                            Text(
                                                text = "Select one of the quick chips or ask an expert wellness question below to start.",
                                                style = MaterialTheme.typography.labelSmall,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    AiCoachState.Generating -> {
                                        Column(
                                            modifier = Modifier.align(Alignment.Center),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(color = Color(0xFF6A0DAD))
                                            Text(
                                                "Interpreting profiles & meals...",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF6A0DAD)
                                            )
                                        }
                                    }
                                    is AiCoachState.Success -> {
                                        val ans = (coachState as AiCoachState.Success).response
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // User visual simulation bubble
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.End)
                                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                                    .background(Color(0xFF6A0DAD).copy(alpha = 0.1f))
                                                    .padding(12.dp)
                                            ) {
                                                Text("Discuss personalized metrics suggestion", fontSize = 12.sp, color = Color(0xFF6A0DAD), fontWeight = FontWeight.SemiBold)
                                            }

                                            // Coach response bubble
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.Start)
                                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    .padding(14.dp)
                                            ) {
                                                Text(
                                                    text = ans,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    lineHeight = 18.sp
                                                )
                                            }
                                        }
                                    }
                                    is AiCoachState.Error -> {
                                        Text(
                                            text = (coachState as AiCoachState.Error).message,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.align(Alignment.Center).padding(16.dp)
                                        )
                                    }
                                }
                            }

                            // Premade Helper Chips Rows
                            var currentChipsList = listOf(
                                "Suggest recipe based on my goals",
                                "Optimize my water intake targets",
                                "How do calorie levels affect weight goals?"
                            )
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                currentChipsList.forEach { query ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFF6A0DAD).copy(alpha = 0.08f))
                                            .clickable { viewModel.askAiCoach(query) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(query, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6A0DAD))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Custom chat text line input fields
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customUserMessage,
                                    onValueChange = { customUserMessage = it },
                                    placeholder = { Text("Ask your premium coach anything...", fontSize = 11.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("coach_custom_field"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                IconButton(
                                    onClick = {
                                        if (customUserMessage.isNotBlank()) {
                                            viewModel.askAiCoach(customUserMessage)
                                            customUserMessage = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF6A0DAD))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send prompt",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // =========================================================================
            // TAB 2: ORIGINAL GOALS & MIFFLIN-ST JEOR CALCULATOR
            // =========================================================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Mifflin-St Jeor Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // --- Interactive Goals Indicators Banner ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "REACTIVE NUTRI TARGETS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Daily Calorie Budget: ${calculatedProfile.dailyCalorieGoal} kcal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Macros Limits: P: ${calculatedProfile.dailyProteinGoalGrams}g  |  C: ${calculatedProfile.dailyCarbsGoalGrams}g  |  F: ${calculatedProfile.dailyFatGoalGrams}g",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                    }
                }

                // --- Edit Form Block ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Age Field
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            label = { Text("Profile Age") },
                            leadingIcon = { Icon(Icons.Default.HourglassEmpty, contentDescription = "Age") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_age_input")
                        )

                        // Height & Weight Fields
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = height,
                                onValueChange = { height = it },
                                label = { Text("Height (cm)") },
                                leadingIcon = { Icon(Icons.Default.Height, contentDescription = "Height") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = weight,
                                onValueChange = { weight = it },
                                label = { Text("Weight (kg)") },
                                leadingIcon = { Icon(Icons.Default.MonitorWeight, contentDescription = "Weight") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Gender Select Dropdown menu box
                        ExposedDropdownMenuBox(
                            expanded = genderExpanded,
                            onExpandedChange = { genderExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = gender,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Gender Identity") },
                                leadingIcon = { Icon(Icons.Default.People, contentDescription = "Gender") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = genderExpanded,
                                onDismissRequest = { genderExpanded = false }
                            ) {
                                genders.forEach { g ->
                                    DropdownMenuItem(
                                        text = { Text(g) },
                                        onClick = {
                                            gender = g
                                            genderExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Activity level Dropdown select box
                        ExposedDropdownMenuBox(
                            expanded = activityExpanded,
                            onExpandedChange = { activityExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = activityLevel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Metabolic Activity Level") },
                                leadingIcon = { Icon(Icons.Default.DirectionsRun, contentDescription = "Activity") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = activityExpanded,
                                onDismissRequest = { activityExpanded = false }
                            ) {
                                activityLevels.forEach { a ->
                                    DropdownMenuItem(
                                        text = { Text(a) },
                                        onClick = {
                                            activityLevel = a
                                            activityExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Fitness Goal Dropdown select box
                        ExposedDropdownMenuBox(
                            expanded = fitnessExpanded,
                            onExpandedChange = { fitnessExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = fitnessGoal,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Primary Physical Goal") },
                                leadingIcon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Goal") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fitnessExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = fitnessExpanded,
                                onDismissRequest = { fitnessExpanded = false }
                            ) {
                                fitnessGoals.forEach { fg ->
                                    DropdownMenuItem(
                                        text = { Text(fg) },
                                        onClick = {
                                            fitnessGoal = fg
                                            fitnessExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Save Button trigger
                Button(
                    onClick = {
                        viewModel.updateProfile(calculatedProfile)
                        viewModel.navigateTo("dashboard")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("save_profile_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save profile")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Update Profile & Recalculate", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
