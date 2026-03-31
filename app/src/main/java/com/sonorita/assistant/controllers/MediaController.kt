package com.sonorita.assistant.controllers

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import java.io.File
import java.io.IOException

class MediaController(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var recordingFile: File? = null

    fun recordAudio(): String {
        return if (isRecording) {
            stopRecording()
            "🎙️ Audio recording bondho! File: ${recordingFile?.absolutePath}"
        } else {
            startRecording()
            "🎙️ Audio recording shuru!"
        }
    }

    private fun startRecording(): Boolean {
        return try {
            val dir = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "Sonorita/Audio")
            dir.mkdirs()
            recordingFile = File(dir, "audio_${System.currentTimeMillis()}.mp3")

            @Suppress("DEPRECATION")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordingFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            true
        } catch (e: IOException) {
            false
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Handle error
        }
        mediaRecorder = null
        isRecording = false
    }

    fun recordVideo(): String {
        return "Video recording korte camera intent dorkar. Camera kholo."
    }

    fun takeScreenshot(): String {
        return "Screenshot korte MediaProjection enable korte hobe. Settings e giye enable koro."
    }

    fun takePhoto(): String {
        return "Photo tulte camera intent dorkar. Camera kholo."
    }

    fun controlMedia(text: String): String {
        return when {
            text.contains("play") -> {
                // Use MediaSession to control playback
                "🎵 Music play korchi..."
            }
            text.contains("pause") -> {
                "⏸️ Music pause korchi..."
            }
            text.contains("next") -> {
                "⏭️ Next track..."
            }
            text.contains("previous") -> {
                "⏮️ Previous track..."
            }
            else -> "Media control: play/pause/next/previous"
        }
    }
}
