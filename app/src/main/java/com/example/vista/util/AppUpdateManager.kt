package com.example.vista.util

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class Downloading(val progress: Int = 0) : UpdateState()
    data class UpdateAvailable(
        val latestVersion: String,
        val latestVersionCode: Int,
        val downloadUrl: String,
        val changelog: String,
        val mandatory: Boolean = false
    ) : UpdateState()
    data class DownloadComplete(val file: File) : UpdateState()
    data class Failed(val message: String) : UpdateState()
}

/**
 * Sideloaded update manager for apps distributed OUTSIDE the Play Store.
 *
 * The app checks a version manifest (JSON) hosted on the developer's website,
 * compares the versionCode, downloads the APK via DownloadManager and launches
 * the system package installer.
 */
object InAppUpdateManager {

    private const val BASE_URL = "https://lxobsidianportal.co.za"
    private const val VERSION_CHECK_URL = "$BASE_URL/vista-version.json"
    private const val APK_FILE_NAME = "VistaCinema-update.apk"
    private const val PREFS_NAME = "vista_update_prefs"
    private const val KEY_DISMISSED_CODE = "dismissed_version_code"
    private const val TEN_MINUTES_MS = 10L * 60 * 1000

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var lastAvailableVersionCode = 0

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private var lastCheckStartedAt = 0L

    // onCreate, first composition and the first ON_RESUME all fire within the same second on
    // launch; one network check is enough for all of them.
    private const val RECENT_CHECK_WINDOW_MS = 30_000L

    private suspend fun clearCheckingState() {
        withContext(Dispatchers.Main) {
            if (_updateState.value is UpdateState.Checking) _updateState.value = UpdateState.Idle
        }
    }

    /**
     * Each operation gets a fresh scope so destroying an Activity can never
     * permanently cancel the singleton's coroutines.
     */
    fun checkForUpdates(context: Context, force: Boolean = false) {
        if (!force && System.currentTimeMillis() - lastCheckStartedAt < RECENT_CHECK_WINDOW_MS) return
        if (_updateState.value is UpdateState.Checking ||
            _updateState.value is UpdateState.Downloading ||
            _updateState.value is UpdateState.DownloadComplete ||
            _updateState.value is UpdateState.UpdateAvailable
        ) return
        lastCheckStartedAt = System.currentTimeMillis()
        _updateState.value = UpdateState.Checking

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val currentVersionCode = getPackageInfo(context).first

                val request = Request.Builder()
                    .url(VERSION_CHECK_URL)
                    .cacheControl(okhttp3.CacheControl.FORCE_NETWORK)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        clearCheckingState()
                        return@launch
                    }
                    val body = response.body?.string() ?: run {
                        clearCheckingState()
                        return@launch
                    }
                    val json = JSONObject(body)

                    val latestVersionCode = json.optInt("versionCode", 0)
                    val latestVersionName = json.optString("versionName", "Unknown")
                    val downloadUrl = json.optString("downloadUrl", "")
                    val changelog = json.optString("changelog", "")
                    val mandatory = json.optBoolean("mandatory", false)

                    // Don't nag about a version the user already dismissed
                    val dismissed = getDismissedCode(context)
                    val shouldPrompt = latestVersionCode > dismissed

                    val result = if (shouldPrompt &&
                        latestVersionCode > currentVersionCode && downloadUrl.isNotEmpty()
                    ) {
                        lastAvailableVersionCode = latestVersionCode
                        UpdateState.UpdateAvailable(
                            latestVersion = latestVersionName,
                            latestVersionCode = latestVersionCode,
                            downloadUrl = downloadUrl,
                            changelog = changelog,
                            mandatory = mandatory
                        )
                    } else {
                        UpdateState.Idle
                    }
                    withContext(Dispatchers.Main) { _updateState.value = result }
                }
            } catch (e: Exception) {
                clearCheckingState()
            }
        }
    }

    /** Downloads the latest APK via [DownloadManager], reporting progress. */
    fun downloadUpdate(context: Context, downloadUrl: String) {
        val uri = runCatching { Uri.parse(downloadUrl) }.getOrNull()
        val expectedHost = Uri.parse(BASE_URL).host
        if (uri?.scheme != "https" || uri.host != expectedHost) {
            _updateState.value = UpdateState.Failed("Update source is not trusted")
            return
        }
        _updateState.value = UpdateState.Downloading(0)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates")
        if (!downloadDir.exists()) downloadDir.mkdirs()
        val targetFile = File(downloadDir, APK_FILE_NAME)
        if (targetFile.exists()) targetFile.delete()

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("Updating Vista Watch")
            .setDescription("Downloading latest version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(targetFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setMimeType("application/vnd.android.package-archive")

        val id = downloadManager.enqueue(request)
        val query = DownloadManager.Query().setFilterById(id)

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            var downloading = true
            val deadline = System.currentTimeMillis() + TEN_MINUTES_MS
            while (downloading) {
                if (System.currentTimeMillis() > deadline) {
                    downloading = false
                    withContext(Dispatchers.Main) { _updateState.value = UpdateState.Failed("Download timed out") }
                    break
                }
                var sawRow = false
                val cursor: Cursor = downloadManager.query(query)
                try {
                    if (cursor.moveToFirst()) {
                        sawRow = true
                        val status = cursor.getInt(
                            cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                        )
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                downloading = false
                                withContext(Dispatchers.Main) {
                                    _updateState.value = UpdateState.DownloadComplete(targetFile)
                                }
                            }
                            DownloadManager.STATUS_FAILED -> {
                                downloading = false
                                withContext(Dispatchers.Main) {
                                    _updateState.value = UpdateState.Failed("Download failed")
                                }
                            }
                            // STATUS_RUNNING / STATUS_PENDING
                            else -> {
                                val received = cursor.getInt(
                                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                )
                                val total = cursor.getInt(
                                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                                )
                                val progress = if (total > 0) (received * 100 / total).toInt().coerceIn(0, 99) else 0
                                withContext(Dispatchers.Main) {
                                    if (_updateState.value is UpdateState.Downloading) {
                                        _updateState.value = UpdateState.Downloading(progress)
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    downloading = false
                    withContext(Dispatchers.Main) { _updateState.value = UpdateState.Failed("Download interrupted") }
                } finally {
                    cursor.close()
                }
                if (downloading && !sawRow) {
                    downloading = false
                    withContext(Dispatchers.Main) { _updateState.value = UpdateState.Failed("Download was cancelled") }
                }
                if (downloading) delay(1000)
            }
        }
    }

    /**
     * Launches the system package installer. Returns false when the app is not
     * allowed to install packages; callers should direct the user to settings.
     */
    fun installUpdate(context: Context, file: File): Boolean {
        if (!hasInstallPermission(context)) return false

        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun hasInstallPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    /** Opens the "Install unknown apps" settings screen for this app. */
    fun openInstallSettings(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        try {
            val intent = android.content.Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                Uri.parse("package:${context.packageName}")
            )
            fallback.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallback)
        }
    }

    fun dismissUpdate(context: Context) {
        if (lastAvailableVersionCode > 0) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_DISMISSED_CODE, lastAvailableVersionCode)
                .apply()
        }
        _updateState.value = UpdateState.Idle
    }

    private fun getDismissedCode(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_DISMISSED_CODE, 0)
    }

    /** Returns (versionCode, versionName). */
    private fun getPackageInfo(context: Context): Pair<Int, String> {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(info).toInt() to (info.versionName ?: "1.0")
        } catch (e: Exception) {
            1 to "1.0"
        }
    }
}
