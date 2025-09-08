package com.videolab

import android.view.Surface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaMuxer
import android.media.MediaFormat
import android.util.Log
import android.net.Uri
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.bridge.ReactMethod
import java.nio.ByteBuffer
import java.util.concurrent.Executors

@ReactModule(name = VideoLabModule.NAME)
class VideoLabModule(reactContext: ReactApplicationContext) :
  NativeVideoLabSpec(reactContext) {

  override fun getName(): String = NAME

  // Implement TS Spec methods

  override fun trim(path: String, start: Double, end: Double, promise: Promise) {
    Thread {
      val startUs = (start * 1_000_000).toLong()
      val endUs = (end * 1_000_000).toLong()

      val inputPath = if (path.startsWith("file://")) path.removePrefix("file://") else path
      val outputPath = reactApplicationContext.cacheDir.absolutePath + "/trimmed_${System.currentTimeMillis()}.mp4"

      var extractor: MediaExtractor? = null
      var muxer: MediaMuxer? = null

      try {
        extractor = MediaExtractor()
        extractor.setDataSource(inputPath)

        muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val trackCount = extractor.trackCount
        val indexMap = HashMap<Int, Int>(trackCount)

        for (i in 0 until trackCount) {
          extractor.selectTrack(i)
          val format = extractor.getTrackFormat(i)
          val dstIndex = muxer.addTrack(format)
          indexMap[i] = dstIndex
        }

        muxer.start()

        val bufferSize = 1 * 1024 * 1024
        val buffer = ByteBuffer.allocate(bufferSize)
        val bufferInfo = MediaCodec.BufferInfo()

        for (i in 0 until trackCount) {
          extractor.selectTrack(i)
          extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

          while (true) {
            bufferInfo.offset = 0
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) break

            bufferInfo.presentationTimeUs = extractor.sampleTime
            if (bufferInfo.presentationTimeUs > endUs) break

            bufferInfo.flags = extractor.sampleFlags

            val trackIndex = extractor.sampleTrackIndex
            muxer.writeSampleData(indexMap[trackIndex]!!, buffer, bufferInfo)

            extractor.advance()
          }

          extractor.unselectTrack(i)
        }

        muxer.stop()
        muxer.release()
        extractor.release()

        promise.resolve("file://$outputPath")
      } catch (e: Exception) {
        e.printStackTrace()
        try {
          muxer?.release()
          extractor?.release()
        } catch (_: Exception) {}
        promise.reject("TRIM_ERROR", "Failed to trim video", e)
      }
    }.start()
  }

  override fun merge(paths: com.facebook.react.bridge.ReadableArray, promise: Promise) {
    if (paths.size() == 0) {
      promise.reject("NO_INPUT", "No video paths provided")
      return
    }

    val outputPath = reactApplicationContext.cacheDir.absolutePath + "/merged.mp4"
    try {
      val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
      var videoTrackIndex = -1
      var audioTrackIndex = -1
      var currentTimeUs: Long = 0

      for (i in 0 until paths.size()) {
        val path = paths.getString(i) ?: continue
        val extractor = MediaExtractor()
        extractor.setDataSource(path)

        // find video/audio tracks
        val trackCount = extractor.trackCount
        for (t in 0 until trackCount) {
          val format = extractor.getTrackFormat(t)
          val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: continue
          extractor.selectTrack(t)

          val dstTrackIndex: Int = if (mime.startsWith("video/")) {
            if (videoTrackIndex == -1) {
              videoTrackIndex = muxer.addTrack(format)
            }
            videoTrackIndex
          } else if (mime.startsWith("audio/")) {
            if (audioTrackIndex == -1) {
              audioTrackIndex = muxer.addTrack(format)
            }
            audioTrackIndex
          } else {
            continue
          }

          if (i == 0 && t == 0) {
            // Start muxer only once after first track is added
            muxer.start()
          }

          val buffer = ByteBuffer.allocate(1024 * 1024)
          val bufferInfo = MediaCodec.BufferInfo()

          extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
          while (true) {
            bufferInfo.offset = 0
            bufferInfo.size = extractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) break

            bufferInfo.presentationTimeUs = extractor.sampleTime + currentTimeUs
            bufferInfo.flags = extractor.sampleFlags

            muxer.writeSampleData(dstTrackIndex, buffer, bufferInfo)
            extractor.advance()
          }
          extractor.release()
        }

        // increase offset for next clip
        currentTimeUs += 1_000_000 // 1s padding between clips
      }

      muxer.stop()
      muxer.release()

      promise.resolve(outputPath)
    } catch (e: Exception) {
      promise.reject("MERGE_ERROR", "Failed to merge videos", e)
    }
  }

