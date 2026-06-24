package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.Meal
import com.example.data.UserProfile
import com.example.data.WaterLog
import com.example.ui.theme.*
import com.example.ui.viewmodel.NutriScanViewModel
import com.example.ui.viewmodel.RecipeUiState
import androidx.compose.ui.graphics.ColorFilter
import com.example.utils.WaterReminderHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.compose.ui.draw.scale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: NutriScanViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()
    val todayMeals by viewModel.todayMeals.collectAsState()
    val allMeals by viewModel.mealsList.collectAsState()
    val todayWaterLogs by viewModel.todayWaterLogs.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    var showPremiumUpgradeDialog by remember { mutableStateOf(false) }

    val todayActiveBurnCalories by viewModel.todayActiveBurnCalories.collectAsState()
    val todayActivitySteps by viewModel.todayActivitySteps.collectAsState()
    val syncingHealthData by viewModel.syncingHealthData.collectAsState()
    val isGoogleFitLinked by viewModel.isGoogleFitLinked.collectAsState()
    val isAppleHealthLinked by viewModel.isAppleHealthLinked.collectAsState()

    var showHealthSyncDialog by remember { mutableStateOf(false) }

    val waterGoalMl = (profile.weightKg * 35).toInt().coerceIn(2000, 4000)

    // Aggregate calorie progress
    val consumedCalories = todayMeals.sumOf { it.calories }
    val adjustedCalorieGoal = profile.dailyCalorieGoal + todayActiveBurnCalories
    val remainingCalories = (adjustedCalorieGoal - consumedCalories).coerceAtLeast(0.0)
    val consumedProtein = todayMeals.sumOf { it.protein }
    val consumedCarbs = todayMeals.sumOf { it.carbohydrates }
    val consumedFat = todayMeals.sumOf { it.fat }
    val consumedFiber = todayMeals.sumOf { it.fiber }

    val calorieTrendData = remember(allMeals) {
        val sdf = SimpleDateFormat("M/d", Locale.getDefault())
        val days = List(7) { index ->
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -6 + index)
            c
        }
        days.map { day ->
            val startOfDay = Calendar.getInstance().apply {
                timeInMillis = day.timeInMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val endOfDay = Calendar.getInstance().apply {
                timeInMillis = day.timeInMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            val dayMeals = allMeals.filter { it.timestamp in startOfDay..endOfDay }
            val totalCalories = dayMeals.sumOf { it.calories }.toFloat()
            val label = sdf.format(day.time)
            label to totalCalories
        }
    }

    val caloriePercent = if (adjustedCalorieGoal > 0) {
        (consumedCalories / adjustedCalorieGoal).coerceIn(0.0, 1.0).toFloat()
    } else 0f

    val context = LocalContext.current
    var showManualLogDialog by remember { mutableStateOf(false) }

    val lastMeal = todayMeals.lastOrNull()
    val overallScore = if (todayMeals.isNotEmpty()) todayMeals.map { it.healthScore }.average().toInt() else 88
    val overallGrade = if (todayMeals.isNotEmpty()) {
        val avgScore = todayMeals.map { it.healthScore }.average()
        if (avgScore >= 80) "A" else if (avgScore >= 65) "B" else if (avgScore >= 50) "C" else "D"
    } else "A"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- High Density Sleek Brand Greeting Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "NutriScan ",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenDeep
                    )
                    Text(
                        text = "AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenMedium
                    )
                }
                Text(
                    text = "SCANNER ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    letterSpacing = 2.sp
                )
            }
            // Dynamic avatar ring with organic green gradient
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ForestGreenSolid, NutriGreen)
                        )
                    )
                    .clickable { viewModel.navigateTo("profile") }
            )
        }

        // --- ⌚ APPLE HEALTH & GOOGLE FIT SYNC BANNER ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("sync_banner_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showHealthSyncDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = "Sync Health",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Activity Sync Center",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isGoogleFitLinked) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Google Fit Selected",
                                    tint = ForestGreenSolid,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Google Fit Linked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            if (isAppleHealthLinked) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Apple Health Selected",
                                    tint = ForestGreenSolid,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Apple HealthKit Linked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            if (!isGoogleFitLinked && !isAppleHealthLinked) {
                                Text(
                                    text = "Tap to link Apple Health / Google Fit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                
                if (todayActiveBurnCalories > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "+${todayActiveBurnCalories.toInt()} kcal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = ForestGreenSolid
                        )
                        Text(
                            text = "${todayActivitySteps} steps",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Configure Health Sync",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // --- High Density Image Scanner Card ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(32.dp))
                .border(4.dp, Color.White, RoundedCornerShape(32.dp))
                .clickable { viewModel.navigateTo("scanner") },
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.scanner_onboarding),
                    contentDescription = "AI Scanner Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Dark overlay gradient for stylish premium legibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 100f
                            )
                        )
                )

                // Laser Scanning line animation emulation overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(NutriGreen, ForestGreenDeep, NutriGreen)
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (lastMeal != null) "${lastMeal.foodName} detected" else "Ready to analyze",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "AI Food Scanner",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(ForestGreenSolid, NutriGreen)
                                )
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Open Scanner",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // --- High Density Quick Stats Grid ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickStatCard(
                label = "CALS",
                value = "${consumedCalories.toInt()}",
                modifier = Modifier.weight(1f)
            )
            QuickStatCard(
                label = "PROT",
                value = "${consumedProtein.toInt()}g",
                valueColor = ForestGreenSolid,
                modifier = Modifier.weight(1f)
            )
            QuickStatCard(
                label = "CARB",
                value = "${consumedCarbs.toInt()}g",
                modifier = Modifier.weight(1f)
            )
            QuickStatCard(
                label = "FAT",
                value = "${consumedFat.toInt()}g",
                modifier = Modifier.weight(1f)
            )
        }

        // --- High Density Health Standing Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = ForestGreenDeep),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ForestGreenMedium)
                            .border(4.dp, ForestGreenSolid, CircleShape)
                    ) {
                        Text(
                            text = overallScore.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = (2).dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "GRADE $overallGrade",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenDeep
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Dietary Standing",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (todayMeals.isNotEmpty()) {
                                "High quality food choices today."
                            } else {
                                "Build your metabolic profile by scanning meals."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = NutriGreenLight.copy(alpha = 0.9f)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NutriGreen)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "ORGANIC",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenDeep
                    )
                    Text(
                        text = "BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenDeep
                    )
                }
            }
        }

        // --- Organic AI Suggestions Tip Bar ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NutriGreenAccent)
                .border(1.dp, NutriGreenLight, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💡", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI Suggestion:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenDeep
                    )
                    Text(
                        text = if (lastMeal != null && lastMeal.recommendations.isNotEmpty()) {
                            lastMeal.recommendations
                        } else {
                            "Swap processed dressing for organic lemon and olive oil to save kcal."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestGreenDeep.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // --- High Density Daily Calories Progress Section ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Daily Calories Progress",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${consumedCalories.toInt()} / ${adjustedCalorieGoal.toInt()} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(caloriePercent)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(NutriGreen, ForestGreenDeep)
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (remainingCalories > 0) "${remainingCalories.toInt()} kcal left in budget" else "Goal achieved!",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (remainingCalories > 0) MaterialTheme.colorScheme.primary else ActiveAmber
                    )
                    Text(
                        text = "${(caloriePercent * 100).toInt()}% Done",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // --- 📊 Caloric Consumption Trends (Past 7 Days) Section ---
        Text(
            text = "Caloric Consumption Trends",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            modifier = Modifier.fillMaxWidth().testTag("caloric_trends_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ForestGreenSolid.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = "Calorie Chart Icon",
                                tint = ForestGreenSolid,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Weekly Caloric Line Chart",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Past 7 days energy intake trends",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    val avgCalories = remember(calorieTrendData) {
                        if (calorieTrendData.isNotEmpty()) calorieTrendData.map { it.second }.average().toInt() else 0
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$avgCalories kcal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = ForestGreenSolid
                        )
                        Text(
                            text = "Daily Avg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    CalorieTrendLineChart(
                        data = calorieTrendData,
                        targetVal = adjustedCalorieGoal.toFloat(),
                        mainColor = ForestGreenSolid,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // --- High Density Water Intake Tracking Widget ---
        WaterIntakeWidget(
            todayWaterLogs = todayWaterLogs,
            waterGoalMl = waterGoalMl,
            onAddWater = { amount -> viewModel.logWaterIntake(amount) },
            onDeleteWater = { logId -> viewModel.removeWaterLogById(logId) },
            onClearWater = { viewModel.clearAllWaterToday() }
        )

        // --- Macronutrients Split Progress Bars ---
        Text(
            text = "Daily Macronutrients Breakdown",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Protein bar
                MacroProgressRow(
                    label = "Protein",
                    current = consumedProtein,
                    goal = profile.dailyProteinGoalGrams.toDouble(),
                    unit = "g",
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.Egg
                )

                // Carbs bar
                MacroProgressRow(
                    label = "Carbohydrates",
                    current = consumedCarbs,
                    goal = profile.dailyCarbsGoalGrams.toDouble(),
                    unit = "g",
                    color = MaterialTheme.colorScheme.secondary,
                    icon = Icons.Default.RiceBowl
                )

                // Fat bar
                MacroProgressRow(
                    label = "Fats",
                    current = consumedFat,
                    goal = profile.dailyFatGoalGrams.toDouble(),
                    unit = "g",
                    color = MaterialTheme.colorScheme.error,
                    icon = Icons.Default.WaterDrop
                )

                // Fiber bar
                MacroProgressRow(
                    label = "Fiber",
                    current = consumedFiber,
                    goal = profile.dailyFiberGoalGrams.toDouble(),
                    unit = "g",
                    color = Color(0xFFC084FC),
                    icon = Icons.Default.Forest
                )
            }
        }

        // --- 💡 AI RECIPE RECOMMENDER WIDGET (Based on daily macro gaps) ---
        val recipeUiState by viewModel.recipeUiState.collectAsState()
        
        Text(
            text = "Gemini AI Deficiency Meals Suggestion",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Card(
            modifier = Modifier.fillMaxWidth().testTag("ai_recipes_recommender_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Personalized healthy recipes recommendation mapped specifically by Gemini artificial intelligence to perfectly cover your current tracked calorie and macronutrient deficiency gaps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Button(
                    onClick = {
                        viewModel.recommendRecipesBasedOnDeficiencies(
                            targetCalories = adjustedCalorieGoal,
                            consumedCalories = consumedCalories,
                            targetProtein = profile.dailyProteinGoalGrams.toDouble(),
                            consumedProtein = consumedProtein,
                            targetCarbs = profile.dailyCarbsGoalGrams.toDouble(),
                            consumedCarbs = consumedCarbs,
                            targetFat = profile.dailyFatGoalGrams.toDouble(),
                            consumedFat = consumedFat
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("suggest_deficiencies_recipes_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenDeep),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Generate Recipes", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze & Suggest Deficiencies Recipes", color = Color.White)
                }
                
                when (val state = recipeUiState) {
                    is RecipeUiState.Idle -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Click above to analyze deficiencies & generate meal recipes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    is RecipeUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = ForestGreenSolid)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Analyzing macro deficits and generating customized refuel meals with Gemini...", style = MaterialTheme.typography.bodySmall, color = ForestGreenDeep)
                            }
                        }
                    }
                    is RecipeUiState.Success -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            state.recipes.forEach { recipe ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = NutriGreenAccent.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, NutriGreenLight),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = recipe.recipeName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = ForestGreenDeep,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.White)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = recipe.prepTime,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ForestGreenSolid
                                                )
                                            }
                                        }
                                        
                                        // Macro pills
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            MacroPill(text = "${recipe.calories.toInt()} kcal", color = Color(0xFFFCD34D))
                                            MacroPill(text = "Pro ${recipe.protein.toInt()}g", color = Color(0xFF4ADE80))
                                            MacroPill(text = "Carb ${recipe.carbs.toInt()}g", color = Color(0xFF60A5FA))
                                            MacroPill(text = "Fat ${recipe.fat.toInt()}g", color = Color(0xFFF87171))
                                        }
                                        
                                        Text(
                                            text = "🎯 Why Suited: ${recipe.whySuited}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ForestGreenDeep.copy(alpha = 0.9f)
                                        )
                                        
                                        Text(
                                            text = "Ingredients:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ForestGreenDeep
                                        )
                                        Text(
                                            text = recipe.ingredients.joinToString(", "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ForestGreenDeep.copy(alpha = 0.8f)
                                        )
                                        
                                        Text(
                                            text = "Instructions:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ForestGreenDeep
                                        )
                                        recipe.instructions.forEachIndexed { idx, step ->
                                            Text(
                                                text = "${idx + 1}. $step",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = ForestGreenDeep.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is RecipeUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Authentication or network error: ${state.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // --- Daily Meals History Preview ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today's Logged Meals",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(
                onClick = { viewModel.navigateTo("history") }
            ) {
                Text("View History")
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Arrow")
            }
        }

        if (todayMeals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Fastfood,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "No meals logged today yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                todayMeals.forEach { meal ->
                    MealRecordCard(meal = meal, onDelete = { viewModel.deleteMeal(meal) })
                }
            }
        }

        // --- Quick Manual Entry Button ---
        Button(
            onClick = { showManualLogDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("manual_add_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Log Meal Manually", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }

        // --- PRO UPGRADE AND SIMULATED AD CARD AT THE BOTTOM (Monetization Point 1) ---
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("premium_status_banner")
                .clickable { showPremiumUpgradeDialog = true },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isPremium) Color(0xFF6A0DAD).copy(alpha = 0.08f) else Color(0xFFFF9F1C).copy(alpha = 0.08f)
            ),
            border = BorderStroke(1.dp, if (isPremium) Color(0xFF6A0DAD).copy(alpha = 0.4f) else Color(0xFFFF9F1C).copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isPremium) Color(0xFF6A0DAD) else Color(0xFFFF9F1C)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPremium) Icons.Default.WorkspacePremium else Icons.Default.Info,
                        contentDescription = "Subscription badge",
                        tint = Color.White
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isPremium) "NutriScan Pro Active" else "Simulated Ad: Upgrade to Pro",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPremium) Color(0xFF6A0DAD) else Color(0xFFFF9F1C)
                    )
                    Text(
                        text = if (isPremium) 
                            "Welcome to the inner circle! All ads removed, Gemini nutrition chat unlocked, reporting active." 
                            else "Remove standard bottom layout ads, activate the Gemini Coach, and unlock offline CSV reporting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showPremiumUpgradeDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPremium) Color(0xFF6A0DAD) else Color(0xFFFF9F1C)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (isPremium) "Manage" else "Unlock", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    if (showManualLogDialog) {
        ManualLoggingDialog(
            onDismiss = { showManualLogDialog = false },
            onSave = { name, cals, protein, carbs, fat, grams ->
                viewModel.logManualMeal(name, cals, protein, carbs, fat, grams)
                showManualLogDialog = false
            }
        )
    }

    if (showPremiumUpgradeDialog) {
        PremiumBillingUpgradeDialog(
            isPremium = isPremium,
            onTogglePremium = { viewModel.togglePremiumStatus() },
            onDismiss = { showPremiumUpgradeDialog = false }
        )
    }

    if (showHealthSyncDialog) {
        AlertDialog(
            onDismissRequest = { showHealthSyncDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync Info",
                        tint = ForestGreenSolid
                    )
                    Text(
                        text = "Cross-Device Health Sync",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Connect Google Fit or Apple HealthKit to automatically synchronize daily walk steps and exercise energy with NutriScan to dynamically adapt calorie tracking targets.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)
                    
                    // Google Fit Sync Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.toggleGoogleFitSync() }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                imageVector = Icons.Default.DirectionsRun,
                                contentDescription = "Google Fit logo",
                                tint = ForestGreenSolid,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text("Google Fit Integration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Sync steps & calories seamlessly", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                            }
                        }
                        Switch(
                            checked = isGoogleFitLinked,
                            onCheckedChange = { viewModel.toggleGoogleFitSync() }
                        )
                    }

                    // Apple HealthKit Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.toggleAppleHealthSync() }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Apple Health logo",
                                tint = Color(0xFFFF2D55),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text("Apple HealthKit Link", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Unified activity cloud-fetch", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                            }
                        }
                        Switch(
                            checked = isAppleHealthLinked,
                            onCheckedChange = { viewModel.toggleAppleHealthSync() }
                        )
                    }
                    
                    if (isGoogleFitLinked || isAppleHealthLinked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { viewModel.syncHealthData() },
                            modifier = Modifier.fillMaxWidth().testTag("sync_health_now_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenSolid)
                        ) {
                            if (syncingHealthData) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Synchronizing...")
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Now")
                            }
                        }
                        
                        if (todayActiveBurnCalories > 0) {
                            Text(
                                text = "Latest Sync Complete! Added +${todayActiveBurnCalories.toInt()} active calories based on ${todayActivitySteps} steps.",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenDeep,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHealthSyncDialog = false }) {
                    Text("Close", color = ForestGreenSolid)
                }
            }
        )
    }
}

