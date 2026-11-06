package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.sin

object SampleModels {

  data class SampleItem(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val generator: () -> Bitmap
  )

  val samples: List<SampleItem> = listOf(
    SampleItem(
      id = "fantasy_knight",
      titleAr = "شخصية محارب أسطوري",
      titleEn = "Fantasy Knight Warrior",
      descriptionAr = "شخصية ثلاثية الأبعاد متكاملة بدرع فولاذي وخوذة وسلاح جانبي",
      descriptionEn = "Full 3D character with steel plate armor, horned helm, and battle stance",
      generator = { createWarriorCharacterBitmap() }
    ),
    SampleItem(
      id = "cyberpunk_mech",
      titleAr = "روبوت مقاتل سايبربانك",
      titleEn = "Cyberpunk Combat Mech",
      descriptionAr = "شخصية آلية مستقبلية بألواح دروع زاوية وخطوط نيون مشعة",
      descriptionEn = "Futuristic mech character with angular armor plating and neon emissive accents",
      generator = { createCyberpunkMechBitmap() }
    ),
    SampleItem(
      id = "katana_blade",
      titleAr = "سيف كاتانا خيالي أسطوري",
      titleEn = "Epic Katana Gaming Blade",
      descriptionAr = "سلاح ألعاب ناري ذو نصل مزدوج ومقبض ملفوف وحلية ذهبية",
      descriptionEn = "Stylized gaming weapon asset with polished double-edged blade and gold guard",
      generator = { createKatanaBladeBitmap() }
    ),
    SampleItem(
      id = "battle_shield",
      titleAr = "درع حربي مقاتل",
      titleEn = "Royal Combat Shield",
      descriptionAr = "درع فرسان بارز ذو حد حاد ونتوء وسطي مخصص للطباعة والاستخدام",
      descriptionEn = "Curved medieval kite shield with steel boss and embossed heraldic emblem",
      generator = { createBattleShieldBitmap() }
    ),
    SampleItem(
      id = "gold_coin",
      titleAr = "عملة فرعونية أثرية",
      titleEn = "Ancient Pharaoh Coin",
      descriptionAr = "نقش بارز عالي الدقة لعملة ذهبية بتفاصيل ثلاثية الأبعاد دقيقة",
      descriptionEn = "High-relief ancient gold medallion with intricate engraved details",
      generator = { createGoldCoinBitmap() }
    )
  )

  private fun createWarriorCharacterBitmap(): Bitmap {
    val size = 360
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    // Deep dark studio background
    canvas.drawColor(Color.rgb(15, 17, 24))

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx = size / 2f

    // 1. Torso & Armor (Steel gradient)
    paint.shader = LinearGradient(
      cx - 60f, 130f, cx + 60f, 250f,
      intArrayOf(Color.rgb(180, 195, 215), Color.rgb(90, 105, 130), Color.rgb(45, 55, 75)),
      null, Shader.TileMode.CLAMP
    )
    val torsoPath = Path().apply {
      moveTo(cx - 50f, 130f)
      lineTo(cx + 50f, 130f)
      lineTo(cx + 38f, 230f)
      lineTo(cx - 38f, 230f)
      close()
    }
    canvas.drawPath(torsoPath, paint)

    // 2. Head & Helmet with Golden Crest
    paint.shader = RadialGradient(
      cx, 85f, 45f,
      intArrayOf(Color.rgb(255, 220, 120), Color.rgb(175, 135, 45), Color.rgb(90, 70, 20)),
      null, Shader.TileMode.CLAMP
    )
    canvas.drawCircle(cx, 85f, 38f, paint)

    // Visor slit
    paint.shader = null
    paint.color = Color.rgb(20, 22, 30)
    canvas.drawRoundRect(RectF(cx - 24f, 82f, cx + 24f, 94f), 6f, 6f, paint)

    // 3. Shoulder Pauldrons
    paint.shader = LinearGradient(
      cx - 95f, 110f, cx - 45f, 160f,
      intArrayOf(Color.rgb(220, 185, 80), Color.rgb(120, 95, 30)),
      null, Shader.TileMode.CLAMP
    )
    canvas.drawRoundRect(RectF(cx - 85f, 115f, cx - 45f, 165f), 12f, 12f, paint)
    canvas.drawRoundRect(RectF(cx + 45f, 115f, cx + 85f, 165f), 12f, 12f, paint)

    // 4. Arms
    paint.shader = null
    paint.color = Color.rgb(110, 125, 145)
    canvas.drawRoundRect(RectF(cx - 80f, 165f, cx - 55f, 240f), 10f, 10f, paint)
    canvas.drawRoundRect(RectF(cx + 55f, 165f, cx + 80f, 240f), 10f, 10f, paint)

    // 5. Belt & Buckle
    paint.color = Color.rgb(220, 180, 50)
    canvas.drawRoundRect(RectF(cx - 40f, 226f, cx + 40f, 244f), 6f, 6f, paint)

    // 6. Legs (Armored greaves)
    paint.color = Color.rgb(85, 98, 120)
    canvas.drawRoundRect(RectF(cx - 36f, 244f, cx - 8f, 335f), 12f, 12f, paint)
    canvas.drawRoundRect(RectF(cx + 8f, 244f, cx + 36f, 335f), 12f, 12f, paint)

    return bmp
  }

