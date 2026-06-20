package com.example.api

import android.util.Log
import com.example.data.SyncPayload
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class GoogleDriveSyncService {
    private val TAG = "GoogleDriveSyncService"
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val payloadAdapter = moshi.adapter(SyncPayload::class.java)

    /**
     * Compresses a string using GZIP.
     */
    private fun compress(data: String): ByteArray {
        val bos = ByteArrayOutputStream(data.length)
        GZIPOutputStream(bos).use { it.write(data.toByteArray(Charsets.UTF_8)) }
        return bos.toByteArray()
    }

    /**
     * Decompresses a GZIP byte array into a string.
     */
    private fun decompress(data: ByteArray): String {
        return GZIPInputStream(ByteArrayInputStream(data)).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    /**
     * Checks if a byte array is GZIP compressed.
     */
    private fun isGzipped(data: ByteArray): Boolean {
        return data.size >= 2 && data[0] == 0x1f.toByte() && data[1] == 0x8b.toByte()
    }

    /**
     * Resolves the file ID of "gayatri_japa_data.json" inside the hidden "appDataFolder".
     * Returns null if not found or the token is invalid.
     */
    private fun findBackupFileId(accessToken: String): String? {
        val url = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=name%3D%27gayatri_japa_data.json%27"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Search request failed with status code ${response.code}")
                    return null
                }
                val bodyStr = response.body?.string() ?: return null
                val json = JSONObject(bodyStr)
                val files = json.getJSONArray("files")
                if (files.length() > 0) {
                    return files.getJSONObject(0).getString("id")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception searching for backup file: ${e.message}", e)
        }
        return null
    }

    /**
     * Creates an empty metadata placeholder for the file and returns its newly allocated File ID.
     */
    private fun createMetadataPlaceholder(accessToken: String): String? {
        val url = "https://www.googleapis.com/drive/v3/files"
        // Ensure "parents" is a JSON array instead of a string slice
        val jsonPayload = JSONObject()
            .put("name", "gayatri_japa_data.json")
            .put("parents", org.json.JSONArray(listOf("appDataFolder")))
            .toString()

        val body = jsonPayload.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Creating metadata failed: ${response.code} -> ${response.body?.string()}")
                    return null
                }
                val bodyStr = response.body?.string() ?: return null
                return JSONObject(bodyStr).getString("id")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating metadata placeholder: ${e.message}", e)
        }
        return null
    }

    /**
     * Fetches the user backup json payload from the appDataFolder on Google Drive.
     */
    fun fetchCloudData(accessToken: String): SyncPayload? {
        val fileId = findBackupFileId(accessToken) ?: return null
        val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Fetching media failed: ${response.code}")
                    return null
                }
                val bodyBytes = response.body?.bytes() ?: return null
                val bodyStr = if (isGzipped(bodyBytes)) {
                    decompress(bodyBytes)
                } else {
                    String(bodyBytes, Charsets.UTF_8)
                }
                Log.d(TAG, "Downloaded payload successfully.")
                return payloadAdapter.fromJson(bodyStr)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading cloud metadata: ${e.message}", e)
        }
        return null
    }

    /**
     * Uploads the local repository state into Google Drive appDataFolder.
     */
    fun saveCloudData(accessToken: String, payload: SyncPayload): Boolean {
        var fileId = findBackupFileId(accessToken)
        if (fileId == null) {
            Log.d(TAG, "No existing backup found. Creating metadata file...")
            fileId = createMetadataPlaceholder(accessToken)
            if (fileId == null) {
                Log.e(TAG, "Unable to create backup metadata descriptor!")
                return false
            }
        }

        val url = "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media"
        val jsonString = payloadAdapter.toJson(payload)
        val compressedBytes = compress(jsonString)
        val body = compressedBytes.toRequestBody("application/octet-stream".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .patch(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Uploaded japa backup successfully.")
                    return true
                } else {
                    Log.e(TAG, "Upload failure: code ${response.code} -> ${response.body?.string()}")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network IO connection error syncing to cloud: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "General exception during upload: ${e.message}", e)
        }
        return false
    }
}
