package com.example.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String // Base64 chunk
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: ResponseContent?
)

@JsonClass(generateAdapter = true)
data class ResponseContent(
    val parts: List<ResponsePart>?
)

@JsonClass(generateAdapter = true)
data class ResponsePart(
    val text: String?
)

// --- Our custom parsed nutrition details class ---
@JsonClass(generateAdapter = true)
data class NutritionAnalysisResult(
    val foodName: String,
    val estimatedWeight: Double,      // weight in grams
    val calories: Double,             // kcal
    val protein: Double,              // grams
    val carbs: Double,                // grams
    val fat: Double,                  // grams
    val fiber: Double,                // grams
    val sugar: Double,                // grams
    val sodium: Double,               // mg
    val potassium: Double,            // mg
    val healthScore: Int,             // 0-100
    val grade: String,                // A, B, C, D, E
    val isHealthy: Boolean,
    val dietCompatibility: List<String>, // e.g. ["Vegan", "High Protein"]
    val ingredients: List<String>,
    val recommendations: List<String>,
    val confidence: Int               // confidence percentage
)

@JsonClass(generateAdapter = true)
data class HealthyRecipe(
    val recipeName: String,
    val prepTime: String,
    val ingredients: List<String>,
    val instructions: List<String>,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val whySuited: String
)

@JsonClass(generateAdapter = true)
data class RecipeListResponse(
    val recipes: List<HealthyRecipe>
)

