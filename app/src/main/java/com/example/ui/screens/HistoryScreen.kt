package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import android.widget.Toast
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import coil.compose.AsyncImage
import com.example.data.Meal
import com.example.ui.viewmodel.NutriScanViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    viewModel: NutriScanViewModel,
    modifier: Modifier = Modifier
) {
    val mealsList by viewModel.mealsList.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val context = LocalContext.current
    var showExportProPrompt by remember { mutableStateOf(false) }
    var showCsvViewerDialog by remember { mutableStateOf(false) }
    var isSimulatingExport by remember { mutableStateOf(false) }

    val generatedCsvText = remember(mealsList) {
        val sb = StringBuilder()
        sb.append("Timestamp,FoodName,Calories,Protein(g),Carbs(g),Fat(g),Grade,IsHealthy,Ingredients\n")
        mealsList.forEach { m ->
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(m.timestamp))
            val sanitizedFood = m.foodName.replace("\"", "\"\"")
            val sanitizedIngredients = m.ingredients.replace("\"", "\"\"")
            sb.append("\"$dateStr\",\"$sanitizedFood\",${m.calories},${m.protein},${m.carbohydrates},${m.fat},\"${m.grade}\",${m.isHealthy},\"$sanitizedIngredients\"\n")
        }
        sb.toString()
    }

    val filteredMeals = mealsList.filter {
        it.foodName.lowercase().contains(searchQuery.lowercase()) ||
                it.ingredients.lowercase().contains(searchQuery.lowercase())
    }

    var activeMealToEdit by remember { mutableStateOf<Meal?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title block
        Text(
            text = "Your Meal Diary",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Dynamic metrics summary banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Logged Food Cycles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${mealsList.size} Complete Meals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = "Trend", tint = Color.White)
                }
            }
        }

        // --- 📊 PREMIUM DATA EXPORTER CARD (Monetization Point 3) ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("export_analytics_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, if (isPremium) Color(0xFF0077B6).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = "Stats",
                            tint = if (isPremium) Color(0xFF0077B6) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Advanced CSV Health Reports",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (isPremium) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0077B6).copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "PRO UNLOCKED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0077B6)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "PRO FEATURE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = "Weekly summaries and meal records compiled into CSV formats ready to share with trainers, clinicians, or fitness spreadsheets.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isSimulatingExport) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF0077B6))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compiling diary database records...", fontSize = 11.sp, color = Color(0xFF0077B6))
                    }
                } else {
                    Button(
                        onClick = {
                            if (isPremium) {
                                isSimulatingExport = true
                                viewModel.viewModelScope.launch {
                                    delay(1000)
                                    isSimulatingExport = false
                                    showCsvViewerDialog = true
                                }
                            } else {
                                showExportProPrompt = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("export_csv_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPremium) Color(0xFF0077B6) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isPremium) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Health Log (CSV)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by food name or ingredient...") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("history_search_input"),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        // List container
        if (filteredMeals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.NoFood,
                        contentDescription = "Empty list",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isEmpty()) "Your history list is empty." else "No matches found.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredMeals) { meal ->
                    HistoryItemCard(
                        meal = meal,
                        onDelete = { viewModel.deleteMeal(meal) },
                        onEdit = { activeMealToEdit = meal }
                    )
                }
            }
        }
    }

    // --- PRO POPUP PROMPT ---
    if (showExportProPrompt) {
        AlertDialog(
            onDismissRequest = { showExportProPrompt = false },
            title = { Text("Unlock Advanced Reporting", fontWeight = FontWeight.Bold, color = Color(0xFF0077B6)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Exporting structured sheets is a premium-only feature configured for NutriScan Pro members.", style = MaterialTheme.typography.bodyMedium)
                    Text("Benefits include offline file-saving formats, printable diary indexes, and precise macro logs.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportProPrompt = false
                        viewModel.navigateTo("dashboard")
                        Toast.makeText(context, "Click 'Unlock Pro' at the bottom of Home!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6))
                ) {
                    Text("Get NutriScan Pro")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportProPrompt = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- CSV PRO READER EXPORTER VIEW ---
    if (showCsvViewerDialog) {
        AlertDialog(
            onDismissRequest = { showCsvViewerDialog = false },
            title = { Text("Exported Live CSV Log", fontWeight = FontWeight.Bold, color = Color(0xFF0077B6)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Data format synthesized successfully. Live CSV text:", style = MaterialTheme.typography.bodySmall)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (mealsList.isEmpty()) "No meal logs to export." else generatedCsvText,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("NutriScan Report", generatedCsvText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "CSV copied successfully to Clipboard! Ready to paste into Excel.", Toast.LENGTH_SHORT).show()
                        showCsvViewerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6))
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvViewerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (activeMealToEdit != null) {
        val editingMeal = activeMealToEdit!!
        EditMealDialog(
            meal = editingMeal,
            onDismiss = { activeMealToEdit = null },
            onSave = { updatedMeal ->
                viewModel.updateMeal(updatedMeal)
                activeMealToEdit = null
            }
        )
    }
}

@Composable
fun HistoryItemCard(
    meal: Meal,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val dateText = remember(meal.timestamp) {
        SimpleDateFormat("EEE, MMM dd, h:mm a", Locale.getDefault()).format(Date(meal.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (meal.imagePath != null) {
                AsyncImage(
                    model = meal.imagePath,
                    contentDescription = meal.foodName,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = meal.grade,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.foodName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("P: ${meal.protein.toInt()}g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("C: ${meal.carbohydrates.toInt()}g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text("F: ${meal.fat.toInt()}g", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${meal.calories.toInt()} kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit meal log", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete meal log", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EditMealDialog(
    meal: Meal,
    onDismiss: () -> Unit,
    onSave: (Meal) -> Unit
) {
    var name by remember { mutableStateOf(meal.foodName) }
    var calories by remember { mutableStateOf(meal.calories.toString()) }
    var protein by remember { mutableStateOf(meal.protein.toString()) }
    var carbs by remember { mutableStateOf(meal.carbohydrates.toString()) }
    var fat by remember { mutableStateOf(meal.fat.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Saved Meal Log", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Food Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("Calories (kcal)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = { Text("Protein (g)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = { Text("Carbohydrates (g)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text("Fats (g)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = meal.copy(
                        foodName = name,
                        calories = calories.toDoubleOrNull() ?: meal.calories,
                        protein = protein.toDoubleOrNull() ?: meal.protein,
                        carbohydrates = carbs.toDoubleOrNull() ?: meal.carbohydrates,
                        fat = fat.toDoubleOrNull() ?: meal.fat
                    )
                    onSave(updated)
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
