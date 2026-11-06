package com.example.data

import kotlinx.coroutines.flow.Flow

class ModelRepository(private val dao: ModelDao) {
  val allModels: Flow<List<ModelEntity>> = dao.getAllModels()

  suspend fun insert(model: ModelEntity): Long = dao.insertModel(model)

  suspend fun delete(id: Long) = dao.deleteModelById(id)

  suspend fun getById(id: Long): ModelEntity? = dao.getModelById(id)
}
