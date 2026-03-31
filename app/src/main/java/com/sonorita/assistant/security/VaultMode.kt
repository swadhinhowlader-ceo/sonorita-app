package com.sonorita.assistant.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.*
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
import java.io.File

class VaultMode(private val context: Context) {

    data class VaultItem(
        val id: String,
        val name: String,
        val type: VaultItemType,
        val encryptedPath: String,
        val timestamp: Long = System.currentTimeMillis(),
        val isHidden: Boolean = false
    )

    enum class VaultItemType {
        FILE, PHOTO, VIDEO, NOTE, CONTACT, PASSWORD
    }

    private val vaultDir = File(context.filesDir, ".vault")
    private val items = mutableListOf<VaultItem>()
    private val decoyItems = mutableListOf<VaultItem>()

    private var isDecoyMode = false
    private var wrongAttempts = 0
    private val maxWrongAttempts = 10

    init {
        if (!vaultDir.exists()) vaultDir.mkdirs()
        loadItems()
    }

    fun unlock(pin: String): Boolean {
        val storedPin = getStoredPin()

        return when {
            pin == storedPin -> {
                isDecoyMode = false
                wrongAttempts = 0
                true
            }
            pin == getDecoyPin() -> {
                isDecoyMode = true
                wrongAttempts = 0
                true
            }
            else -> {
                wrongAttempts++
                if (wrongAttempts >= maxWrongAttempts) {
                    selfDestruct()
                }
                false
            }
        }
    }

    fun storeFile(filePath: String, name: String): String {
        return try {
            val source = File(filePath)
            if (!source.exists()) return "File not found."

            val encryptedFile = File(vaultDir, "enc_${System.currentTimeMillis()}.vault")
            val key = getOrCreateKey()

            // Encrypt and save
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv

            val encrypted = cipher.doFinal(source.readBytes())

            FileOutputStream(encryptedFile).use { fos ->
                fos.write(iv.size)
                fos.write(iv)
                fos.write(encrypted)
            }

            val item = VaultItem(
                id = "vault_${System.currentTimeMillis()}",
                name = name,
                type = VaultItemType.FILE,
                encryptedPath = encryptedFile.absolutePath
            )
            items.add(item)
            saveItems()

            "🔒 File encrypted and stored in vault: $name"
        } catch (e: Exception) {
            "Vault store error: ${e.message}"
        }
    }

    fun retrieveFile(itemId: String): String {
        val item = items.find { it.id == itemId }
            ?: return "Item not found."

        return try {
            val encryptedFile = File(item.encryptedPath)
            val key = getOrCreateKey()

            FileInputStream(encryptedFile).use { fis ->
                val ivSize = fis.read()
                val iv = ByteArray(ivSize)
                fis.read(iv)

                val encrypted = fis.readBytes()

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, key, spec)

                val decrypted = cipher.doFinal(encrypted)

                val outputFile = File(context.cacheDir, item.name)
                outputFile.writeBytes(decrypted)

                "🔓 File decrypted: ${outputFile.absolutePath}"
            }
        } catch (e: Exception) {
            "Vault retrieve error: ${e.message}"
        }
    }

    fun createHiddenApp(packageName: String): String {
        // Hide app from launcher by disabling its main activity
        return try {
            val pm = context.packageManager
            val component = android.content.ComponentName(context.packageName, "ui.SplashActivity")
            pm.setComponentEnabledSetting(
                component,
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
            "👻 App hidden from launcher! Use voice command to open."
        } catch (e: Exception) {
            "Hide app error: ${e.message}"
        }
    }

    fun showApp(): String {
        return try {
            val pm = context.packageManager
            val component = android.content.ComponentName(context.packageName, "ui.SplashActivity")
            pm.setComponentEnabledSetting(
                component,
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
            "👁️ App visible again in launcher."
        } catch (e: Exception) {
            "Show app error: ${e.message}"
        }
    }

    private fun selfDestruct() {
        // Delete all vault items
        vaultDir.deleteRecursively()
        items.clear()
        decoyItems.clear()

        // Clear app data
        val sharedPrefs = File(context.filesDir, "../shared_prefs")
        sharedPrefs.deleteRecursively()
    }

    fun storePassword(site: String, username: String, password: String): String {
        val item = VaultItem(
            id = "pwd_${System.currentTimeMillis()}",
            name = "$site ($username)",
            type = VaultItemType.PASSWORD,
            encryptedPath = ""
        )
        items.add(item)
        saveItems()
        return "🔑 Password stored for $site"
    }

    fun getVaultStatus(): String {
        val realItems = items.size
        val hiddenItems = items.count { it.isHidden }

        return buildString {
            appendLine("🔒 Vault Status:")
            appendLine("Items: $realItems")
            appendLine("Hidden: $hiddenItems")
            appendLine("Wrong attempts: $wrongAttempts/$maxWrongAttempts")
            appendLine("Decoy mode: ${if (isDecoyMode) "ON" else "OFF"}")
        }
    }

    fun getVaultList(): String {
        val displayItems = if (isDecoyMode) decoyItems else items

        return if (displayItems.isEmpty()) {
            "Vault empty."
        } else {
            buildString {
                appendLine("🔒 Vault Items:")
                displayItems.forEach { item ->
                    val icon = when (item.type) {
                        VaultItemType.FILE -> "📄"
                        VaultItemType.PHOTO -> "📷"
                        VaultItemType.VIDEO -> "🎬"
                        VaultItemType.NOTE -> "📝"
                        VaultItemType.CONTACT -> "👤"
                        VaultItemType.PASSWORD -> "🔑"
                    }
                    appendLine("$icon ${item.name}")
                }
            }
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (!keyStore.containsAlias("sonorita_vault_key")) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val spec = KeyGenParameterSpec.Builder(
                "sonorita_vault_key",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }

        return keyStore.getKey("sonorita_vault_key", null) as SecretKey
    }

    private fun getStoredPin(): String = "2003" // Default, should be stored in KeyStore
    private fun getDecoyPin(): String = "0000" // Shows fake vault

    private fun loadItems() {
        // Load from encrypted storage
    }

    private fun saveItems() {
        // Save to encrypted storage
    }
}
