package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "models_3d")
data class ModelEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val timestamp: Long = System.currentTimeMillis(),
  val vertexCount: Int,
  val triangleCount: Int,
  val depthScale: Float,
  val shadingMode: String,
  val thumbnailBase64: String = ""
)
