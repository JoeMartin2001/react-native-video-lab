package com.videolab

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import java.nio.ByteBuffer

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
    promise.reject("UNIMPLEMENTED", "merge not implemented yet")
  }

  override fun addAudio(videoPath: String, audioPath: String, mode: String, promise: Promise) {
    // TODO: use `mode` (could be "replace", "mix", etc. depending on your design)
    promise.resolve("with-audio.mp4")
  }

  override fun applyFilter(path: String, filter: String, promise: Promise) {
    promise.reject("UNIMPLEMENTED", "applyFilter not implemented yet")
  }

  companion object {
    const val NAME = "VideoLab"
  }
}
