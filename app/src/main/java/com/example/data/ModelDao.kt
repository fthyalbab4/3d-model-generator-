package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
  @Query("SELECT * FROM models_3d ORDER BY timestamp DESC")
  fun getAllModels(): Flow<List<ModelEntity>>

  @Query("SELECT * FROM models_3d WHERE id = :id LIMIT 1")
  suspend fun getModelById(id: Long): ModelEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertModel(model: ModelEntity): Long

  @Query("DELETE FROM models_3d WHERE id = :id")
  suspend fun deleteModelById(id: Long)
}
