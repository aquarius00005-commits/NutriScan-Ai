package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val foodName: String,
    val estimatedWeightGrams: Double,
    val calories: Double,
    val protein: Double,
    val carbohydrates: Double,
    val fat: Double,
    val fiber: Double,
    val sugar: Double,
    val sodiumMg: Double,
    val potassiumMg: Double,
    val healthScore: Int,
    val grade: String,
    val isHealthy: Boolean,
    val dietCompatibility: String, // Comma separated, e.g. "Vegan, Keto, High Protein"
    val ingredients: String,       // Comma separated
    val recommendations: String,   // AI Advice / healthy swaps
    val imagePath: String?,         // Nullable in case visual history lacks photo
    val timestamp: Long = System.currentTimeMillis()
)
