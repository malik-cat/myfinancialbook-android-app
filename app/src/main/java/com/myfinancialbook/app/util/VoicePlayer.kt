package com.myfinancialbook.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import java.io.File

object VoicePlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun play(context: Context, path: String) {
        try {
            stop()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                val file = File(path)
                if (file.exists()) {
                    setDataSource(context, Uri.fromFile(file))
                    prepare()
                    start()
                    Log.d("VoicePlayer", "Started playing: $path")
                } else {
                    Log.e("VoicePlayer", "File not found: $path")
                }
            }
        } catch (e: Exception) {
            Log.e("VoicePlayer", "Failed to play audio", e)
        }
    }

    fun stop() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("VoicePlayer", "Failed to stop audio", e)
        }
    }
}
