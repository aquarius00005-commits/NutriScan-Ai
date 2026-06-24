package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "body_metrics")
data class BodyMetric(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weightKg: Float,
    val bodyFatPercent: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface BodyMetricDao {
    @Query("SELECT * FROM body_metrics ORDER BY timestamp ASC")
    fun getAllMetricsFlow(): Flow<List<BodyMetric>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: BodyMetric): Long

    @Delete
    suspend fun deleteMetric(metric: BodyMetric)

    @Query("DELETE FROM body_metrics WHERE id = :id")
    suspend fun deleteMetricById(id: Int)

    @Query("DELETE FROM body_metrics")
    suspend fun clearAllMetrics()

    @Query("SELECT COUNT(*) FROM body_metrics")
    suspend fun getCount(): Int
}
