package com.example.ui.components

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.engine.GaussianSplatCloud
import com.example.engine.GaussianSplatNative
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Real-Time OpenGL ES 3.0 3D Gaussian Splatting Renderer.
 * Renders instanced elliptical 3D Gaussians with C++ NDK back-to-front depth sorting.
 */
class GaussianSplatRenderer : GLSurfaceView.Renderer {

  private var programId = 0
  private var quadVbo = 0
  private var instanceVbo = 0

  private val viewMatrix = FloatArray(16)
  private val projMatrix = FloatArray(16)
  private val vpMatrix = FloatArray(16)

  @Volatile var pitch: Float = 15f
  @Volatile var yaw: Float = -30f
  @Volatile var zoom: Float = 1.0f
  @Volatile var panX: Float = 0f
  @Volatile var panY: Float = 0f
  @Volatile var splatScaleMultiplier: Float = 1.35f

  private var pendingCloud: GaussianSplatCloud? = null
  private var currentCloud: GaussianSplatCloud? = null
  private var sortedIndices: IntArray = IntArray(0)
  private var instanceData: FloatArray = FloatArray(0)
  private var instanceBuffer: FloatBuffer? = null
  private var needsSort = false

  private var lastPitch = Float.NaN
  private var lastYaw = Float.NaN
  private var lastZoom = Float.NaN

  private val vertexShaderCode = """
    #version 300 es
    layout(location = 0) in vec2 aQuad;
    layout(location = 1) in vec3 aPos;
    layout(location = 2) in vec3 aScale;
    layout(location = 3) in vec3 aColor;
    layout(location = 4) in float aOpacity;

    uniform mat4 uView;
    uniform mat4 uProj;
    uniform float uScaleMult;
    uniform vec2 uViewport;

    out vec2 vUv;
    out vec3 vColor;
    out float vOpacity;

    void main() {
      vec4 camPos = uView * vec4(aPos, 1.0);
      vec4 clipCenter = uProj * camPos;

      float dist = max(0.35, -camPos.z);
      float radius = max(aScale.x, max(aScale.y, aScale.z)) * uScaleMult;
      float pxSize = (radius / dist) * uViewport.y * 1.15;
      pxSize = clamp(pxSize, 2.0, 96.0);

      vec2 ndcOffset = (aQuad * pxSize) / uViewport * 2.0;
      gl_Position = clipCenter + vec4(ndcOffset * clipCenter.w, 0.0, 0.0);

      vUv = aQuad;
      vColor = aColor;
      vOpacity = aOpacity;
    }
  """.trimIndent()

  private val fragmentShaderCode = """
    #version 300 es
    precision mediump float;

    in vec2 vUv;
    in vec3 vColor;
    in float vOpacity;

    out vec4 fragColor;

    void main() {
      float r2 = dot(vUv, vUv);
      if (r2 > 1.0) discard;
      float gauss = exp(-2.6 * r2);
      float alpha = gauss * vOpacity;
      if (alpha < 0.02) discard;
      fragColor = vec4(vColor, alpha);
    }
  """.trimIndent()

  private var viewportWidth = 1080f
  private var viewportHeight = 1920f

  fun setCloud(cloud: GaussianSplatCloud?) {
    pendingCloud = cloud
  }

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    GLES30.glClearColor(0.059f, 0.067f, 0.090f, 1.0f) // #0F1117 StudioBackground
    GLES30.glEnable(GLES30.GL_BLEND)
    GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
    GLES30.glDisable(GLES30.GL_DEPTH_TEST)