@Composable
fun MacroPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.25f))
            .border(1.dp, color.copy(alpha = 0.6f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun PremiumBillingUpgradeDialog(
    isPremium: Boolean,
    onTogglePremium: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPlanAnnual by remember { mutableStateOf(true) }
    var cardNumber by remember { mutableStateOf("4242 4242 4242 1274") }
    var cardExpiry by remember { mutableStateOf("12/28") }
    var cardCvc by remember { mutableStateOf("382") }
    var isProcessingPayment by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFFFFD700)
                )
                Text(
                    text = if (isPremium) "Your Pro Membership" else "Subscribe to NutriScan Pro",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isPremium) {
                    Text(
                        text = "Thank you for supporting NutriScan! Your contributions assist us in upgrading our server-side image-recognition nodes and maintaining responsive models.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Text(
                        text = "Current Plan: ${if (selectedPlanAnnual) "Annual ($49.99/year)" else "Monthly ($9.99/month)"}",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6A0DAD)
                    )
                    Text(
                        text = "Billing cycle resets automatically. You can turn off automatic renewal or downgrade your status instantly below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Unlock standard premium capacities instantly and fund professional nutrition trackers:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Plan selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPlanAnnual = true },
                            border = BorderStroke(2.dp, if (selectedPlanAnnual) Color(0xFF6A0DAD) else Color.Transparent),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ANNUAL PLAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6A0DAD))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$49.99/yr", fontWeight = FontWeight.Bold)
                                Text("Save 60%", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPlanAnnual = false },
                            border = BorderStroke(2.dp, if (!selectedPlanAnnual) Color(0xFF6A0DAD) else Color.Transparent),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("MONTHLY PLAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$9.99/mo", fontWeight = FontWeight.Bold)
                                Text("Standard rate", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Simulated Payment form
                    Text("Secure Debit or Credit Payment", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        label = { Text("Card Number", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = cardExpiry,
                            onValueChange = { cardExpiry = it },
                            label = { Text("Expiry", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cardCvc,
                            onValueChange = { cardCvc = it },
                            label = { Text("CVC", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Text("Simulated transaction processing encrypted with SSL", fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }
        },
        confirmButton = {
            if (isPremium) {
                Button(
                    onClick = {
                        onTogglePremium()
                        Toast.makeText(context, "Pro features downgraded. Standard version active.", Toast.LENGTH_LONG).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancel Subscription")
                }
            } else {
                Button(
                    onClick = {
                        onTogglePremium()
                        Toast.makeText(context, "Payment Processed Successfully! Congratulations, Pro Member!", Toast.LENGTH_LONG).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A0DAD))
                ) {
                    Text("Confirm & Get Pro")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MacroProgressRow(
    label: String,
    current: Double,
    goal: Double,
    unit: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val progress = if (goal > 0) (current / goal).coerceIn(0.0, 1.0).toFloat() else 0f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "${current.toInt()} / ${goal.toInt()} $unit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun MealRecordCard(
    meal: Meal,
    onDelete: () -> Unit
) {
    val timeText = remember(meal.timestamp) {
        val now = Calendar.getInstance()
        val mealCal = Calendar.getInstance().apply { timeInMillis = meal.timestamp }
        val isToday = now.get(Calendar.YEAR) == mealCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == mealCal.get(Calendar.DAY_OF_YEAR)
                
        if (isToday) {
            "Today at " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(meal.timestamp))
        } else {
            SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(Date(meal.timestamp))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Meal Pic or Fallback Visual Ring
                if (meal.imagePath != null) {
                    AsyncImage(
                        model = meal.imagePath,
                        contentDescription = meal.foodName,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = meal.grade,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = meal.foodName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Estimated Weight: ${meal.estimatedWeightGrams.toInt()}g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Logged time",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text("P: ${meal.protein.toInt()}g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text("C: ${meal.carbohydrates.toInt()}g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                        Text("F: ${meal.fat.toInt()}g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${meal.calories.toInt()} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Logged Meal",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ManualLoggingDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Double, Double, Double) -> Unit
) {
    var foodName by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Meal Manually", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = { Text("Food Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("Calories (kcal)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text("Protein (g)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text("Carbs (g)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text("Fat (g)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Weight (g)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val c = calories.toDoubleOrNull() ?: 0.0
                    val p = protein.toDoubleOrNull() ?: 0.0
                    val cb = carbs.toDoubleOrNull() ?: 0.0
                    val f = fat.toDoubleOrNull() ?: 0.0
                    val w = weight.toDoubleOrNull() ?: 100.0
                    if (foodName.isNotBlank()) {
                        onSave(foodName, c, p, cb, f, w)
                    }
                }
            ) {
                Text("Save Meal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun QuickStatCard(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WaterIntakeWidget(
    todayWaterLogs: List<WaterLog>,
    waterGoalMl: Int,
    onAddWater: (Int) -> Unit,
    onDeleteWater: (Int) -> Unit,
    onClearWater: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomDialog by remember { mutableStateOf(false) }
    var customAmountText by remember { mutableStateOf("") }
    
    val totalWaterMl = todayWaterLogs.sumOf { it.amountMl }
    val progressPercent = if (waterGoalMl > 0) {
        (totalWaterMl.toFloat() / waterGoalMl).coerceIn(0f, 1f)
    } else 0f

    val hydrationBlue = Color(0xFF0077B6)
    val hydrationAqua = Color(0xFF4EA8DE)
    
    Card(
        modifier = modifier.fillMaxWidth().testTag("water_intake_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(hydrationAqua.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Hydration Icon",
                            tint = hydrationBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Daily Hydration",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Keep your metabolism running",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Text(
                    text = "${totalWaterMl} / ${waterGoalMl} mL",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = hydrationBlue
                )
            }

            // Progress Bar & Percentage Done
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressPercent)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(hydrationAqua, hydrationBlue)
                                )
                            )
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (totalWaterMl >= waterGoalMl) "Hydration goal achieved!" else "${(waterGoalMl - totalWaterMl).coerceAtLeast(0)} mL left to goal",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (totalWaterMl >= waterGoalMl) ForestGreenSolid else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(progressPercent * 100).toInt()}% Done",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = hydrationBlue
                    )
                }
            }

            // Quick Add Container Sizes Buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple("150 mL", 150, Icons.Default.LocalCafe),
                    Triple("250 mL", 250, Icons.Default.LocalDrink),
                    Triple("500 mL", 500, Icons.Default.WaterDrop),
                    Triple("750 mL", 750, Icons.Default.WaterDrop)
                ).forEach { (label, amount, icon) ->
                    Button(
                        onClick = { onAddWater(amount) },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("add_water_${amount}"),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = hydrationAqua.copy(alpha = 0.1f),
                            contentColor = hydrationBlue
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Custom plus button
                IconButton(
                    onClick = { showCustomDialog = true },
                    modifier = Modifier
                        .size(38.dp)
                        .background(hydrationBlue.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        .testTag("custom_water_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add custom volume", tint = hydrationBlue, modifier = Modifier.size(16.dp))
                }
            }

            // --- SMART REMINDERS SECTION ---
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)

            val context = LocalContext.current
            val notificationPermissionState = rememberPermissionState(
                permission = Manifest.permission.POST_NOTIFICATIONS
            )

            var remindersEnabled by remember { mutableStateOf(WaterReminderHelper.isRemindersEnabled(context)) }
            var selectedInterval by remember { mutableStateOf(WaterReminderHelper.getReminderIntervalMinutes(context)) }
            var pendingPermissionRequest by remember { mutableStateOf(false) }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Notification Icon",
                            tint = hydrationBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Smart Hydration Reminders",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (notificationPermissionState.status.isGranted) {
                                        remindersEnabled = true
                                        WaterReminderHelper.setRemindersEnabled(context, true, selectedInterval)
                                    } else {
                                        pendingPermissionRequest = true
                                        notificationPermissionState.launchPermissionRequest()
                                    }
                                } else {
                                    remindersEnabled = true
                                    WaterReminderHelper.setRemindersEnabled(context, true, selectedInterval)
                                }
                            } else {
                                remindersEnabled = false
                                WaterReminderHelper.setRemindersEnabled(context, false, selectedInterval)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = hydrationBlue,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.scale(0.8f).testTag("reminder_switch")
                    )
                }

                LaunchedEffect(notificationPermissionState.status.isGranted) {
                    if (notificationPermissionState.status.isGranted && pendingPermissionRequest) {
                        remindersEnabled = true
                        WaterReminderHelper.setRemindersEnabled(context, true, selectedInterval)
                        pendingPermissionRequest = false
                    }
                }

                if (remindersEnabled) {
                    val smartIntervalMin = ((12f * 60f) / (waterGoalMl / 250f)).toInt().coerceIn(30, 240)
                    
                    Text(
                        text = "Interval Reminder Timer:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Smart (${smartIntervalMin}m)" to smartIntervalMin,
                            "1 Hour" to 60,
                            "2 Hours" to 120,
                            "3 Hours" to 180
                        ).forEach { (label, minutes) ->
                            val isSelected = selectedInterval == minutes
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) hydrationBlue.copy(alpha = 0.15f) else Color.Transparent)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) hydrationBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedInterval = minutes
                                        WaterReminderHelper.setRemindersEnabled(context, true, minutes)
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) hydrationBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Next notification scheduled.",
                            style = MaterialTheme.typography.labelSmall,
                            color = hydrationBlue,
                            fontWeight = FontWeight.Medium
                        )
                        
                        TextButton(
                            onClick = { WaterReminderHelper.triggerInstantTestNotification(context) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp).testTag("test_push_btn")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(10.dp), tint = hydrationBlue)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Push", fontSize = 10.sp, color = hydrationBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Today's Recent Drink Log Chips
            if (todayWaterLogs.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 1.dp)
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Intake History",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Clear All",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ActiveRed,
                            modifier = Modifier
                                .clickable { onClearWater() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Horizontal list of logged chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        todayWaterLogs.forEach { log ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .clickable { onDeleteWater(log.id) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.WaterDrop,
                                        contentDescription = null,
                                        tint = hydrationBlue.copy(alpha = 0.7f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "${log.amountMl} mL",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Log Custom Water Intake", fontWeight = FontWeight.Bold, color = ForestGreenDeep) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter custom water amount in milliliters (mL):", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { customAmountText = it.filter { char -> char.isDigit() } },
                        placeholder = { Text("e.g. 350") },
                        suffix = { Text("mL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_water_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = hydrationBlue),
                    onClick = {
                        val amount = customAmountText.toIntOrNull()
                        if (amount != null && amount > 0) {
                            onAddWater(amount)
                        }
                        customAmountText = ""
                        showCustomDialog = false
                    }
                ) {
                    Text("Log Intake")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CalorieTrendLineChart(
    data: List<Pair<String, Float>>,
    targetVal: Float,
    modifier: Modifier = Modifier,
    mainColor: Color = ForestGreenSolid
) {
    var selectedIndex by remember(data) { mutableStateOf<Int?>(null) }

    BoxWithConstraints(modifier = modifier) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        val paddingLeftRight = 60f
        val paddingTopBottom = 48f
        val chartWidth = width - 2 * paddingLeftRight
        val chartHeight = height - 2 * paddingTopBottom

        // Compute min and max values for scaling
        val maxVal = (data.maxOfOrNull { it.second } ?: 0f).coerceAtLeast(targetVal).coerceAtLeast(500f) + 300f
        val minVal = 0f // Start at zero so we see total volume clearly
        val valRange = maxVal - minVal

        val points = remember(data, minVal, valRange, chartWidth, chartHeight) {
            data.mapIndexed { index, pair ->
                val x = paddingLeftRight + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * chartWidth
                val y = paddingTopBottom + chartHeight - ((pair.second - minVal) / valRange) * chartHeight
                Offset(x, y)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        var bestIndex = -1
                        var minDistance = Float.MAX_VALUE
                        points.forEachIndexed { idx, point ->
                            val dist = kotlin.math.abs(point.x - offset.x)
                            if (dist < minDistance) {
                                minDistance = dist
                                bestIndex = idx
                            }
                        }
                        if (bestIndex != -1 && minDistance < 60f) {
                            selectedIndex = if (selectedIndex == bestIndex) null else bestIndex
                        }
                    }
                }
        ) {
            // Draw horizontal grid lines and labels
            val gridLinesCount = 3
            for (i in 0..gridLinesCount) {
                val fraction = i.toFloat() / gridLinesCount
                val y = paddingTopBottom + chartHeight * fraction
                
                // Draw grid line
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.35f),
                    start = Offset(paddingLeftRight, y),
                    end = Offset(width - paddingLeftRight, y),
                    strokeWidth = 1.5f
                )
            }

            // Draw Target Reference Line
            val targetY = paddingTopBottom + chartHeight - ((targetVal - minVal) / valRange) * chartHeight
            drawLine(
                color = mainColor.copy(alpha = 0.5f),
                start = Offset(paddingLeftRight, targetY),
                end = Offset(width - paddingLeftRight, targetY),
                strokeWidth = 2.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
            )

            // Draw line connection path
            val path = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        // Smooth Bezier Curve computation
                        cubicTo(
                            (prev.x + curr.x) / 2, prev.y,
                            (prev.x + curr.x) / 2, curr.y,
                            curr.x, curr.y
                        )
                    }
                }
            }

            // Draw area gradient fill underneath
            val areaPath = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points.first().x, paddingTopBottom + chartHeight)
                    lineTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        cubicTo(
                            (prev.x + curr.x) / 2, prev.y,
                            (prev.x + curr.x) / 2, curr.y,
                            curr.x, curr.y
                        )
                    }
                    lineTo(points.last().x, paddingTopBottom + chartHeight)
                    close()
                }
            }

            if (points.isNotEmpty()) {
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(mainColor.copy(alpha = 0.22f), Color.Transparent),
                        startY = paddingTopBottom,
                        endY = paddingTopBottom + chartHeight
                    )
                )

                drawPath(
                    path = path,
                    color = mainColor,
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Draw actual coordinate circles
            points.forEachIndexed { idx, point ->
                val isSelected = selectedIndex == idx
                drawCircle(
                    color = if (isSelected) Color.White else mainColor,
                    radius = if (isSelected) 7.dp.toPx() else 4.dp.toPx(),
                    center = point
                )
                if (isSelected) {
                    drawCircle(
                        color = mainColor,
                        radius = 7.dp.toPx(),
                        center = point,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // Selected Tooltip Bubble Overlay UI
        selectedIndex?.let { idx ->
            val point = points[idx]
            val value = data[idx].second
            val dateLabel = data[idx].first

            val tooltipOffset = remember(point, width) {
                val offsetLeft = (point.x - 55.dp.value)
                Offset(offsetLeft.coerceIn(8f, width - 110.dp.value - 8f), (point.y - 52.dp.value).coerceAtLeast(4f))
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = mainColor),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .offset(x = tooltipOffset.x.dp, y = tooltipOffset.y.dp)
                    .width(110.dp)
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 9.sp
                    )
                    Text(
                        text = "${value.toInt()} kcal",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
        
        // Always visible labels at the bottom for dates
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = (paddingLeftRight / 3).dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { pair ->
                Text(
                    text = pair.first,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        
        // Draw Target Limit Text Indicator in the upper-right area
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 4.dp)
        ) {
            Text(
                text = "Target: ${targetVal.toInt()} kcal",
                style = MaterialTheme.typography.labelSmall,
                color = mainColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

