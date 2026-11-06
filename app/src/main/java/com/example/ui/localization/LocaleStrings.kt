package com.example.ui.localization

enum class AppLanguage {
  ARABIC,
  ENGLISH
}

object LocaleStrings {

  fun appTitle(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "ستوديو 3D التوليدي"
    AppLanguage.ENGLISH -> "3D Generative Studio"
  }

  fun subtitle(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "توليد مجسمات 3D كاملة 360° بالذكاء الاصطناعي المحلي"
    AppLanguage.ENGLISH -> "Real On-Device 360° Generative 3D AI Engine"
  }

  fun tabImageTo3D(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "صورة إلى 3D"
    AppLanguage.ENGLISH -> "Image to 3D"
  }

  fun tabTextTo3D(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "نص إلى 3D"
    AppLanguage.ENGLISH -> "Text to 3D"
  }

  fun promptPlaceholder(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "صف المجسم المطلوب (مثال: كأس قهوة، سيف أثري، خاتم ذهبي، سفينة فضاء)..."
    AppLanguage.ENGLISH -> "Describe 3D model (e.g. coffee mug, medieval sword, gold ring, spaceship)..."
  }

  fun generate3D(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "توليد مجسم 360°"
    AppLanguage.ENGLISH -> "Generate 360°"
  }

  fun reconstruct(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "توليد النموذج 3D"
    AppLanguage.ENGLISH -> "Reconstruct 3D"
  }

  fun modelHub(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "مركز نماذج الـ AI"
    AppLanguage.ENGLISH -> "AI Model Hub"
  }

  fun cameraCapture(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "التقاط صورة"
    AppLanguage.ENGLISH -> "Camera"
  }

  fun galleryPick(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "من المعرض"
    AppLanguage.ENGLISH -> "Gallery"
  }

  fun demoPresets(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "أمثلة جاهزة"
    AppLanguage.ENGLISH -> "Demo Samples"
  }

  fun export3d(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "تصدير المجسم"
    AppLanguage.ENGLISH -> "Export 3D"
  }

  fun myModels(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "نماذجي المحفوظة"
    AppLanguage.ENGLISH -> "Saved Models"
  }

  fun depthScale(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "شدة الحجم والبروز"
    AppLanguage.ENGLISH -> "3D Scale & Extrusion"
  }

  fun meshResolution(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "دقة شبكة Marching Cubes"
    AppLanguage.ENGLISH -> "Marching Cubes Density"
  }

  fun smoothing(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "تنعيم الأسطح"
    AppLanguage.ENGLISH -> "Surface Smoothing"
  }

  fun lightAngle(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "زاوية مصدر الإضاءة"
    AppLanguage.ENGLISH -> "Studio Light Angle"
  }

  fun resetCamera(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "إعادة ضبط الكاميرا"
    AppLanguage.ENGLISH -> "Reset View"
  }

  fun viewFront(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "أمامي"
    AppLanguage.ENGLISH -> "Front"
  }

  fun viewBack(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "خلفي"
    AppLanguage.ENGLISH -> "Back"
  }

  fun viewTop(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "علوي"
    AppLanguage.ENGLISH -> "Top"
  }

  fun viewIso(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "إيزومتري"
    AppLanguage.ENGLISH -> "Isometric"
  }

  fun shadeTextured(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "إكساء ملون"
    AppLanguage.ENGLISH -> "Textured"
  }

  fun shadeClay(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "صلصال استوديو"
    AppLanguage.ENGLISH -> "Clay Studio"
  }

  fun shadeWireframe(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "شبكة سلكية 360°"
    AppLanguage.ENGLISH -> "Wireframe 360°"
  }

  fun shadeNormals(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "خريطة الاتجاهات"
    AppLanguage.ENGLISH -> "Normals"
  }

  fun shadePoints(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "سحابة نقاط"
    AppLanguage.ENGLISH -> "Point Cloud"
  }

  fun privacyBadge(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "توليد محلي 100% داخل معالج شاومي 11T بدون إنترنت"
    AppLanguage.ENGLISH -> "100% On-Device Neural 3D Generator • Private & Offline"
  }

  fun processingStages(lang: AppLanguage, stage: Int) = when (lang) {
    AppLanguage.ARABIC -> when (stage) {
      1 -> "المرحلة 1: تحليل الدخل واستخراج الخصائص الحجمية..."
      2 -> "المرحلة 2: توليد حقل المسافات ثلاثي الأبعاد 360° (Volumetric SDF)..."
      3 -> "المرحلة 3: استخراج الأسطح المغلقة عبر خوارزمية Marching Cubes..."
      else -> "المرحلة 4: حساب المتجهات والإكساء ثلاثي الأبعاد..."
    }
    AppLanguage.ENGLISH -> when (stage) {
      1 -> "Stage 1: Extracting volumetric latent geometry..."
      2 -> "Stage 2: Synthesizing 360° Volumetric Signed Distance Field..."
      3 -> "Stage 3: Extracting closed manifold surface via Marching Cubes..."
      else -> "Stage 4: Surface normals & 360° texture coordinates..."
    }
  }

  fun exportTitle(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "تصدير المجسم ثلاثي الأبعاد 360°"
    AppLanguage.ENGLISH -> "Export 360° 3D Asset"
  }

  fun objDesc(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "ملف OBJ + خامات MTL وصورة الإكساء لبرامج Blender وMaya وUnity"
    AppLanguage.ENGLISH -> "OBJ 360° mesh + MTL material & PNG texture for Blender & engines"
  }

  fun stlDesc(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "مجسم مصمت مغلق 360° (Watertight Solid) جاهز مباشرة للطباعة 3D"
    AppLanguage.ENGLISH -> "Watertight manifold 360° solid ready for 3D slicers (Cura/Prusa)"
  }

  fun gltfDesc(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "معيار glTF 2.0 القياسي للويب والواقع المعزز (AR)"
    AppLanguage.ENGLISH -> "glTF 2.0 standard for Web3D & Android AR"
  }

  fun plyDesc(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "ملف سحابة نقاط ومضلعات ملونة لبرامج الهندسة CAD"
    AppLanguage.ENGLISH -> "Stanford PLY with per-vertex color & normal vectors"
  }

  fun shareButton(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "مشاركة وحفظ الملف"
    AppLanguage.ENGLISH -> "Share / Save to Device"
  }

  fun saveToLibrary(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "حفظ في النماذج"
    AppLanguage.ENGLISH -> "Save to Library"
  }

  fun deleteModel(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "حذف"
    AppLanguage.ENGLISH -> "Delete"
  }

  fun verticesCount(lang: AppLanguage, count: Int) = when (lang) {
    AppLanguage.ARABIC -> "النقاط: $count"
    AppLanguage.ENGLISH -> "Vertices: $count"
  }

  fun trianglesCount(lang: AppLanguage, count: Int) = when (lang) {
    AppLanguage.ARABIC -> "المثلثات: $count"
    AppLanguage.ENGLISH -> "Faces: $count"
  }

  fun inferenceTime(lang: AppLanguage, ms: Long) = when (lang) {
    AppLanguage.ARABIC -> "وقت التوليد: $ms مللي ثانية"
    AppLanguage.ENGLISH -> "Inference: ${ms}ms"
  }

  fun noModelsYet(lang: AppLanguage) = when (lang) {
    AppLanguage.ARABIC -> "لا توجد نماذج محفوظة بعد. جرب التوليد بالنص أو الصورة للبدء!"
    AppLanguage.ENGLISH -> "No saved models yet. Try Text-to-3D or Image-to-3D to start!"
  }
}