    val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
    val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)
    programId = GLES30.glCreateProgram()
    GLES30.glAttachShader(programId, vs)
    GLES30.glAttachShader(programId, fs)
    GLES30.glLinkProgram(programId)

    // Unit quad [-1, 1]
    val quadVertices = floatArrayOf(
      -1f, -1f,
       1f, -1f,
      -1f,  1f,
       1f,  1f
    )
    val quadBuf = ByteBuffer.allocateDirect(quadVertices.size * 4)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer()
      .put(quadVertices)
    quadBuf.position(0)

    val vbos = IntArray(2)
    GLES30.glGenBuffers(2, vbos, 0)
    quadVbo = vbos[0]
    instanceVbo = vbos[1]

    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVbo)
    GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quadVertices.size * 4, quadBuf, GLES30.GL_STATIC_DRAW)
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    GLES30.glViewport(0, 0, width, height)
    viewportWidth = width.toFloat().coerceAtLeast(1f)
    viewportHeight = height.toFloat().coerceAtLeast(1f)
    val aspect = viewportWidth / viewportHeight
    Matrix.perspectiveM(projMatrix, 0, 45f, aspect, 0.1f, 50f)
  }

  override fun onDrawFrame(gl: GL10?) {
    GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

    val newCloud = pendingCloud
    if (newCloud != null && newCloud !== currentCloud) {
      currentCloud = newCloud
      sortedIndices = IntArray(newCloud.count) { it }
      instanceData = FloatArray(newCloud.count * 10) // pos(3) + scale(3) + color(3) + opacity(1)
      instanceBuffer = ByteBuffer.allocateDirect(instanceData.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
      needsSort = true
    }

    val cloud = currentCloud ?: return
    if (cloud.count <= 0) return

    // Build View Matrix
    Matrix.setIdentityM(viewMatrix, 0)
    val camDist = (2.6f / zoom.coerceIn(0.25f, 5.0f))
    Matrix.translateM(viewMatrix, 0, panX * 0.002f, -panY * 0.002f, -camDist)
    Matrix.rotateM(viewMatrix, 0, pitch, 1f, 0f, 0f)
    Matrix.rotateM(viewMatrix, 0, yaw, 0f, 1f, 0f)

    if (needsSort || pitch != lastPitch || yaw != lastYaw || zoom != lastZoom) {
      lastPitch = pitch
      lastYaw = yaw
      lastZoom = zoom
      needsSort = false

      // Fast C++ NDK depth sort
      GaussianSplatNative.nativeSortGaussians(cloud.positions, cloud.count, viewMatrix, sortedIndices)

      // Pack sorted instance buffer
      for (i in 0 until cloud.count) {
        val idx = sortedIndices[i]
        val dst = i * 10
        instanceData[dst + 0] = cloud.positions[idx * 3 + 0]
        instanceData[dst + 1] = cloud.positions[idx * 3 + 1]
        instanceData[dst + 2] = cloud.positions[idx * 3 + 2]

        instanceData[dst + 3] = cloud.scales[idx * 3 + 0]
        instanceData[dst + 4] = cloud.scales[idx * 3 + 1]
        instanceData[dst + 5] = cloud.scales[idx * 3 + 2]

        instanceData[dst + 6] = cloud.colors[idx * 3 + 0]
        instanceData[dst + 7] = cloud.colors[idx * 3 + 1]
        instanceData[dst + 8] = cloud.colors[idx * 3 + 2]

        instanceData[dst + 9] = cloud.opacities[idx]
      }

      instanceBuffer?.let { buf ->
        buf.position(0)
        buf.put(instanceData)
        buf.position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, instanceData.size * 4, buf, GLES30.GL_DYNAMIC_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
      }
    }

    GLES30.glUseProgram(programId)

    val uViewLoc = GLES30.glGetUniformLocation(programId, "uView")
    val uProjLoc = GLES30.glGetUniformLocation(programId, "uProj")
    val uScaleLoc = GLES30.glGetUniformLocation(programId, "uScaleMult")
    val uViewportLoc = GLES30.glGetUniformLocation(programId, "uViewport")

    GLES30.glUniformMatrix4fv(uViewLoc, 1, false, viewMatrix, 0)
    GLES30.glUniformMatrix4fv(uProjLoc, 1, false, projMatrix, 0)
    GLES30.glUniform1f(uScaleLoc, splatScaleMultiplier)
    GLES30.glUniform2f(uViewportLoc, viewportWidth, viewportHeight)

    // Bind Quad VBO (location 0)
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadVbo)
    GLES30.glEnableVertexAttribArray(0)
    GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 8, 0)
    GLES30.glVertexAttribDivisor(0, 0)

    // Bind Instance VBO (locations 1..4)
    val strideBytes = 10 * 4
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instanceVbo)

    GLES30.glEnableVertexAttribArray(1)
    GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, strideBytes, 0)
    GLES30.glVertexAttribDivisor(1, 1)

    GLES30.glEnableVertexAttribArray(2)
    GLES30.glVertexAttribPointer(2, 3, GLES30.GL_FLOAT, false, strideBytes, 3 * 4)
    GLES30.glVertexAttribDivisor(2, 1)

    GLES30.glEnableVertexAttribArray(3)
    GLES30.glVertexAttribPointer(3, 3, GLES30.GL_FLOAT, false, strideBytes, 6 * 4)
    GLES30.glVertexAttribDivisor(3, 1)

    GLES30.glEnableVertexAttribArray(4)
    GLES30.glVertexAttribPointer(4, 1, GLES30.GL_FLOAT, false, strideBytes, 9 * 4)
    GLES30.glVertexAttribDivisor(4, 1)

    GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLE_STRIP, 0, 4, cloud.count)

    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
  }

  private fun compileShader(type: Int, code: String): Int {
    val shader = GLES30.glCreateShader(type)
    GLES30.glShaderSource(shader, code)
    GLES30.glCompileShader(shader)
    return shader
  }
}
