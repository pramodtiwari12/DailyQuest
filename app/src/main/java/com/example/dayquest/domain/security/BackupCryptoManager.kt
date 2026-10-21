package com.example.dayquest.domain.security

import android.util.Base64
import com.example.dayquest.data.model.TaskEntity
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCryptoManager {

    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_BITS = 128

    /**
     * Encrypt plaintext JSON using AES-256-GCM and a user passphrase.
     * Returns a versioned JSON envelope with salt, IV, ciphertext, and SHA-256 checksum.
     */
    fun encryptBackup(plaintextJson: String, passphrase: String): String {
        require(passphrase.length >= 4) { "Passphrase must be at least 4 characters." }
        val random = SecureRandom()
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val iv = ByteArray(12).also { random.nextBytes(it) }

        val keySpec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val secretKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec)
        val aesKey = SecretKeySpec(secretKey.encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintextJson.toByteArray(Charsets.UTF_8))

        val sha256 = sha256Hex(plaintextJson)

        val envelope = JSONObject().apply {
            put("format_version", 1)
            put("cipher", "AES-256-GCM")
            put("kdf", "PBKDF2WithHmacSHA256")
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            put("plaintext_sha256", sha256)
            put("created_utc", System.currentTimeMillis())
        }
        return envelope.toString(2)
    }

    /**
     * Decrypt a versioned AES-256-GCM envelope using the user's passphrase and verify SHA-256 integrity.
     */
    fun decryptBackup(envelopeJson: String, passphrase: String): String {
        val obj = JSONObject(envelopeJson)
        val version = obj.optInt("format_version", 0)
        require(version == 1) { "Unsupported backup envelope version: $version" }

        val salt = Base64.decode(obj.getString("salt"), Base64.NO_WRAP)
        val iv = Base64.decode(obj.getString("iv"), Base64.NO_WRAP)
        val ciphertext = Base64.decode(obj.getString("ciphertext"), Base64.NO_WRAP)
        val expectedSha = obj.getString("plaintext_sha256")

        val keySpec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val secretKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec)
        val aesKey = SecretKeySpec(secretKey.encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val decryptedBytes = cipher.doFinal(ciphertext)
        val plaintext = String(decryptedBytes, Charsets.UTF_8)

        val actualSha = sha256Hex(plaintext)
        require(actualSha == expectedSha) { "Backup SHA-256 integrity mismatch." }
        return plaintext
    }

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun exportTasksToJson(tasks: List<TaskEntity>): String {
        val root = JSONObject()
        root.put("schema_version", 1)
        root.put("exported_at", System.currentTimeMillis())
        val arr = JSONArray()
        for (t in tasks) {
            val item = JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("notes", t.notes ?: "")
                put("categoryId", t.categoryId)
                put("difficulty", t.difficulty)
                put("estMinutes", t.estMinutes)
                put("scheduleType", t.scheduleType)
                if (t.dueAt != null) put("dueAt", t.dueAt)
                put("localDateKey", t.localDateKey)
                put("status", t.status)
                put("xpAwarded", t.xpAwarded)
            }
            arr.put(item)
        }
        root.put("tasks", arr)
        return root.toString(2)
    }

    fun importTasksFromJson(jsonStr: String): List<TaskEntity> {
        val root = JSONObject(jsonStr)
        val arr = root.getJSONArray("tasks")
        val list = mutableListOf<TaskEntity>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val title = item.optString("title", "").trim()
            if (title.isNotEmpty()) {
                list.add(
                    TaskEntity(
                        id = item.optString("id", java.util.UUID.randomUUID().toString()),
                        title = title.take(120),
                        notes = item.optString("notes", "").ifEmpty { null },
                        categoryId = item.optString("categoryId", "Other"),
                        difficulty = item.optString("difficulty", "Medium"),
                        estMinutes = item.optInt("estMinutes", 30),
                        scheduleType = item.optString("scheduleType", "anytime"),
                        dueAt = if (item.has("dueAt")) item.getLong("dueAt") else null,
                        localDateKey = item.optString("localDateKey", "2026-10-20"),
                        status = item.optString("status", "pending"),
                        xpAwarded = item.optInt("xpAwarded", 0)
                    )
                )
            }
        }
        return list
    }

    fun exportTasksToCsv(tasks: List<TaskEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("id,title,category,difficulty,est_minutes,schedule_type,local_date,status,xp_awarded")
        for (t in tasks) {
            val escapedTitle = "\"${t.title.replace("\"", "\"\"")}\""
            sb.appendLine("${t.id},$escapedTitle,${t.categoryId},${t.difficulty},${t.estMinutes},${t.scheduleType},${t.localDateKey},${t.status},${t.xpAwarded}")
        }
        return sb.toString()
    }
}
