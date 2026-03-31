package com.sonorita.assistant.ai

import android.content.Context
import dalvik.system.DexClassLoader
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarFile

class PluginSystem(private val context: Context) {

    data class Plugin(
        val id: String,
        val name: String,
        val version: String,
        val description: String,
        val author: String,
        val commands: List<String>,
        val filePath: String,
        val isActive: Boolean = false
    )

    data class PluginManifest(
        val id: String,
        val name: String,
        val version: String,
        val description: String,
        val author: String,
        val mainClass: String,
        val commands: List<String>,
        val permissions: List<String> = emptyList()
    )

    private val plugins = mutableListOf<Plugin>()
    private val loadedClasses = mutableMapOf<String, Any>()

    fun loadPlugins(): String {
        val pluginDir = File(context.filesDir, "plugins")
        if (!pluginDir.exists()) {
            pluginDir.mkdirs()
            return "🧩 Plugin directory created. Add .sonorita-plugin files to install."
        }

        val pluginFiles = pluginDir.listFiles { file -> file.extension == "sonorita-plugin" }
            ?: return "No plugins found."

        pluginFiles.forEach { file ->
            try {
                loadPlugin(file)
            } catch (e: Exception) {
                // Skip failed plugins
            }
        }

        return "🧩 Loaded ${plugins.size} plugins."
    }

    private fun loadPlugin(file: File) {
        val manifest = readManifest(file) ?: return

        val plugin = Plugin(
            id = manifest.id,
            name = manifest.name,
            version = manifest.version,
            description = manifest.description,
            author = manifest.author,
            commands = manifest.commands,
            filePath = file.absolutePath,
            isActive = true
        )

        plugins.add(plugin)

        // Load the plugin class
        try {
            val dexOutputDir = File(context.filesDir, "dex_output")
            dexOutputDir.mkdirs()

            val classLoader = DexClassLoader(
                file.absolutePath,
                dexOutputDir.absolutePath,
                null,
                context.classLoader
            )

            val pluginClass = classLoader.loadClass(manifest.mainClass)
            val pluginInstance = pluginClass.newInstance()
            loadedClasses[manifest.id] = pluginInstance
        } catch (e: Exception) {
            // Plugin loading failed
        }
    }

    private fun readManifest(file: File): PluginManifest? {
        return try {
            val jar = JarFile(file)
            val manifestEntry = jar.getEntry("manifest.json")
            if (manifestEntry != null) {
                val content = jar.getInputStream(manifestEntry).bufferedReader().readText()
                com.google.gson.Gson().fromJson(content, PluginManifest::class.java)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun executePluginCommand(command: String): String {
        val lower = command.lowercase()

        for (plugin in plugins.filter { it.isActive }) {
            if (plugin.commands.any { lower.contains(it.lowercase()) }) {
                return try {
                    val instance = loadedClasses[plugin.id]
                    val method = instance?.javaClass?.getMethod("execute", String::class.java)
                    method?.invoke(instance, command) as? String
                        ?: "Plugin '${plugin.name}' executed but returned nothing."
                } catch (e: Exception) {
                    "Plugin '${plugin.name}' error: ${e.message}"
                }
            }
        }

        return "No plugin found for command: $command"
    }

    fun installPlugin(filePath: String): String {
        val source = File(filePath)
        if (!source.exists()) return "File not found: $filePath"

        val pluginDir = File(context.filesDir, "plugins")
        pluginDir.mkdirs()

        val dest = File(pluginDir, source.name)
        source.copyTo(dest, overwrite = true)

        return try {
            loadPlugin(dest)
            "🧩 Plugin installed: ${source.name}"
        } catch (e: Exception) {
            "Plugin install failed: ${e.message}"
        }
    }

    fun uninstallPlugin(pluginId: String): String {
        val plugin = plugins.find { it.id == pluginId }
            ?: return "Plugin '$pluginId' not found."

        val file = File(plugin.filePath)
        if (file.exists()) file.delete()

        plugins.remove(plugin)
        loadedClasses.remove(pluginId)

        return "🧩 Plugin uninstalled: ${plugin.name}"
    }

    fun getPluginList(): String {
        if (plugins.isEmpty()) return "No plugins installed. Add .sonorita-plugin files to plugins/ folder."

        return buildString {
            appendLine("🧩 Installed Plugins (${plugins.size}):")
            plugins.forEach { plugin ->
                val status = if (plugin.isActive) "🟢" else "🔴"
                appendLine("$status ${plugin.name} v${plugin.version} by ${plugin.author}")
                appendLine("   ${plugin.description}")
                appendLine("   Commands: ${plugin.commands.joinToString()}")
            }
        }
    }

    fun togglePlugin(pluginId: String): String {
        val plugin = plugins.find { it.id == pluginId }
            ?: return "Plugin not found."

        val idx = plugins.indexOf(plugin)
        plugins[idx] = plugin.copy(isActive = !plugin.isActive)

        return if (plugins[idx].isActive) {
            "🧩 Plugin '${plugin.name}' enabled!"
        } else {
            "🧩 Plugin '${plugin.name}' disabled."
        }
    }

    fun getPluginCount(): Int = plugins.size
}
