package com.example.ui.components

import android.opengl.GLSurfaceView
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.engine.GaussianSplatCloud

@Composable
fun GaussianViewport(
  cloud: GaussianSplatCloud?,
  pitch: Float,
  yaw: Float,
  zoom: Float,
  panX: Float,
  panY: Float,
  splatScaleMultiplier: Float = 1.35f,
  onRotate: (Float, Float) -> Unit,
  onZoom: (Float) -> Unit,
  onPan: (Float, Float) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val renderer = remember { GaussianSplatRenderer() }

  renderer.setCloud(cloud)
  renderer.pitch = pitch
  renderer.yaw = yaw
  renderer.zoom = zoom
  renderer.panX = panX
  renderer.panY = panY
  renderer.splatScaleMultiplier = splatScaleMultiplier

  Box(
    modifier = modifier
      .fillMaxSize()
      .pointerInput(Unit) {
        detectTransformGestures { _, pan, zoomChange, _ ->
          if (zoomChange != 1f) {
            onZoom(zoomChange)
          }
          if (pan.x != 0f || pan.y != 0f) {
            onRotate(pan.y * 0.35f, pan.x * 0.35f)
          }
        }
      }
  ) {
    AndroidView(
      factory = {
        GLSurfaceView(context).apply {
          setEGLContextClientVersion(3)
          setRenderer(renderer)
          renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
      },
      modifier = Modifier.fillMaxSize()
    )
  }
}
