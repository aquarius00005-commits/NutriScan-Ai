package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Single row constraint (only one active user profile)
    val age: Int = 28,
    val gender: String = "Male",
    val heightCm: Float = 175f,
    val weightKg: Float = 70f,
    val activityLevel: String = "Moderate",
    val fitnessGoal: String = "Maintain Weight"
) {
    // Estimations for daily limits based on Mifflin-St Jeor formula or profile preferences
    val dailyCalorieGoal: Int
        get() {
            val bmr = if (gender.lowercase() == "male") {
                10 * weightKg + 6.25 * heightCm - 5 * age + 5
            } else {
                10 * weightKg + 6.25 * heightCm - 5 * age - 161
            }
            val multiplier = when (activityLevel.lowercase()) {
                "sedentary" -> 1.2
                "light" -> 1.375
                "moderate" -> 1.55
                "active" -> 1.725
                "very active" -> 1.9
                else -> 1.55
            }
            val maintenance = (bmr * multiplier).toInt()
            return when (fitnessGoal.lowercase()) {
                "weight loss" -> maintenance - 500
                "weight gain" -> maintenance + 400
                "muscle gain" -> maintenance + 300
                else -> maintenance
            }
        }

    val dailyProteinGoalGrams: Int
        get() = (weightKg * when (fitnessGoal.lowercase()) {
            "muscle gain" -> 2.0
            "weight gain" -> 1.8
            else -> 1.5
        }).toInt()

    val dailyCarbsGoalGrams: Int
        get() = (dailyCalorieGoal * 0.5 / 4).toInt()

    val dailyFatGoalGrams: Int
        get() = (dailyCalorieGoal * 0.25 / 9).toInt()

    val dailyFiberGoalGrams: Int
        get() = if (gender.lowercase() == "male") 38 else 25
}