override fun addAudio(videoPath: String, audioPath: String, mode: String, promise: Promise) {
  val outputPath = reactApplicationContext.cacheDir.absolutePath + "/video_with_audio.mp4"

  try {
    val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    // --- Extract video ---
    val videoExtractor = MediaExtractor()
    videoExtractor.setDataSource(videoPath)

    var videoTrackIndex = -1
    var videoFormat: MediaFormat? = null
    for (i in 0 until videoExtractor.trackCount) {
      val format = videoExtractor.getTrackFormat(i)
      val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
      if (mime.startsWith("video/")) {
        videoExtractor.selectTrack(i)
        videoTrackIndex = muxer.addTrack(format)
        videoFormat = format
        break
      }
    }

    if (videoTrackIndex == -1) {
      promise.reject("NO_VIDEO", "No video track found in $videoPath")
      return
    }

    val videoDurationUs = videoFormat?.getLong(MediaFormat.KEY_DURATION) ?: Long.MAX_VALUE

    // --- Extract audio ---
    val audioExtractor = MediaExtractor()
    val uri = Uri.parse(audioPath)
    val resolver = reactApplicationContext.contentResolver
    val input = resolver.openFileDescriptor(uri, "r")

    if (input != null) {
      val fd = input.fileDescriptor
      audioExtractor.setDataSource(fd)
      input.close()
    } else {
      promise.reject("AUDIO_ERROR", "Could not open audio file")
      return
    }

    var audioTrackIndex = -1
    for (i in 0 until audioExtractor.trackCount) {
      val format = audioExtractor.getTrackFormat(i)
      val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
      if (mime.startsWith("audio/")) {
        if (mime != "audio/mp4a-latm") {
          promise.reject("UNSUPPORTED_AUDIO", "Audio format $mime not supported. Please use AAC (.m4a)")
          return
        }
        audioExtractor.selectTrack(i)
        audioTrackIndex = muxer.addTrack(format)
        break
      }
    }

    if (audioTrackIndex == -1) {
      promise.reject("NO_AUDIO", "No audio track found in $audioPath")
      return
    }

    // --- Start muxer ---
    muxer.start()

    val buffer = ByteBuffer.allocate(1024 * 1024)
    val bufferInfo = MediaCodec.BufferInfo()

    // --- Write video samples ---
    while (true) {
      bufferInfo.offset = 0
      bufferInfo.size = videoExtractor.readSampleData(buffer, 0)
      if (bufferInfo.size < 0) break

      bufferInfo.presentationTimeUs = videoExtractor.sampleTime
      bufferInfo.flags = videoExtractor.sampleFlags
      muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo)
      videoExtractor.advance()
    }
    videoExtractor.release()

    // --- Write audio samples (clamp to video length) ---
    while (true) {
      bufferInfo.offset = 0
      bufferInfo.size = audioExtractor.readSampleData(buffer, 0)
      if (bufferInfo.size < 0) break

      val sampleTime = audioExtractor.sampleTime
      if (sampleTime > videoDurationUs) break // clamp audio length to video

      bufferInfo.presentationTimeUs = sampleTime
      bufferInfo.flags = audioExtractor.sampleFlags
      muxer.writeSampleData(audioTrackIndex, buffer, bufferInfo)
      audioExtractor.advance()
    }
    audioExtractor.release()

    // --- Finish ---
    muxer.stop()
    muxer.release()

    promise.resolve(outputPath)
  } catch (e: Exception) {
    promise.reject("ADD_AUDIO_ERROR", "Failed to add audio", e)
  }
}

