package com.sonorita.assistant.controllers

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.sonorita.assistant.services.SonoritaAccessibilityService

class YouTubeController(private val context: Context) {

    private val accessibilityService = SonoritaAccessibilityService.instance

    fun openYouTube(): String {
        return openApp("com.google.android.youtube")
    }

    fun searchAndPlay(query: String): String {
        return try {
            // Use YouTube intent to search and play
            val intent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "🎬 YouTube e '$query' search korchi..."
        } catch (e: Exception) {
            // Fallback: open with search URL
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "🎬 YouTube e '$query' khulchi..."
            } catch (e2: Exception) {
                "YouTube open korte parlam na: ${e2.message}"
            }
        }
    }

    fun playVideo(videoId: String): String {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://www.youtube.com/watch?v=$videoId")
                setPackage("com.google.android.youtube")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "🎬 Video play korchi..."
        } catch (e: Exception) {
            "Video play korte parlam na: ${e.message}"
        }
    }

    fun pauseVideo(): String {
        return controlPlayback("pause")
    }

    fun resumeVideo(): String {
        return controlPlayback("play")
    }

    fun nextVideo(): String {
        return controlPlayback("next")
    }

    fun previousVideo(): String {
        return controlPlayback("previous")
    }

    fun playBengaliSong(songName: String = ""): String {
        val query = if (songName.isNotEmpty()) "$songName Bengali song" else "Bengali song"
        return searchAndPlay(query)
    }

    fun playHindiSong(songName: String = ""): String {
        val query = if (songName.isNotEmpty()) "$songName Hindi song" else "Hindi song"
        return searchAndPlay(query)
    }

    fun playMusicByGenre(genre: String): String {
        return searchAndPlay("$genre music")
    }

    private fun controlPlayback(action: String): String {
        val service = accessibilityService
        if (service == null) {
            return "Accessibility service active nei. Settings e giye enable koro."
        }

        return when (action) {
            "pause" -> {
                // Try to find and click pause button via accessibility
                val root = service.getRootNode()
                if (root != null) {
                    val pauseBtn = service.findNodeByText(root, "Pause") 
                        ?: service.findNodeByText(root, "পজ")
                    if (pauseBtn != null && service.clickNode(pauseBtn)) {
                        "⏸️ Video paused!"
                    } else {
                        "Pause button pai ni. Screen e pause button ache?"
                    }
                } else {
                    "Active window pai ni."
                }
            }
            "play" -> {
                val root = service.getRootNode()
                if (root != null) {
                    val playBtn = service.findNodeByText(root, "Play")
                        ?: service.findNodeByText(root, "Play video")
                    if (playBtn != null && service.clickNode(playBtn)) {
                        "▶️ Video playing!"
                    } else {
                        "Play button pai ni."
                    }
                } else {
                    "Active window pai ni."
                }
            }
            "next" -> {
                val root = service.getRootNode()
                if (root != null) {
                    val nextBtn = service.findNodeByText(root, "Next")
                    if (nextBtn != null && service.clickNode(nextBtn)) {
                        "⏭️ Next video!"
                    } else {
                        "Next button pai ni."
                    }
                } else {
                    "Active window pai ni."
                }
            }
            "previous" -> {
                val root = service.getRootNode()
                if (root != null) {
                    val prevBtn = service.findNodeByText(root, "Previous")
                    if (prevBtn != null && service.clickNode(prevBtn)) {
                        "⏮️ Previous video!"
                    } else {
                        "Previous button pai ni."
                    }
                } else {
                    "Active window pai ni."
                }
            }
            else -> "Unknown action: $action"
        }
    }

    private fun openApp(packageName: String): String {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "📱 App kholchi: $packageName"
            } else {
                "App '$packageName' install nei."
            }
        } catch (e: Exception) {
            "App open korte parlam na: ${e.message}"
        }
    }

    fun getNowPlaying(): String {
        val service = accessibilityService ?: return "Accessibility service active nei."
        val root = service.getRootNode() ?: return "Active window pai ni."

        // Try to find video title
        val titleNode = service.findNodeByText(root, "Video title")
            ?: service.findNodeByText(root, "YouTube")

        return if (titleNode != null) {
            "🎬 Now playing: ${titleNode.text ?: "Unknown"}"
        } else {
            "YouTube active nei, kichu play hocche na."
        }
    }
}