  private fun createCyberpunkMechBitmap(): Bitmap {
    val size = 360
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    canvas.drawColor(Color.rgb(12, 14, 20))

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx = size / 2f

    // Angular Mech Body
    paint.color = Color.rgb(40, 48, 62)
    val mechChest = Path().apply {
      moveTo(cx - 65f, 120f)
      lineTo(cx + 65f, 120f)
      lineTo(cx + 45f, 235f)
      lineTo(cx - 45f, 235f)
      close()
    }
    canvas.drawPath(mechChest, paint)

    // Glowing Neon Visor / Core
    paint.color = Color.rgb(0, 245, 255)
    paint.setShadowLayer(25f, 0f, 0f, Color.rgb(0, 225, 255))
    canvas.drawRoundRect(RectF(cx - 35f, 75f, cx + 35f, 105f), 8f, 8f, paint)
    canvas.drawCircle(cx, 165f, 18f, paint)

    // Shoulder Cannons / Pauldrons
    paint.clearShadowLayer()
    paint.color = Color.rgb(80, 92, 115)
    canvas.drawRect(RectF(cx - 100f, 110f, cx - 60f, 170f), paint)
    canvas.drawRect(RectF(cx + 60f, 110f, cx + 100f, 170f), paint)

    // Mechanical Legs
    paint.color = Color.rgb(55, 65, 82)
    canvas.drawRect(RectF(cx - 42f, 235f, cx - 12f, 335f), paint)
    canvas.drawRect(RectF(cx + 12f, 235f, cx + 42f, 335f), paint)

    return bmp
  }

  private fun createKatanaBladeBitmap(): Bitmap {
    val size = 360
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(Color.rgb(14, 16, 22))

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx = size / 2f

    // 1. Blade (Steel reflection)
    paint.shader = LinearGradient(
      cx - 15f, 30f, cx + 15f, 220f,
      intArrayOf(Color.rgb(255, 255, 255), Color.rgb(205, 215, 235), Color.rgb(130, 145, 170)),
      null, Shader.TileMode.CLAMP
    )
    val bladePath = Path().apply {
      moveTo(cx, 25f)
      lineTo(cx + 14f, 60f)
      lineTo(cx + 14f, 225f)
      lineTo(cx - 14f, 225f)
      lineTo(cx - 14f, 60f)
      close()
    }
    canvas.drawPath(bladePath, paint)

    // 2. Gold Tsuba / Crossguard
    paint.shader = null
    paint.color = Color.rgb(235, 185, 45)
    canvas.drawRoundRect(RectF(cx - 45f, 222f, cx + 45f, 240f), 8f, 8f, paint)

    // 3. Tsuka / Wrapped Hilt
    paint.color = Color.rgb(180, 30, 45)
    canvas.drawRoundRect(RectF(cx - 11f, 240f, cx + 11f, 325f), 6f, 6f, paint)

    // 4. Pommel
    paint.color = Color.rgb(235, 185, 45)
    canvas.drawCircle(cx, 332f, 13f, paint)

    return bmp
  }

  private fun createBattleShieldBitmap(): Bitmap {
    val size = 360
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(Color.rgb(15, 18, 26))

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx = size / 2f
    val cy = size / 2f

    // Kite Shield Shape
    paint.shader = RadialGradient(
      cx - 30f, cy - 30f, 150f,
      intArrayOf(Color.rgb(195, 40, 50), Color.rgb(120, 20, 30), Color.rgb(55, 10, 18)),
      null, Shader.TileMode.CLAMP
    )
    val shieldPath = Path().apply {
      moveTo(cx - 95f, cy - 110f)
      lineTo(cx + 95f, cy - 110f)
      quadTo(cx + 90f, cy + 30f, cx, cy + 130f)
      quadTo(cx - 90f, cy + 30f, cx - 95f, cy - 110f)
      close()
    }
    canvas.drawPath(shieldPath, paint)

    // Steel Rim
    paint.shader = null
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 14f
    paint.color = Color.rgb(195, 205, 220)
    canvas.drawPath(shieldPath, paint)

    // Central Steel Boss
    paint.style = Paint.Style.FILL
    paint.color = Color.rgb(240, 245, 255)
    canvas.drawCircle(cx, cy - 15f, 28f, paint)

    return bmp
  }

  private fun createGoldCoinBitmap(): Bitmap {
    val size = 320
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx = size / 2f
    val cy = size / 2f
    val r = size * 0.44f

    canvas.drawColor(Color.rgb(18, 22, 32))

    val goldShader = RadialGradient(
      cx - r * 0.3f, cy - r * 0.3f, r * 1.3f,
      intArrayOf(Color.rgb(255, 230, 110), Color.rgb(212, 175, 55), Color.rgb(140, 100, 20)),
      floatArrayOf(0.0f, 0.6f, 1.0f),
      Shader.TileMode.CLAMP
    )
    paint.shader = goldShader
    canvas.drawCircle(cx, cy, r, paint)

    return bmp
  }
}
