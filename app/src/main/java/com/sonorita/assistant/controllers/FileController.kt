package com.sonorita.assistant.controllers

import android.content.Context
import android.os.Environment
import java.io.File

class FileController(private val context: Context) {

    fun searchFiles(query: String): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val results = mutableListOf<String>()

        fun searchRecursive(dir: File) {
            dir.listFiles()?.forEach { file ->
                if (file.name.contains(query, ignoreCase = true)) {
                    results.add(file.absolutePath)
                }
                if (file.isDirectory) {
                    searchRecursive(file)
                }
            }
        }

        searchRecursive(downloadsDir)

        return if (results.isEmpty()) {
            "'$query' naam er kono file pai ni."
        } else {
            "📁 Found ${results.size} files:\n" + results.take(10).joinToString("\n")
        }
    }

    fun createFile(name: String, content: String, path: String = "Sonorita"): String {
        return try {
            val dir = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), path)
            dir.mkdirs()
            val file = File(dir, name)
            file.writeText(content)
            "📄 File toiri: ${file.absolutePath}"
        } catch (e: Exception) {
            "File create korte parlam na: ${e.message}"
        }
    }

    fun deleteFile(path: String): String {
        return try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
                "🗑️ File delete hoyeche: ${file.name}"
            } else {
                "File pai ni: $path"
            }
        } catch (e: Exception) {
            "File delete korte parlam na: ${e.message}"
        }
    }

    fun copyFile(source: String, destination: String): String {
        return try {
            val src = File(source)
            val dest = File(destination)
            src.copyTo(dest, overwrite = true)
            "📋 File copy hoyeche: ${dest.name}"
        } catch (e: Exception) {
            "File copy korte parlam na: ${e.message}"
        }
    }

    fun getFileInfo(path: String): String {
        val file = File(path)
        return if (file.exists()) {
            buildString {
                appendLine("📄 File Info:")
                appendLine("Name: ${file.name}")
                appendLine("Size: ${file.length() / 1024} KB")
                appendLine("Modified: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(file.lastModified()))}")
                appendLine("Path: ${file.absolutePath}")
            }
        } else {
            "File pai ni: $path"
        }
    }
}
