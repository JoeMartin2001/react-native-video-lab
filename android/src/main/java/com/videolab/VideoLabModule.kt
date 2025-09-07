package com.videolab

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import android.media.MediaFormat
import android.util.Log
import android.net.Uri
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
    promise.reject("UNIMPLEMENTED", "applyFilter not implemented yet")
  }

  companion object {
    const val NAME = "VideoLab"
  }
}
