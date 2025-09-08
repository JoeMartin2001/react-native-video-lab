package com.videolab

import android.graphics.SurfaceTexture
import android.opengl.*
import android.opengl.Matrix
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Decoder -> SurfaceTexture(external OES) --[GL shader]--> Encoder input Surface
 *
 * Usage:
 *  - setup(encoderInputSurface)
 *  - decoder.configure(videoFormat, helper.decoderSurface, null, 0)
 *  - helper.setFilter("grayscale" | "sepia" | "invert" | "none")
 *  - helper.setRotation(0|90|180|270) // optional
 *  - helper.setFlipY(true)            // fix upside-down
 *  - on each rendered decoder frame: helper.drawFrame(ptsUs)
 */
class CodecSurfaceHelper(
  private val width: Int,
  private val height: Int
) {
  // Surface given to the decoder (backed by SurfaceTexture attached to external OES)
  lateinit var decoderSurface: Surface
    private set

  // EGL objects for rendering into encoder's input surface
  private lateinit var eglDisplay: EGLDisplay
  private lateinit var eglContext: EGLContext
  private lateinit var eglSurface: EGLSurface

  // External OES texture + SurfaceTexture we sample from
  private var oesTexId = 0
  private lateinit var surfaceTexture: SurfaceTexture
  private val stMatrix = FloatArray(16)

  // GL program/handles
  private var program = 0
  private var aPos = 0
  private var aTex = 0
  private var uTexMatrix = 0
  private var uSampler = 0
  private var uMode = 0 // 0=none, 1=grayscale, 2=sepia, 3=invert

  // Geometry
  private val vertexBuffer: FloatBuffer
  private val texBuffer: FloatBuffer

  // Filter + transforms
  private var filterMode = 1 // default grayscale
  private var rotationDeg = 0
  private var flipY = true
  private val userMatrix = FloatArray(16)
  private val finalMatrix = FloatArray(16)

  init {
    val vertexCoords = floatArrayOf(
      -1f, -1f,   1f, -1f,
      -1f,  1f,   1f,  1f
    )
    val texCoords = floatArrayOf(
      0f, 1f,   1f, 1f,
      0f, 0f,   1f, 0f
    )
    vertexBuffer = ByteBuffer.allocateDirect(vertexCoords.size * 4)
      .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(vertexCoords); position(0)
      }
    texBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
      .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(texCoords); position(0)
      }
  }

  fun setRotation(deg: Int) { rotationDeg = ((deg % 360) + 360) % 360 }
  fun setFlipY(enable: Boolean) { flipY = enable }

  fun updateDefaultBufferSize(w: Int, h: Int) {
    surfaceTexture.setDefaultBufferSize(w, h)
  }

  fun setup(encoderInputSurface: Surface) {
    // --- EGL setup (recordable config for encoder input surface)
    eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    require(eglDisplay != EGL14.EGL_NO_DISPLAY) { "No EGL display" }
    val vers = IntArray(2)
    EGL14.eglInitialize(eglDisplay, vers, 0, vers, 1)

    val EGL_RECORDABLE_ANDROID = 0x3142
    val cfgAttribs = intArrayOf(
      EGL14.EGL_RED_SIZE, 8,
      EGL14.EGL_GREEN_SIZE, 8,
      EGL14.EGL_BLUE_SIZE, 8,
      EGL14.EGL_ALPHA_SIZE, 8,
      EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
      EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
      EGL_RECORDABLE_ANDROID, 1,
      EGL14.EGL_NONE
    )
    val configs = arrayOfNulls<EGLConfig>(1)
    val num = IntArray(1)
    EGL14.eglChooseConfig(eglDisplay, cfgAttribs, 0, configs, 0, 1, num, 0)
    val config = configs[0] ?: error("No EGL config")

    val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
    eglContext = EGL14.eglCreateContext(eglDisplay, config, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
    require(eglContext != EGL14.EGL_NO_CONTEXT) { "eglCreateContext failed" }

    eglSurface = EGL14.eglCreateWindowSurface(
      eglDisplay, config, encoderInputSurface, intArrayOf(EGL14.EGL_NONE), 0
    )
    require(eglSurface != EGL14.EGL_NO_SURFACE) { "eglCreateWindowSurface failed" }

    EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

    // --- External OES texture + SurfaceTexture (decoder will render into this)
    val tex = IntArray(1)
    GLES20.glGenTextures(1, tex, 0)
    oesTexId = tex[0]
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTexId)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

    surfaceTexture = SurfaceTexture(oesTexId).apply {
      setDefaultBufferSize(width, height)
    }
    decoderSurface = Surface(surfaceTexture)

    // --- Shader program
    buildProgram()
    GLES20.glUseProgram(program)
    GLES20.glUniform1i(uSampler, 0)
  }

  fun setFilter(name: String) {
    filterMode = when (name.lowercase()) {
      "none" -> 0
      "grayscale", "mono" -> 1
      "sepia" -> 2
      "invert", "negative" -> 3
      else -> 1 // default grayscale
    }
  }

  private fun buildUserMatrix() {
    Matrix.setIdentityM(userMatrix, 0)

    // rotate/flip around center
    Matrix.translateM(userMatrix, 0, 0.5f, 0.5f, 0f)
    if (rotationDeg != 0) {
      Matrix.rotateM(userMatrix, 0, rotationDeg.toFloat(), 0f, 0f, 1f)
    }
    if (flipY) {
      Matrix.scaleM(userMatrix, 0, 1f, -1f, 1f)
    }
    Matrix.translateM(userMatrix, 0, -0.5f, -0.5f, 0f)
  }

  /**
   * Called once per decoded frame that was released with render=true.
   * presentationTimeUs = decoder output PTS.
   */
  fun drawFrame(presentationTimeUs: Long) {
    // Latch the new frame & transform from SurfaceTexture
    surfaceTexture.updateTexImage()
    surfaceTexture.getTransformMatrix(stMatrix)

    // Compose user transform (rotation/flip) with SurfaceTexture matrix:
    // final = userMatrix * stMatrix
    buildUserMatrix()
    Matrix.multiplyMM(finalMatrix, 0, userMatrix, 0, stMatrix, 0)

    GLES20.glViewport(0, 0, width, height)
    GLES20.glClearColor(0f, 0f, 0f, 1f)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

    GLES20.glUseProgram(program)

    GLES20.glEnableVertexAttribArray(aPos)
    GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

    GLES20.glEnableVertexAttribArray(aTex)
    GLES20.glVertexAttribPointer(aTex, 2, GLES20.GL_FLOAT, false, 0, texBuffer)

    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTexId)
    GLES20.glUniform1i(uSampler, 0)
    GLES20.glUniform1i(uMode, filterMode)
    GLES20.glUniformMatrix4fv(uTexMatrix, 1, false, finalMatrix, 0)

    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

    // Stamp PTS for the encoder, then present
    EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, presentationTimeUs * 1000L)
    EGL14.eglSwapBuffers(eglDisplay, eglSurface)

    GLES20.glDisableVertexAttribArray(aPos)
    GLES20.glDisableVertexAttribArray(aTex)
  }

  fun release() {
    try { decoderSurface.release() } catch (_: Throwable) {}
    try { surfaceTexture.release() } catch (_: Throwable) {}

    if (program != 0) try { GLES20.glDeleteProgram(program) } catch (_: Throwable) {}
    if (oesTexId != 0) {
      try {
        val t = intArrayOf(oesTexId)
        GLES20.glDeleteTextures(1, t, 0)
      } catch (_: Throwable) {}
    }

    try {
      EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
    } catch (_: Throwable) {}
    try { EGL14.eglDestroySurface(eglDisplay, eglSurface) } catch (_: Throwable) {}
    try { EGL14.eglDestroyContext(eglDisplay, eglContext) } catch (_: Throwable) {}
    try { EGL14.eglTerminate(eglDisplay) } catch (_: Throwable) {}
  }

  // ---------- internals ----------

  private fun buildProgram() {
    val vsh = """
      attribute vec4 aPosition;
      attribute vec2 aTexCoord;
      uniform mat4 uTexMatrix;
      varying vec2 vTexCoord;
      void main() {
        gl_Position = aPosition;
        vec4 tc = vec4(aTexCoord, 0.0, 1.0);
        tc = uTexMatrix * tc;
        vTexCoord = tc.xy;
      }
    """.trimIndent()

    val fsh = """
      #extension GL_OES_EGL_image_external : require
      #ifdef GL_FRAGMENT_PRECISION_HIGH
      precision highp float;
      #else
      precision mediump float;
      #endif

      varying vec2 vTexCoord;
      uniform samplerExternalOES sTexture;
      uniform int uMode; // 0=none, 1=grayscale, 2=sepia, 3=invert

      vec3 toGray(vec3 c) {
        float g = dot(c, vec3(0.299, 0.587, 0.114));
        return vec3(g);
      }
      vec3 toSepia(vec3 c) {
        float r = dot(c, vec3(0.393, 0.769, 0.189));
        float g = dot(c, vec3(0.349, 0.686, 0.168));
        float b = dot(c, vec3(0.272, 0.534, 0.131));
        return vec3(r, g, b);
      }

      void main() {
        vec4 color = texture2D(sTexture, vTexCoord);
        if (uMode == 1) {
          color.rgb = toGray(color.rgb);
        } else if (uMode == 2) {
          color.rgb = toSepia(color.rgb);
        } else if (uMode == 3) {
          color.rgb = 1.0 - color.rgb;  // invert
        }
        gl_FragColor = vec4(color.rgb, 1.0);
      }
    """.trimIndent()

    val vs = compile(GLES20.GL_VERTEX_SHADER, vsh)
    val fs = compile(GLES20.GL_FRAGMENT_SHADER, fsh)
    program = GLES20.glCreateProgram().also { p ->
      GLES20.glAttachShader(p, vs)
      GLES20.glAttachShader(p, fs)
      GLES20.glLinkProgram(p)
      val linkStatus = IntArray(1)
      GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, linkStatus, 0)
      require(linkStatus[0] == GLES20.GL_TRUE) {
        "Program link failed: " + GLES20.glGetProgramInfoLog(p)
      }
    }
    aPos = GLES20.glGetAttribLocation(program, "aPosition")
    aTex = GLES20.glGetAttribLocation(program, "aTexCoord")
    uTexMatrix = GLES20.glGetUniformLocation(program, "uTexMatrix")
    uSampler = GLES20.glGetUniformLocation(program, "sTexture")
    uMode = GLES20.glGetUniformLocation(program, "uMode")
  }

  private fun compile(type: Int, src: String): Int {
    val s = GLES20.glCreateShader(type)
    GLES20.glShaderSource(s, src)
    GLES20.glCompileShader(s)
    val status = IntArray(1)
    GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, status, 0)
    require(status[0] == GLES20.GL_TRUE) {
      "Shader compile failed: " + GLES20.glGetShaderInfoLog(s)
    }
    return s
  }
}
