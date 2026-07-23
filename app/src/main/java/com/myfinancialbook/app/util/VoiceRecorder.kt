package com.myfinancialbook.app.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var lastFile: File? = null
    var isRecording: Boolean = false
        private set

    fun startRecording(): Boolean {
        stopRecording()
        return try {
            val file = File(context.filesDir, "voice_${System.currentTimeMillis()}.mp4")
            lastFile = file

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            Log.d("VoiceRecorder", "Started recording: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "Failed to start recording", e)
            recorder = null
            false
        }
    }

    fun stopRecording(): String? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false
            val path = lastFile?.absolutePath
            Log.d("VoiceRecorder", "Stopped recording: $path")
            path
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "Failed to stop recording", e)
            recorder = null
            isRecording = false
            null
        }
    }
}