override fun applyFilter(path: String, filter: String, promise: Promise) {
  Thread {
    val inputPath = if (path.startsWith("file://")) path.removePrefix("file://") else path
    val outputPath = reactApplicationContext.cacheDir.absolutePath + "/filtered_${System.currentTimeMillis()}.mp4"

    var videoExtractor: MediaExtractor? = null
    var audioExtractor: MediaExtractor? = null
    var decoder: MediaCodec? = null
    var encoder: MediaCodec? = null
    var inputSurface: Surface? = null
    var muxer: MediaMuxer? = null
    var helper: CodecSurfaceHelper? = null

    fun align(v: Int, a: Int) = ((v + (a - 1)) / a) * a
    fun even(v: Int) = if (v % 2 == 0) v else v + 1

    try {
      // --- Extractors
      videoExtractor = MediaExtractor().apply { setDataSource(inputPath) }
      audioExtractor = MediaExtractor().apply { setDataSource(inputPath) }

      var videoTrack = -1
      var audioTrack = -1
      var videoFormat: MediaFormat? = null
      var audioFormat: MediaFormat? = null

      for (i in 0 until videoExtractor!!.trackCount) {
        val f = videoExtractor!!.getTrackFormat(i)
        if ((f.getString(MediaFormat.KEY_MIME) ?: "").startsWith("video/")) {
          videoTrack = i
          videoFormat = f
          videoExtractor!!.selectTrack(i)
          break
        }
      }
      if (videoTrack == -1 || videoFormat == null) {
        videoExtractor?.release()
        audioExtractor?.release()
        promise.reject("NO_VIDEO", "No video track")
        return@Thread
      }

      for (i in 0 until audioExtractor!!.trackCount) {
        val f = audioExtractor!!.getTrackFormat(i)
        if ((f.getString(MediaFormat.KEY_MIME) ?: "").startsWith("audio/")) {
          audioTrack = i
          audioFormat = f
          break
        }
      }

      val srcW = videoFormat!!.getInteger(MediaFormat.KEY_WIDTH)
      val srcH = videoFormat!!.getInteger(MediaFormat.KEY_HEIGHT)
      val rotation = if (videoFormat!!.containsKey(MediaFormat.KEY_ROTATION)) {
        videoFormat!!.getInteger(MediaFormat.KEY_ROTATION)
      } else 0

      // --- Encoder + caps-driven size/fps/bitrate
      encoder = MediaCodec.createEncoderByType("video/avc")

      val vCaps = encoder!!.codecInfo
        .getCapabilitiesForType("video/avc")
        .videoCapabilities!!   // non-null for AVC on API 21+

      var encW = even(align(srcW, vCaps.widthAlignment))
      var encH = even(align(srcH, vCaps.heightAlignment))

      fun trySet(w: Int, h: Int): Boolean {
        val wA = even(align(w, vCaps.widthAlignment))
        val hA = even(align(h, vCaps.heightAlignment))
        return if (vCaps.isSizeSupported(wA, hA)) { encW = wA; encH = hA; true } else false
      }
      if (!vCaps.isSizeSupported(encW, encH)) {
        if (!trySet(1280, 720) && !trySet(640, 360)) {
          trySet(vCaps.supportedWidths.lower, vCaps.supportedHeights.lower)
        }
      }

      val srcFps = if (videoFormat!!.containsKey(MediaFormat.KEY_FRAME_RATE))
        maxOf(1, videoFormat!!.getInteger(MediaFormat.KEY_FRAME_RATE)) else 30
      val fpsUpper = vCaps.supportedFrameRates.upper.toInt().coerceAtLeast(30)
      val fps = srcFps.coerceIn(1, fpsUpper)

      val pixels = encW * encH
      val bitRate = when {
        pixels >= 1920 * 1080 -> 6_000_000
        pixels >= 1280 * 720  -> 3_000_000
        else                  -> 1_500_000
      }

      val outFormat = MediaFormat.createVideoFormat("video/avc", encW, encH).apply {
        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
      }

      encoder!!.configure(outFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
      inputSurface = encoder!!.createInputSurface()
      encoder!!.start()

      // GL helper + decoder surface (render at encoder size)
      helper = CodecSurfaceHelper(encW, encH).apply {
        setup(inputSurface!!)
        setFilter(filter)
        setRotation(rotation) // apply source rotation in GL
        setFlipY(true)        // fix upside-down
        try { updateDefaultBufferSize(encW, encH) } catch (_: Throwable) {}
      }

      decoder = MediaCodec.createDecoderByType(videoFormat!!.getString(MediaFormat.KEY_MIME)!!).apply {
        configure(videoFormat, helper!!.decoderSurface, null, 0)
        start()
      }

      // Muxer (no orientationHint; rotation handled in GL)
      muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

      var muxerStarted = false
      var muxVideoIndex = -1
      var muxAudioIndex = -1
      if (audioTrack != -1 && audioFormat != null) {
        muxAudioIndex = muxer!!.addTrack(audioFormat!!)
      }

      val decInfo = MediaCodec.BufferInfo()
      val encInfo = MediaCodec.BufferInfo()

      var sawDecoderEOS = false
      var sawEncoderEOS = false

      // Pump
      while (!sawEncoderEOS) {
        if (!sawDecoderEOS) {
          val inIndex = decoder!!.dequeueInputBuffer(10_000)
          if (inIndex >= 0) {
            val inBuf = decoder!!.getInputBuffer(inIndex)
            val sampleSize = videoExtractor!!.readSampleData(inBuf!!, 0)
            if (sampleSize < 0) {
              decoder!!.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
              sawDecoderEOS = true
            } else {
              decoder!!.queueInputBuffer(inIndex, 0, sampleSize, videoExtractor!!.sampleTime, videoExtractor!!.sampleFlags)
              videoExtractor!!.advance()
            }
          }
        }

        when (val outIndex = decoder!!.dequeueOutputBuffer(decInfo, 10_000)) {
          MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
          MediaCodec.INFO_OUTPUT_FORMAT_CHANGED,
          MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Unit
          else -> if (outIndex >= 0) {
            val render = decInfo.size > 0
            decoder!!.releaseOutputBuffer(outIndex, render)
            if (render) helper!!.drawFrame(decInfo.presentationTimeUs)
            if ((decInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
              encoder!!.signalEndOfInputStream()
            }
          }
        }

        loop@ while (true) {
          val eIndex = encoder!!.dequeueOutputBuffer(encInfo, 10_000)
          when {
            eIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> break@loop
            eIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
              muxVideoIndex = muxer!!.addTrack(encoder!!.outputFormat)
              if (!muxerStarted) { muxer!!.start(); muxerStarted = true }
            }
            eIndex >= 0 -> {
              val out = encoder!!.getOutputBuffer(eIndex)!!
              if (encInfo.size > 0) {
                out.position(encInfo.offset)
                out.limit(encInfo.offset + encInfo.size)
                if (!muxerStarted) {
                  muxVideoIndex = muxer!!.addTrack(encoder!!.outputFormat)
                  muxer!!.start(); muxerStarted = true
                }
                muxer!!.writeSampleData(muxVideoIndex, out, encInfo)
              }
              val isEos = (encInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
              encoder!!.releaseOutputBuffer(eIndex, false)
              if (isEos) { sawEncoderEOS = true; break@loop }
            }
          }
        }
      }

      // Audio passthrough
      if (muxerStarted && muxAudioIndex != -1) {
        audioExtractor!!.selectTrack(audioTrack)
        val aInfo = MediaCodec.BufferInfo()
        val max = if (audioFormat!!.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE))
          audioFormat!!.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 262_144
        val buffer = ByteBuffer.allocateDirect(max)
        while (true) {
          val size = audioExtractor!!.readSampleData(buffer, 0)
          if (size < 0) break
          aInfo.offset = 0
          aInfo.size = size
          aInfo.presentationTimeUs = audioExtractor!!.sampleTime
          aInfo.flags = audioExtractor!!.sampleFlags
          muxer!!.writeSampleData(muxAudioIndex, buffer, aInfo)
          audioExtractor!!.advance()
        }
      }

      muxer!!.stop()
      promise.resolve("file://$outputPath")
    } catch (e: Exception) {
      promise.reject("APPLY_FILTER_ERROR", "Failed to apply filter", e)
    } finally {
      try { decoder?.stop() } catch (_: Throwable) {}
      try { decoder?.release() } catch (_: Throwable) {}
      try { encoder?.stop() } catch (_: Throwable) {}
      try { encoder?.release() } catch (_: Throwable) {}
      try { inputSurface?.release() } catch (_: Throwable) {}
      try { muxer?.release() } catch (_: Throwable) {}
      try { videoExtractor?.release() } catch (_: Throwable) {}
      try { audioExtractor?.release() } catch (_: Throwable) {}
      try { helper?.release() } catch (_: Throwable) {}
    }
  }.start()
}

  companion object {
    const val NAME = "VideoLab"
  }
}
