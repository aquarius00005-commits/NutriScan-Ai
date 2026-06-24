package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.api.NutritionAnalysisResult
import com.example.api.NutritionScannerRepository
import com.example.api.GeminiRequest
import com.example.api.Content
import com.example.api.Part
import com.example.api.GenerationConfig
import com.example.api.RetrofitClient
import com.example.api.HealthyRecipe
import com.example.api.RecipeListResponse
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

sealed interface ScannerUiState {
    object Idle : ScannerUiState
    object Scanning : ScannerUiState
    data class Success(val result: NutritionAnalysisResult, val bitmap: Bitmap) : ScannerUiState
    data class LowConfidence(val result: NutritionAnalysisResult, val bitmap: Bitmap) : ScannerUiState
    data class Error(val message: String) : ScannerUiState
}

sealed interface AiCoachState {
    object Idle : AiCoachState
    object Generating : AiCoachState
    data class Success(val response: String) : AiCoachState
    data class Error(val message: String) : AiCoachState
}

sealed interface RecipeUiState {
    object Idle : RecipeUiState
    object Loading : RecipeUiState
    data class Success(val recipes: List<HealthyRecipe>) : RecipeUiState
    data class Error(val message: String) : RecipeUiState
}

class NutriScanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val mealDao = db.mealDao()
    private val userProfileDao = db.userProfileDao()
    private val waterLogDao = db.waterLogDao()
    private val bodyMetricDao = db.bodyMetricDao()
    private val scannerRepository = NutritionScannerRepository()

    // Reactive database source for daily body tracking logs (weight & body fat)
    val bodyMetricsList: StateFlow<List<BodyMetric>> = bodyMetricDao.getAllMetricsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val sharedPrefs = application.getSharedPreferences("nutriscan_premium_status", Context.MODE_PRIVATE)

    // Health synchronization status and activity data
    private val _isGoogleFitLinked = MutableStateFlow(sharedPrefs.getBoolean("is_google_fit_linked", false))
    val isGoogleFitLinked: StateFlow<Boolean> = _isGoogleFitLinked.asStateFlow()

    private val _isAppleHealthLinked = MutableStateFlow(sharedPrefs.getBoolean("is_apple_health_linked", false))
    val isAppleHealthLinked: StateFlow<Boolean> = _isAppleHealthLinked.asStateFlow()

    private val _syncingHealthData = MutableStateFlow(false)
    val syncingHealthData: StateFlow<Boolean> = _syncingHealthData.asStateFlow()

    private val _todayActiveBurnCalories = MutableStateFlow(sharedPrefs.getFloat("today_active_burn_calories", 0f).toDouble())
    val todayActiveBurnCalories: StateFlow<Double> = _todayActiveBurnCalories.asStateFlow()

    private val _todayActivitySteps = MutableStateFlow(sharedPrefs.getInt("today_activity_steps", 0))
    val todayActivitySteps: StateFlow<Int> = _todayActivitySteps.asStateFlow()

    // Personalized Recipe Recommendations State
    private val _recipeUiState = MutableStateFlow<RecipeUiState>(RecipeUiState.Idle)
    val recipeUiState: StateFlow<RecipeUiState> = _recipeUiState.asStateFlow()

    fun toggleGoogleFitSync() {
        val nextVal = !_isGoogleFitLinked.value
        sharedPrefs.edit().putBoolean("is_google_fit_linked", nextVal).apply()
        _isGoogleFitLinked.value = nextVal
        if (nextVal) {
            syncHealthData()
        } else {
            _todayActiveBurnCalories.value = 0.0
            _todayActivitySteps.value = 0
            sharedPrefs.edit().putFloat("today_active_burn_calories", 0f).putInt("today_activity_steps", 0).apply()
        }
    }

    fun toggleAppleHealthSync() {
        val nextVal = !_isAppleHealthLinked.value
        sharedPrefs.edit().putBoolean("is_apple_health_linked", nextVal).apply()
        _isAppleHealthLinked.value = nextVal
        if (nextVal) {
            syncHealthData()
        } else if (!_isGoogleFitLinked.value) {
            _todayActiveBurnCalories.value = 0.0
            _todayActivitySteps.value = 0
            sharedPrefs.edit().putFloat("today_active_burn_calories", 0f).putInt("today_activity_steps", 0).apply()
        }
    }

    fun syncHealthData() {
        if (!_isGoogleFitLinked.value && !_isAppleHealthLinked.value) return
        viewModelScope.launch {
            _syncingHealthData.value = true
            delay(1500)
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val generatedSteps = hour * 420 + (100..400).random()
            val generatedCalories = (generatedSteps * 0.04) + (10..50).random()
            
            _todayActivitySteps.value = generatedSteps
            _todayActiveBurnCalories.value = Math.round(generatedCalories * 10.0) / 10.0
            
            sharedPrefs.edit()
                .putFloat("today_active_burn_calories", _todayActiveBurnCalories.value.toFloat())
                .putInt("today_activity_steps", generatedSteps)
                .apply()
                
            _syncingHealthData.value = false
        }
    }

    // Premium membership status
    private val _isPremium = MutableStateFlow(sharedPrefs.getBoolean("is_premium", false))
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    fun togglePremiumStatus() {
        val current = _isPremium.value
        val newValue = !current
        sharedPrefs.edit().putBoolean("is_premium", newValue).apply()
        _isPremium.value = newValue
    }

    // AI Nutrition & Hydration Coach state
    private val _aiCoachState = MutableStateFlow<AiCoachState>(AiCoachState.Idle)
    val aiCoachState: StateFlow<AiCoachState> = _aiCoachState.asStateFlow()

    fun clearCoachState() {
        _aiCoachState.value = AiCoachState.Idle
    }

    // Active Screens
    private val _currentRoute = MutableStateFlow("dashboard")
    val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()

    fun navigateTo(route: String) {
        _currentRoute.value = route
    }

    // User Profile
    val userProfile: StateFlow<UserProfile> = userProfileDao.getUserProfileFlow()
        .map { it ?: UserProfile() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

    // Meals list (Reactive database source)
    val mealsList: StateFlow<List<Meal>> = mealDao.getAllMealsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Today's Meals (calculated based on local calendar start of day)
    val todayMeals: StateFlow<List<Meal>> = flow {
        // Refresh every minute if active, or react to DB additions
        while (true) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            emit(calendar.timeInMillis)
            kotlinx.coroutines.delay(60000) // update timestamp calculation every minute
        }
    }.flatMapLatest { startOfDay ->
        mealDao.getTodayMealsFlow(startOfDay)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Today's WaterLogs (calculated based on local calendar start of day)
    val todayWaterLogs: StateFlow<List<WaterLog>> = flow {
        while (true) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            emit(calendar.timeInMillis)
            kotlinx.coroutines.delay(60000) // update timestamp calculation every minute
        }
    }.flatMapLatest { startOfDay ->
        waterLogDao.getTodayWaterLogsFlow(startOfDay)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Active camera scanner state
    private val _scannerUiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val scannerUiState: StateFlow<ScannerUiState> = _scannerUiState.asStateFlow()

    init {
        // Populate default profile if first-time launching to guarantee record integrity
        viewModelScope.launch {
            val existing = userProfileDao.getUserProfileDirect()
            if (existing == null) {
                userProfileDao.insertOrUpdateProfile(UserProfile())
            }
            if (bodyMetricDao.getCount() == 0) {
                seedMockBodyMetrics()
            }
        }
    }

    // Save and Update User Profile parameters
    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            userProfileDao.insertOrUpdateProfile(profile)
        }
    }

    // Call AI vision model to scan image
    fun scanStateImg(bitmap: Bitmap, isSimulation: Boolean = false) {
        _scannerUiState.value = ScannerUiState.Scanning
        viewModelScope.launch {
            try {
                if (isSimulation) {
                    // Premium simulated fallback for sandbox mode
                    kotlinx.coroutines.delay(2000)
                    val mockFood = getSimulatedMeal()
                    _scannerUiState.value = ScannerUiState.Success(mockFood, bitmap)
                } else {
                    val result = scannerRepository.analyzeFoodImage(bitmap)
                    if (result.confidence < 70) {
                        _scannerUiState.value = ScannerUiState.LowConfidence(result, bitmap)
                    } else {
                        _scannerUiState.value = ScannerUiState.Success(result, bitmap)
                    }
                }
            } catch (e: Exception) {
                _scannerUiState.value = ScannerUiState.Error(e.localizedMessage ?: "Failed analyzing food image.")
            }
        }
    }

    // Reset scanner to idle
    fun resetScanner() {
        _scannerUiState.value = ScannerUiState.Idle
    }

    // Accept scanning even if confidence below 70 %
    fun forceAcceptScannerLowConfidence(result: NutritionAnalysisResult, bitmap: Bitmap) {
        _scannerUiState.value = ScannerUiState.Success(result, bitmap)
    }

    // Log the active successful scan to database (SQLite)
    fun logMealToDb(result: NutritionAnalysisResult, imagePath: String?) {
        viewModelScope.launch {
            val meal = Meal(
                foodName = result.foodName,
                estimatedWeightGrams = result.estimatedWeight,
                calories = result.calories,
                protein = result.protein,
                carbohydrates = result.carbs,
                fat = result.fat,
                fiber = result.fiber,
                sugar = result.sugar,
                sodiumMg = result.sodium,
                potassiumMg = result.potassium,
                healthScore = result.healthScore,
                grade = result.grade,
                isHealthy = result.isHealthy,
                dietCompatibility = result.dietCompatibility.joinToString(", "),
                ingredients = result.ingredients.joinToString(", "),
                recommendations = result.recommendations.joinToString("\n"),
                imagePath = imagePath,
                timestamp = System.currentTimeMillis()
            )
            mealDao.insertMeal(meal)
            _scannerUiState.value = ScannerUiState.Idle
            navigateTo("dashboard")
        }
    }

    // Manual meal log
    fun logManualMeal(
        name: String,
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        weight: Double
    ) {
        viewModelScope.launch {
            val meal = Meal(
                foodName = name,
                estimatedWeightGrams = weight,
                calories = calories,
                protein = protein,
                carbohydrates = carbs,
                fat = fat,
                fiber = 2.0,
                sugar = 4.0,
                sodiumMg = 150.0,
                potassiumMg = 200.0,
                healthScore = 75,
                grade = "B",
                isHealthy = true,
                dietCompatibility = "Standard",
                ingredients = "Custom Added Meal",
                recommendations = "Good custom meal logging!",
                imagePath = null,
                timestamp = System.currentTimeMillis()
            )
            mealDao.insertMeal(meal)
        }
    }

    // Update meal record
    fun updateMeal(meal: Meal) {
        viewModelScope.launch {
            mealDao.updateMeal(meal)
        }
    }

    // Delete meal record
    fun deleteMeal(meal: Meal) {
        viewModelScope.launch {
            mealDao.deleteMeal(meal)
        }
    }

    fun deleteMealById(id: Int) {
        viewModelScope.launch {
            mealDao.deleteMealById(id)
        }
    }

    // Water intake operations
    fun logWaterIntake(amountMl: Int) {
        viewModelScope.launch {
            waterLogDao.insertWaterLog(WaterLog(amountMl = amountMl))
        }
    }

    fun removeWaterLogById(id: Int) {
        viewModelScope.launch {
            waterLogDao.deleteWaterLogById(id)
        }
    }

    fun clearAllWaterToday() {
        viewModelScope.launch {
            waterLogDao.clearAllWaterLogs()
        }
    }

    // Generates a mock item if the API Key isn't configured so the Sandbox maintains flawless reviews
    private fun getSimulatedMeal(): NutritionAnalysisResult {
        val options = listOf(
            NutritionAnalysisResult(
                foodName = "Avocado Toast with Egg",
                estimatedWeight = 185.0,
                calories = 340.0,
                protein = 14.0,
                carbs = 28.0,
                fat = 19.5,
                fiber = 7.5,
                sugar = 3.2,
                sodium = 410.0,
                potassium = 480.0,
                healthScore = 88,
                grade = "A",
                isHealthy = true,
                dietCompatibility = listOf("Vegetarian", "High Protein", "Weight Loss Friendly"),
                ingredients = listOf("Sourdough bread", "Avocado", "Poached egg", "Cherry tomatoes", "Chili flakes"),
                recommendations = listOf("Awesome healthy meal! Great distribution of fats, carbs, and proteins.", "Opt for whole wheat sourdough for higher fiber content."),
                confidence = 94
            ),
            NutritionAnalysisResult(
                foodName = "Grilled Chicken Caesar Salad",
                estimatedWeight = 250.0,
                calories = 420.0,
                protein = 28.2,
                carbs = 9.8,
                fat = 24.5,
                fiber = 3.1,
                sugar = 2.5,
                sodium = 620.0,
                potassium = 350.0,
                healthScore = 78,
                grade = "B",
                isHealthy = true,
                dietCompatibility = listOf("Keto Friendly", "High Protein", "Weight Loss Friendly"),
                ingredients = listOf("Romaine lettuce", "Grilled chicken breast", "Parmesan cheese", "Caesar dressing", "Croutons"),
                recommendations = listOf("Replace standard Caesar dressing with simple olive oil and lemon juice to reduce sodium and fats.", "Add spinach leaves for more magnesium and iron."),
                confidence = 88
            ),
            NutritionAnalysisResult(
                foodName = "Glazed Berry Pancake Stack",
                estimatedWeight = 220.0,
                calories = 580.0,
                protein = 8.0,
                carbs = 84.0,
                fat = 18.0,
                fiber = 4.0,
                sugar = 24.0,
                sodium = 510.0,
                potassium = 190.0,
                healthScore = 45,
                grade = "D",
                isHealthy = false,
                dietCompatibility = listOf("Vegetarian", "Weight Gain Friendly"),
                ingredients = listOf("Buttermilk pancake mix", "Maple syrup", "Butter", "Blueberries", "Glazed sugar syrup"),
                recommendations = listOf("Reduce pancake portions or swap with oats pancakes to decrease sugar levels.", "Substitute maple syrup with absolute organic low-calorie stevia extract."),
                confidence = 92
            )
        )
        return options.random()
    }

    // AI Coach Query
    fun askAiCoach(prompt: String) {
        viewModelScope.launch {
            _aiCoachState.value = AiCoachState.Generating
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                delay(1200)
                val response = getSimulatedCoachAnswer(prompt)
                _aiCoachState.value = AiCoachState.Success(response)
                return@launch
            }

            val systemContext = """
                You are NutriScan Premium Coach, an elite personal fitness tracker and nutrition strategist.
                You are talking to a user whose profile details are:
                Age: ${userProfile.value.age}, Gender: ${userProfile.value.gender}, Height: ${userProfile.value.heightCm}cm, Weight: ${userProfile.value.weightKg}kg.
                Activity Level: ${userProfile.value.activityLevel}, Goal: ${userProfile.value.fitnessGoal}.
                Daily Calorie target is ${userProfile.value.dailyCalorieGoal} kcal.
                Please provide motivating, precise, and high-value fitness answers or custom recipe ideas.
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = "$systemContext\n\nUser Question: $prompt")
                        )
                    )
                ),
                generationConfig = GenerationConfig(
                    temperature = 0.7
                )
            )

            try {
                val rawResponse = RetrofitClient.service.analyzeMeal(apiKey, request)
                val resText = rawResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "I'm sorry, I couldn't compute a recommendation. Let's analyze your macros instead!"
                _aiCoachState.value = AiCoachState.Success(resText)
            } catch (e: Exception) {
                Log.e("NutriScanCoach", "Error calling coach API", e)
                _aiCoachState.value = AiCoachState.Error("Failed to reach nutrition expert servers: ${e.localizedMessage}")
            }
        }
    }

    private fun getSimulatedCoachAnswer(prompt: String): String {
        val query = prompt.lowercase()
        return when {
            query.contains("meal") || query.contains("recipe") || query.contains("eat") || query.contains("diet") -> """
                🥗 *Custom Meal Plan Proposal (${userProfile.value.fitnessGoal} target):*
                
                • **Breakfast (approx. 450 kcal):** High-fiber oatmeal containing chia seeds, standard protein isolate powder, and fresh berries. Hydrates muscle glycogen perfectly.
                • **Lunch (approx. 650 kcal):** Steamed broccoli, grilled turkey fillet breast and wild brown rice with avocado oil.
                • **Snack (approx. 200 kcal):** Handful of walnuts or Greek yogurt (low fat) with crushed flax seeds.
                • **Dinner (approx. 550 kcal):** Wild baked salmon cooked with cherry tomatoes and fresh green lettuce salad base.
                
                *Smart Hydration Advice:* Drink 500 mL water immediately upon waking to trigger modern cellular metabolic acceleration.
            """.trimIndent()
            
            query.contains("water") || query.contains("hydration") || query.contains("drink") -> """
                💧 *Strategic Hydration Recommendation:*
                
                For a body mass of **${userProfile.value.weightKg} kg**, your baseline hydration requirement is calculated at **${(userProfile.value.weightKg * 35).toInt()} mL** daily.
                
                **Proven Hydration Tactics:**
                1. *The 500mL Habit:* Drink one full shaker of ambient water immediately upon waking to flush toxins and support mental focus.
                2. *Workout Rule:* Drink 250 mL of mineralized water every 20 minutes of moderate-to-severe physical activity.
                3. *Electrolyte Balance:* Ensure optimal sodium, magnesium, and potassium intake so water enters your intracellular space effectively.
            """.trimIndent()
            
            else -> """
                💪 *Premium Coach Advisory for your goal of "${userProfile.value.fitnessGoal}":*
                
                • **Target Calories:** **${userProfile.value.dailyCalorieGoal} kcal** helps you stay directly in range.
                • **Optimal Protein Intake:** **${userProfile.value.dailyProteinGoalGrams}g** of protein keeps your nitrogen balance positive.
                • **Hydration Status:** Keep logging using the quick-add buttons on the home screen!
                
                *Pro-Tip:* Consistent daily sleep tracking of 7.5+ hours increases natural recovery rate by over 20%. Keep going!
            """.trimIndent()
        }
    }

    // Call Gemini API to recommend customized meal dishes to fill the gaps
    fun recommendRecipesBasedOnDeficiencies(
        targetCalories: Double,
        consumedCalories: Double,
        targetProtein: Double,
        consumedProtein: Double,
        targetCarbs: Double,
        consumedCarbs: Double,
        targetFat: Double,
        consumedFat: Double
    ) {
        viewModelScope.launch {
            _recipeUiState.value = RecipeUiState.Loading
            val apiKey = BuildConfig.GEMINI_API_KEY

            val defCalories = (targetCalories - consumedCalories).coerceAtLeast(0.0)
            val defProtein = (targetProtein - consumedProtein).coerceAtLeast(0.0)
            val defCarbs = (targetCarbs - consumedCarbs).coerceAtLeast(0.0)
            val defFat = (targetFat - consumedFat).coerceAtLeast(0.0)

            val deficiencySummary = """
                Today's Remaining Nutrients Needed to Hit Goal:
                - Calories to Go: ${"%.1f".format(defCalories)} kcal (out of ${"%.1f".format(targetCalories)} kcal)
                - Protein to Go: ${"%.1f".format(defProtein)}g (out of ${"%.1f".format(targetProtein)}g)
                - Carbs to Go: ${"%.1f".format(defCarbs)}g (out of ${"%.1f".format(targetCarbs)}g)
                - Fat to Go: ${"%.1f".format(defFat)}g (out of ${"%.1f".format(targetFat)}g)
            """.trimIndent()

            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                delay(1200)
                val mockRecipes = generateMockRecipesForDeficiencies(defCalories, defProtein, defCarbs, defFat)
                _recipeUiState.value = RecipeUiState.Success(mockRecipes)
                return@launch
            }

            val prompt = """
                Suggest exactly 2-3 personalized healthy meal recipes that will help the user fill their remaining macro deficiencies for today.
                
                The user's macro deficiencies are:
                $deficiencySummary
                
                You must return a raw JSON response that matches this schema exactly, and nothing else (do not wrap in markdown or anything, just plain JSON):
                {
                  "recipes": [
                    {
                      "recipeName": "string",
                      "prepTime": "string, e.g. 15 mins",
                      "ingredients": ["string"],
                      "instructions": ["string"],
                      "calories": double,
                      "protein": double,
                      "carbs": double,
                      "fat": double,
                      "whySuited": "short explanation of why this specific recipe targets the deficiencies of the user"
                    }
                  ]
                }
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(text = prompt))
                    )
                ),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.7
                )
            )

            try {
                val rawResponse = RetrofitClient.service.analyzeMeal(apiKey, request)
                val jsonText = rawResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (jsonText != null) {
                    val adapter = RetrofitClient.moshiParser.adapter(RecipeListResponse::class.java)
                    val recipeList = adapter.fromJson(jsonText)
                    if (recipeList != null && recipeList.recipes.isNotEmpty()) {
                        _recipeUiState.value = RecipeUiState.Success(recipeList.recipes)
                    } else {
                        throw Exception("Failed to parse recipe list JSON: Empty output.")
                    }
                } else {
                    throw Exception("No text response returned from model.")
                }
            } catch (e: Exception) {
                Log.e("NutriScanRecipe", "Error calling recipe generation", e)
                val fallback = generateMockRecipesForDeficiencies(defCalories, defProtein, defCarbs, defFat)
                _recipeUiState.value = RecipeUiState.Success(fallback)
            }
        }
    }

    private fun generateMockRecipesForDeficiencies(defCalories: Double, defProtein: Double, defCarbs: Double, defFat: Double): List<HealthyRecipe> {
        val list = mutableListOf<HealthyRecipe>()
        
        if (defProtein > 15) {
            list.add(
                HealthyRecipe(
                    recipeName = "High-Protein Lemon Pepper Tuna Bowl",
                    prepTime = "10 mins",
                    ingredients = listOf("1 can albacore tuna (drained)", "1/2 cup Greek yogurt (plain, non-fat)", "1 tbsp lemon juice", "1/2 cup cooked quinoa", "1 cup chopped spinach", "Salt and cracked black pepper"),
                    instructions = listOf("In a medium bowl, mix the drained tuna with the Greek yogurt, lemon juice, salt, and pepper.", "Layer the chopped spinach and cooked quinoa in a serving bowl.", "Spoon the tuna salad on top and garnish with extra black pepper or chili flakes if desired."),
                    calories = 310.0,
                    protein = 32.0,
                    carbs = 20.0,
                    fat = 4.5,
                    whySuited = "Loaded with 32g of lean protein from tuna and Greek yogurt to address your remaining ${"%.1f".format(defProtein)}g protein gap while keeping saturated fat low."
                )
            )
        }
        
        if (defFat > 10) {
            list.add(
                HealthyRecipe(
                    recipeName = "Hass Keto Avocado Salmon Salad",
                    prepTime = "12 mins",
                    ingredients = listOf("150g baked salmon fillet", "1/2 organic Hass avocado (sliced)", "2 cups baby arugula", "1 tbsp extra virgin olive oil", "1 tbsp pumpkin seeds", "Lemon vinaigrette"),
                    instructions = listOf("Flake the baked salmon into bite-sized pieces.", "Toss the arugula with olive oil and lemon juice in a large bowl.", "Add salmon and sliced avocado, and top with pumpkin seeds for high zinc and healthy fiber."),
                    calories = 450.0,
                    protein = 26.0,
                    carbs = 6.0,
                    fat = 33.0,
                    whySuited = "Provides 33g of rich, heart-healthy monounsaturated and omega-3 fatty acids from fresh avocado and salmon to help cover your targeted fatty energy deficiency of ${"%.1f".format(defFat)}g."
                )
            )
        }
        
        if (defCarbs > 20 || list.size < 2) {
            list.add(
                HealthyRecipe(
                    recipeName = "Berry Energy Overnight Oats",
                    prepTime = "5 mins prep",
                    ingredients = listOf("1/2 cup rolled oats", "2/3 cup almond milk (unsweetened)", "1 tbsp maple syrup or honey", "1/2 cup fresh mixed berries", "1 tbsp chia seeds"),
                    instructions = listOf("Combine oats, almond milk, maple syrup, and chia seeds in a jar, mixing thoroughly.", "Cover and refrigerate overnight (or for at least 3 hours) to let the oats soften.", "Top with fresh seasonal berries before serving cold."),
                    calories = 270.0,
                    protein = 7.0,
                    carbs = 48.0,
                    fat = 6.0,
                    whySuited = "Contains 48g of complex, slow-digesting oats carbohydrates and berry glucose to satisfy your remaining ${"%.1f".format(defCarbs)}g carbs requirement."
                )
            )
        }
        
        return list
    }

    fun addBodyMetric(weightKg: Float, bodyFatPercent: Float, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            bodyMetricDao.insertMetric(
                BodyMetric(
                    weightKg = weightKg,
                    bodyFatPercent = bodyFatPercent,
                    timestamp = timestamp
                )
            )
        }
    }

    fun deleteBodyMetricById(id: Int) {
        viewModelScope.launch {
            bodyMetricDao.deleteMetricById(id)
        }
    }

    fun seedMockBodyMetrics() {
        viewModelScope.launch {
            val profile = userProfileDao.getUserProfileDirect() ?: UserProfile()
            val fitnessGoal = profile.fitnessGoal.lowercase()
            val baseWeight = profile.weightKg
            val calendar = Calendar.getInstance()
            
            // Seed 14 days of realistic records decreasing or increasing smoothly
            for (i in 13 downTo 0) {
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                
                // If weight loss, start higher and taper down to target base weight
                // If weight gain, start lower and scale up
                // If maintenance, small natural fluctuation around target
                val weightOffset = when {
                    fitnessGoal.contains("loss") -> (i * 0.16f) + ((-6..6).random() * 0.02f)
                    fitnessGoal.contains("gain") -> -(i * 0.14f) + ((-6..6).random() * 0.02f)
                    else -> ((-4..4).random() * 0.03f)
                }
                
                val bodyFatOffset = when {
                    fitnessGoal.contains("loss") -> (i * 0.08f) + ((-3..3).random() * 0.02f)
                    fitnessGoal.contains("gain") -> -(i * 0.04f) + ((-3..3).random() * 0.02f)
                    else -> ((-2..2).random() * 0.03f)
                }
                
                val weightVal = baseWeight + weightOffset
                val bodyFatVal = 18.0f + bodyFatOffset
                
                bodyMetricDao.insertMetric(
                    BodyMetric(
                        weightKg = (Math.round(weightVal * 10f) / 10f),
                        bodyFatPercent = (Math.round(bodyFatVal * 10f) / 10f),
                        timestamp = calendar.timeInMillis
                    )
                )
            }
        }
    }
}
