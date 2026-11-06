package com.example.engine

data class AIModelInfo(
  val id: String,
  val nameAr: String,
  val nameEn: String,
  val sizeMb: Int,
  val architecture: String,
  val descriptionAr: String,
  val descriptionEn: String,
  val isDownloaded: Boolean = true,
  val isSelected: Boolean = false
)

object ModelHubManager {

  val availableModels: List<AIModelInfo> = listOf(
    AIModelInfo(
      id = "neural_360_voxel",
      nameAr = "نموذج التوليد الحجمي 360° المتقدم",
      nameEn = "Neural 360° Volumetric Generator v1",
      sizeMb = 48,
      architecture = "Volumetric SDF + Marching Cubes",
      descriptionAr = "توليد مجسمات حقيقية مغلقة بزاوية 360 درجة من النصوص والصور",
      descriptionEn = "Synthesizes full 360-degree closed watertight geometry from text and images",
      isDownloaded = true,
      isSelected = true
    ),
    AIModelInfo(
      id = "shape_e_mobile",
      nameAr = "نموذج Shap-E Mobile للأشكال الهندسية",
      nameEn = "Shap-E Mobile Implicit 3D",
      sizeMb = 72,
      architecture = "Implicit Neural Field (INR)",
      descriptionAr = "توليد كائنات ثلاثية الأبعاد معقدة بدقة سطحية عالية ومضلعات مصمتة",
      descriptionEn = "Generates complex 3D objects with smooth manifold surface boundaries",
      isDownloaded = true,
      isSelected = false
    ),
    AIModelInfo(
      id = "midas_depth_pro",
      nameAr = "نموذج MiDaS لتقدير التضاريس والنقوش",
      nameEn = "MiDaS Monocular Relief Neural",
      sizeMb = 24,
      architecture = "Deep Convolutional Disparity",
      descriptionAr = "تحويل الصور إلى تضاريس ونقوش بارزة ثلاثية الأبعاد بسرعة فائقة",
      descriptionEn = "Ultra-fast relief & depth extrusion for medals, carvings and coins",
      isDownloaded = true,
      isSelected = false
    )
  )
}
