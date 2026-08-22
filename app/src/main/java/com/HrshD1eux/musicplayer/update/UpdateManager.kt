/*
 * Copyright (c) 2026 Music Player Project
 * UpdateManager.kt is part of Music Player.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package com.HrshD1eux.musicplayer.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.HrshD1eux.musicplayer.BuildConfig
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed interface UpdateResult {
    data class Available(
        val version: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val fileName: String,
    ) : UpdateResult

    data object UpToDate : UpdateResult

    data class Error(val message: String) : UpdateResult
}

object UpdateManager {
    private const val GITHUB_API_URL =
        "https://api.github.com/repos/HrshD1eux/Music_Player/releases/latest"

    suspend fun checkForUpdates(currentVersion: String = BuildConfig.VERSION_NAME): UpdateResult =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(GITHUB_API_URL)
                connection =
                    (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10000
                        readTimeout = 10000
                        setRequestProperty("Accept", "application/vnd.github.v3+json")
                        setRequestProperty("User-Agent", "MusicPlayer-Android-App")
                    }

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext UpdateResult.Error(
                        "Server returned code ${connection.responseCode}"
                    )
                }

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val releaseNotes = json.optString("body", "No release notes provided.")
                val assets = json.optJSONArray("assets")

                var downloadUrl: String? = null
                var fileName: String? = null

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url")
                            fileName = name
                            break
                        }
                    }
                }

                if (downloadUrl == null) {
                    return@withContext UpdateResult.Error(
                        "No APK release asset found in latest release."
                    )
                }

                if (isNewerVersion(tagName, currentVersion)) {
                    UpdateResult.Available(
                        version = tagName,
                        releaseNotes = releaseNotes,
                        downloadUrl = downloadUrl,
                        fileName = fileName ?: "MusicPlayer-$tagName.apk",
                    )
                } else {
                    UpdateResult.UpToDate
                }
            } catch (e: Exception) {
                Log.e("UpdateManager", "Failed to check for updates: ", e)
                UpdateResult.Error(e.localizedMessage ?: "Network error checking for updates.")
            } finally {
                connection?.disconnect()
            }
        }

    fun isNewerVersion(remoteVer: String, localVer: String): Boolean {
        val cleanRemote = remoteVer.removePrefix("v").trim()
        val cleanLocal = localVer.removePrefix("v").split("-")[0].trim()

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = cleanLocal.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    fun startDownloadAndInstall(
        context: Context,
        downloadUrl: String,
        fileName: String,
        onDownloadStarted: () -> Unit = {},
    ) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val destinationFile =
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        val request =
            DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Music Player Update")
                setDescription("Downloading $fileName...")
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationUri(Uri.fromFile(destinationFile))
                setMimeType("application/vnd.android.package-archive")
            }

        val downloadId = downloadManager.enqueue(request)
        onDownloadStarted()

        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(recvContext: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                    if (id == downloadId) {
                        try {
                            context.unregisterReceiver(this)
                        } catch (_: Exception) {}

                        installApk(context, destinationFile)
                    }
                }
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED,
            )
        } else {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            )
        }
    }

    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent =
                    Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:${context.packageName}"),
                        )
                        .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(settingsIntent)
                return
            }
        }

        val apkUri =
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)

        val installIntent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        context.startActivity(installIntent)
    }
}
