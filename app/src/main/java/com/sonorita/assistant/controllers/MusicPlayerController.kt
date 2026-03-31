package com.sonorita.assistant.controllers

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.provider.MediaStore

class MusicPlayerController(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val playlist = mutableListOf<MusicTrack>()
    private var currentIndex = 0
    private var isPlaying = false

    data class MusicTrack(
        val title: String,
        val artist: String,
        val path: String,
        val duration: Long,
        val uri: Uri
    )

    fun scanMusic(): List<MusicTrack> {
        playlist.clear()
        val contentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown"
                    val artist = cursor.getString(artistCol) ?: "Unknown"
                    val path = cursor.getString(dataCol) ?: ""
                    val duration = cursor.getLong(durationCol)
                    val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())

                    playlist.add(MusicTrack(title, artist, path, duration, uri))
                }
            }
        } catch (e: Exception) {
            // Permission issue
        }

        return playlist
    }

    fun playMusic(query: String = ""): String {
        if (playlist.isEmpty()) {
            scanMusic()
        }

        if (playlist.isEmpty()) {
            return "Phone e kono music file nei."
        }

        // Find matching track
        val track = if (query.isNotEmpty()) {
            playlist.find {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
            } ?: playlist.first()
        } else {
            playlist.first()
        }

        currentIndex = playlist.indexOf(track)
        return playTrack(track)
    }

    private fun playTrack(track: MusicTrack): String {
        return try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, track.uri)
                prepare()
                start()
            }
            isPlaying = true

            val durationSec = track.duration / 1000
            val minutes = durationSec / 60
            val seconds = durationSec % 60

            "🎵 Bajacchi: ${track.title} — ${track.artist} (${minutes}:${String.format("%02d", seconds)})"
        } catch (e: Exception) {
            "Music play korte parlam na: ${e.message}"
        }
    }

    fun pause(): String {
        return if (isPlaying && mediaPlayer != null) {
            mediaPlayer?.pause()
            isPlaying = false
            "⏸️ Music paused!"
        } else {
            "Kichu bajche na."
        }
    }

    fun resume(): String {
        return if (mediaPlayer != null && !isPlaying) {
            mediaPlayer?.start()
            isPlaying = true
            "▶️ Music resumed!"
        } else {
            "Already bajche na."
        }
    }

    fun next(): String {
        if (playlist.isEmpty()) return "Playlist empty."
        currentIndex = (currentIndex + 1) % playlist.size
        return playTrack(playlist[currentIndex])
    }

    fun previous(): String {
        if (playlist.isEmpty()) return "Playlist empty."
        currentIndex = if (currentIndex > 0) currentIndex - 1 else playlist.size - 1
        return playTrack(playlist[currentIndex])
    }

    fun stop(): String {
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        return "⏹️ Music stopped!"
    }

    fun getNowPlaying(): String {
        if (!isPlaying || playlist.isEmpty()) return "Kichu bajche na."

        val track = playlist[currentIndex]
        val position = mediaPlayer?.currentPosition?.let { it / 1000 } ?: 0
        val duration = track.duration / 1000
        val posMin = position / 60
        val posSec = position % 60
        val durMin = duration / 60
        val durSec = duration % 60

        return buildString {
            appendLine("🎵 Now Playing:")
            appendLine("Title: ${track.title}")
            appendLine("Artist: ${track.artist}")
            appendLine("Progress: ${posMin}:${String.format("%02d", posSec)} / ${durMin}:${String.format("%02d", durSec)}")
        }
    }

    fun getPlaylist(): String {
        if (playlist.isEmpty()) {
            scanMusic()
        }

        return if (playlist.isEmpty()) {
            "Phone e kono music nei."
        } else {
            buildString {
                appendLine("🎵 Music Library (${playlist.size} tracks):")
                playlist.take(15).forEachIndexed { i, track ->
                    val durationMin = track.duration / 60000
                    val durationSec = (track.duration % 60000) / 1000
                    appendLine("${i + 1}. ${track.title} — ${track.artist} (${durationMin}:${String.format("%02d", durationSec)})")
                }
                if (playlist.size > 15) appendLine("... r ${playlist.size - 15} tracks")
            }
        }
    }

    fun playByGenre(genre: String): String {
        if (playlist.isEmpty()) scanMusic()

        val filtered = playlist.filter {
            it.title.contains(genre, ignoreCase = true) ||
            it.artist.contains(genre, ignoreCase = true)
        }

        return if (filtered.isNotEmpty()) {
            playTrack(filtered.first())
        } else {
            "Genre '$genre' match korar moto music pai ni. Shob music play korchi."
            playMusic()
        }
    }

    fun isPlaying(): Boolean = isPlaying
}
