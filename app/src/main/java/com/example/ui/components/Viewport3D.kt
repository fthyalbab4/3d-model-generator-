package com.example.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.engine.Mesh3D
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.ViewportGrid
import com.example.ui.theme.ViewportWireframe
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class ShadingMode {
  TEXTURED,
  CLAY,
  WIREFRAME,
  NORMALS,
  POINT_CLOUD
}

@Composable
fun Viewport3D(
  mesh: Mesh3D?,
  shadingMode: ShadingMode,
  pitch: Float,
  yaw: Float,
  zoom: Float,
  panX: Float,
  panY: Float,
  lightAzimuth: Float,
  lightElevation: Float,
  onRotate: (deltaPitch: Float, deltaYaw: Float) -> Unit,
  onZoom: (zoomFactor: Float) -> Unit,
  onPan: (deltaX: Float, deltaY: Float) -> Unit,
  modifier: Modifier = Modifier
) {
  val transformState = rememberTransformableState { zoomChange, panChange, _ ->
    if (zoomChange != 1.0f) {
      onZoom(zoomChange)
    }
    if (panChange != Offset.Zero) {
      onPan(panChange.x, panChange.y)
    }
  }

  Box(
    modifier = modifier
      .background(StudioBackground)
      .transformable(state = transformState)
      .pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
          change.consume()
          // 1-finger drag rotates the model
          onRotate(-dragAmount.y * 0.45f, dragAmount.x * 0.45f)
        }
      }
  ) {
    val nativePaint = remember {
      AndroidPaint().apply {
        isAntiAlias = true
      }
    }
    val nativePath = remember { AndroidPath() }

    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val cx = w / 2f + panX
      val cy = h / 2f + panY

      // Draw subtle CAD grid on floor
      val gridSpacing = 40f
      var gy = (cy % gridSpacing)
      while (gy < h) {
        drawLine(
          color = ViewportGrid,
          start = Offset(0f, gy),
          end = Offset(w, gy),
          strokeWidth = 1f
        )
        gy += gridSpacing
      }
      var gx = (cx % gridSpacing)
      while (gx < w) {
        drawLine(
          color = ViewportGrid,
          start = Offset(gx, 0f),
          end = Offset(gx, h),
          strokeWidth = 1f
        )
        gx += gridSpacing
      }

      if (mesh == null || mesh.vertexCount == 0) return@Canvas

      // Trigonometric cache for rotation
      val radYaw = Math.toRadians(yaw.toDouble()).toFloat()
      val radPitch = Math.toRadians(pitch.toDouble()).toFloat()

      val cosYaw = cos(radYaw)
      val sinYaw = sin(radYaw)
      val cosPitch = cos(radPitch)
      val sinPitch = sin(radPitch)

      // Light vector in world space
      val radAzim = Math.toRadians(lightAzimuth.toDouble()).toFloat()
      val radElev = Math.toRadians(lightElevation.toDouble()).toFloat()
      val lx = cos(radElev) * sin(radAzim)
      val ly = -sin(radElev)
      val lz = cos(radElev) * cos(radAzim)

      val vCount = mesh.vertexCount
      val v = mesh.vertices
      val vn = mesh.normals
      val colors = mesh.vertexColors

      // Transform all vertices to camera space
      val projX = FloatArray(vCount)
      val projY = FloatArray(vCount)
      val projZ = FloatArray(vCount)

      val cameraDistance = 3.2f
      val focalLength = min(w, h) * 0.95f * zoom

      for (i in 0 until vCount) {
        val vi = i * 3
        val x = v[vi]
        val y = v[vi + 1]
        val z = v[vi + 2]

        // 1. Rotate Y (Yaw)
        val x1 = x * cosYaw + z * sinYaw
        val z1 = -x * sinYaw + z * cosYaw
        val y1 = y

        // 2. Rotate X (Pitch)
        val y2 = y1 * cosPitch - z1 * sinPitch
        val z2 = y1 * sinPitch + z1 * cosPitch
        val x2 = x1

        projZ[i] = z2

        // 3. Perspective projection
        val camZ = z2 + cameraDistance
        val depth = if (camZ > 0.1f) camZ else 0.1f
        val factor = focalLength / depth

        projX[i] = cx + x2 * factor
        projY[i] = cy + y2 * factor
      }

      drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas

        when (shadingMode) {
          ShadingMode.POINT_CLOUD -> {
            nativePaint.style = AndroidPaint.Style.FILL
            val ptRadius = max(1.5f, 2.5f * zoom)
            for (i in 0 until vCount) {
              val px = projX[i]
              val py = projY[i]
              if (px in -50f..(w + 50f) && py in -50f..(h + 50f)) {
                nativePaint.color = colors[i]
                nc.drawCircle(px, py, ptRadius, nativePaint)
              }
            }
          }

          ShadingMode.WIREFRAME -> {
            nativePaint.style = AndroidPaint.Style.STROKE
            nativePaint.strokeWidth = 1.2f
            nativePaint.color = AndroidColor.argb(180, 0, 229, 255)

            val ind = mesh.indices
            nativePath.reset()
            for (i in ind.indices step 3) {
              val i1 = ind[i]
              val i2 = ind[i + 1]
              val i3 = ind[i + 2]

              val x1 = projX[i1]; val y1 = projY[i1]
              val x2 = projX[i2]; val y2 = projY[i2]
              val x3 = projX[i3]; val y3 = projY[i3]

              val cross = (x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1)
              if (cross > 0f) {
                nativePath.moveTo(x1, y1)
                nativePath.lineTo(x2, y2)
                nativePath.lineTo(x3, y3)
                nativePath.close()
              }
            }
            nc.drawPath(nativePath, nativePaint)

            // Draw glowing vertex points
            nativePaint.style = AndroidPaint.Style.FILL
            nativePaint.color = AndroidColor.argb(255, 16, 185, 129)
            val step = max(1, vCount / 400)
            for (i in 0 until vCount step step) {
              nc.drawCircle(projX[i], projY[i], 2f, nativePaint)
            }
          }

          ShadingMode.TEXTURED, ShadingMode.CLAY, ShadingMode.NORMALS -> {
            // Triangle rasterization with depth sorting
            val ind = mesh.indices
            val triCount = ind.size / 3

            val triIndices = IntArray(triCount)
            val triDepths = FloatArray(triCount)
            var visibleTriangles = 0

            // 1. Back-face culling & visibility filter (culled before sorting)
            for (t in 0 until triCount) {
              val base = t * 3
              val i1 = ind[base]
              val i2 = ind[base + 1]
              val i3 = ind[base + 2]

              val x1 = projX[i1]; val y1 = projY[i1]
              val x2 = projX[i2]; val y2 = projY[i2]
              val x3 = projX[i3]; val y3 = projY[i3]

              val cross = (x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1)
              if (cross > 0f) {
                triIndices[visibleTriangles] = t
                triDepths[t] = (projZ[i1] + projZ[i2] + projZ[i3]) / 3f
                visibleTriangles++
              }
            }

            // 2. Primitive zero-allocation in-place quicksort (back-to-front)
            if (visibleTriangles > 1) {
              quickSortTriangles(triIndices, triDepths, 0, visibleTriangles - 1)
            }

            nativePaint.style = AndroidPaint.Style.FILL

            // 3. Render visible triangles
            for (idx in 0 until visibleTriangles) {
              val t = triIndices[idx]
              val base = t * 3
              val i1 = ind[base]
              val i2 = ind[base + 1]
              val i3 = ind[base + 2]

              val x1 = projX[i1]; val y1 = projY[i1]
              val x2 = projX[i2]; val y2 = projY[i2]
              val x3 = projX[i3]; val y3 = projY[i3]

              // Compute lighting from rotated normals
              val nIdx = i1 * 3
              val nx = vn[nIdx]
              val ny = vn[nIdx + 1]
              val nz = vn[nIdx + 2]

              // Rotate normal by yaw and pitch
              val nx1 = nx * cosYaw + nz * sinYaw
              val nz1 = -nx * sinYaw + nz * cosYaw
              val ny1 = ny
              val rnx = (nx1 * cosPitch)
              val rny = ny1 * cosPitch - nz1 * sinPitch
              val rnz = ny1 * sinPitch + nz1 * cosPitch

              // Lambertian dot product with light vector
              val rawDot = rnx * lx + rny * ly + rnz * lz
              val dot = max(0.18f, rawDot)

              when (shadingMode) {
                ShadingMode.CLAY -> {
                  val baseVal = (210 * dot).toInt().coerceIn(30, 255)
                  val spec = max(0f, rnz).let { it * it * it * it } * 40f
                  val finalVal = min(255, (baseVal + spec).toInt())
                  nativePaint.color = AndroidColor.rgb(finalVal, finalVal, (finalVal * 0.94f).toInt())
                }

                ShadingMode.NORMALS -> {
                  val r = (((rnx + 1f) * 0.5f) * 255f).toInt().coerceIn(0, 255)
                  val g = (((rny + 1f) * 0.5f) * 255f).toInt().coerceIn(0, 255)
                  val b = (((rnz + 1f) * 0.5f) * 255f).toInt().coerceIn(0, 255)
                  nativePaint.color = AndroidColor.rgb(r, g, b)
                }

                ShadingMode.TEXTURED -> {
                  val srcCol = colors[i1]
                  val r = (AndroidColor.red(srcCol) * (dot * 0.85f + 0.15f)).toInt().coerceIn(0, 255)
                  val g = (AndroidColor.green(srcCol) * (dot * 0.85f + 0.15f)).toInt().coerceIn(0, 255)
                  val b = (AndroidColor.blue(srcCol) * (dot * 0.85f + 0.15f)).toInt().coerceIn(0, 255)
                  nativePaint.color = AndroidColor.rgb(r, g, b)
                }
                else -> Unit
              }

              nativePath.reset()
              nativePath.moveTo(x1, y1)
              nativePath.lineTo(x2, y2)
              nativePath.lineTo(x3, y3)
              nativePath.close()
              nc.drawPath(nativePath, nativePaint)
            }
          }
        }
      }
    }
  }
}

/**
 * High-performance primitive quicksort for triangle depth sorting.
 * Operates strictly on unboxed IntArray and FloatArray with zero object allocations.
 */
private fun quickSortTriangles(indices: IntArray, depths: FloatArray, low: Int, high: Int) {
  if (low >= high) return
  val pivot = depths[indices[(low + high) ushr 1]]
  var i = low
  var j = high
  while (i <= j) {
    while (depths[indices[i]] < pivot) i++
    while (depths[indices[j]] > pivot) j--
    if (i <= j) {
      val temp = indices[i]
      indices[i] = indices[j]
      indices[j] = temp
      i++
      j--
    }
  }
  if (low < j) quickSortTriangles(indices, depths, low, j)
  if (i < high) quickSortTriangles(indices, depths, i, high)
}

