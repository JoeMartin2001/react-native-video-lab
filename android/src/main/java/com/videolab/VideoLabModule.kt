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
