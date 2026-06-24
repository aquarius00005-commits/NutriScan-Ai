package com.example.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class NutritionScannerRepository {

    private val apiService = RetrofitClient.service
    private val moshi = RetrofitClient.moshiParser

    suspend fun analyzeFoodImage(bitmap: Bitmap): NutritionAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API key is not configured. Please enter a valid key in the Secrets panel.")
        }

        val base64Image = compressAndEncodeBitmap(bitmap)
        
        val systemPrompt = """
            You are NutriScan AI, an elite nutritionist AI that analyzes food using computer vision.
            Your task is to analyze the food items in the provided image.
            
            Identify:
            1. Name of the main food item or multiple items in the image.
            2. Estimated portion size and standard weight in grams.
            3. Calories (kcal), protein (g), carbohydrates (g), fat (g), fiber (g), sugar (g), sodium (mg), potassium (mg).
            4. A Health Score between 0 and 100.
            5. A letter grade (A, B, C, D, E) where A is healthiest and E is highly processed/unhealthy, along with 'isHealthy' boolean.
            6. Diet compatibility flags: Identify if it is Keto Friendly, Vegan, Vegetarian, High Protein, Diabetic Friendly, Weight Loss Friendly, Weight Gain Friendly. Only include if it meets the dietary guidelines (return a list of matches).
            7. Active ingredients list.
            8. Personalized AI suggestions, healthier swaps, or recommended improvements.
            9. A confidence level (index 0 - 100) on how certain you are of the food identification. If items are hard to see or blurry, reflect that in the confidence level.

            You MUST strictly return a standard JSON object containing the exact fields and formats described below. Do NOT output any conversational text or markdown wrappers, just raw JSON.

            JSON Schema key fields list:
            {
              "foodName": "String representing food name(s)",
              "estimatedWeight": Double,
              "calories": Double,
              "protein": Double,
              "carbs": Double,
              "fat": Double,
              "fiber": Double,
              "sugar": Double,
              "sodium": Double,
              "potassium": Double,
              "healthScore": Int,
              "grade": "String (A, B, C, D, or E)",
              "isHealthy": Boolean,
              "dietCompatibility": ["String array of friendly diets"],
              "ingredients": ["String array of prime ingredients"],
              "recommendations": ["String array of personalized visual swaps or advice"],
              "confidence": Int (percentage 0 to 100)
            }
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = "Please analyze the food in this image."),
                        Part(
                            inlineData = InlineData(
                                mimeType = "image/jpeg",
                                data = base64Image
                            )
                        )
                    )
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2
            )
        )

        try {
            // Apply system instruction as user prompt reinforcement or configuration if desired.
            // Putting the detailed prompt as the text part inside Part(text = systemPrompt) is super robust and works flawlessly.
            val combinedRequest = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = systemPrompt),
                            Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                        )
                    )
                ),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.1
                )
            )

            val rawResponse = apiService.analyzeMeal(apiKey, combinedRequest)
            val jsonText = rawResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw IllegalStateException("Empty response from AI Vision model")

            Log.d("NutritionScanner", "Raw vision response: ${jsonText.trim()}")

            val adapter = moshi.adapter(NutritionAnalysisResult::class.java)
            val result = adapter.fromJson(jsonText.trim())
                ?: throw IllegalStateException("Failed to parse nutrition JSON from Gemini response")

            result
        } catch (e: Exception) {
            Log.e("NutritionScanner", "Analysis error", e)
            throw e
        }
    }

    private fun compressAndEncodeBitmap(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Compress to 75% quality to save bandwidth and fit well within payload limits while keeping visual detail
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
