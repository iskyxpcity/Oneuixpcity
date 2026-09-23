package me.weishu.kernelsu.ui.util

import android.net.Uri
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.util.module.LatestVersionInfo
import okhttp3.Request

/**
 * @author weishu
 * @date 2023/6/22.
 */
suspend fun download(
    url: String,
    fileName: String,
    onDownloaded: (Uri) -> Unit = {},
    onDownloading: () -> Unit = {},
    onProgress: (Int) -> Unit = {}
) {
    onDownloading()

    val downloadId = DownloadManager.enqueue(
        context = ksuApp,
        url = url,
        fileName = fileName,
        onCompleted = onDownloaded,
    )

    DownloadManager.downloads
        .onEach { map -> map[downloadId]?.let { onProgress(it.progress) } }
        .first { map ->
            val status = map[downloadId]?.status
            status == DownloadManager.Status.COMPLETED ||
                status == DownloadManager.Status.FAILED
        }
}

/**
 * The manager is OneuiXP, not KernelSU. Use this project's Releases only.
 * Release tags: v1.0.260923, v1.0.260924, ...
 */
fun checkNewVersion(): LatestVersionInfo {
    if (!isNetworkAvailable(ksuApp)) return LatestVersionInfo()
    val endpoint = "https://api.github.com/repos/iSkyJIE/OneuiXP/releases/latest"
    return runCatching {
        ksuApp.okhttpClient.newCall(
            Request.Builder().url(endpoint)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "OneuiXP-Android")
                .build()
        ).execute().use { response ->
            if (!response.isSuccessful) return@use LatestVersionInfo()
            val release = org.json.JSONObject(response.body.string())
            val versionName = release.optString("tag_name").removePrefix("v")
            val components = versionName.split(".").mapNotNull { it.toIntOrNull() }
            if (components.size != 3) return@use LatestVersionInfo()
            val (major, minor, date) = components
            if (major !in 0..20 || minor !in 0..99 || date !in 0..999999) {
                return@use LatestVersionInfo()
            }
            val versionCode = major * 100_000_000 + minor * 1_000_000 + date
            if (versionCode <= me.weishu.kernelsu.BuildConfig.VERSION_CODE) {
                return@use LatestVersionInfo()
            }
            val assets = release.optJSONArray("assets")
            var downloadUrl = release.optString(
                "html_url", "https://github.com/iSkyJIE/OneuiXP/releases"
            )
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true) &&
                        name.contains("OneuiXP", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", downloadUrl)
                        break
                    }
                }
            }
            LatestVersionInfo(
                versionCode = versionCode,
                versionName = versionName,
                downloadUrl = downloadUrl,
                changelog = release.optString("body"),
            )
        }
    }.getOrDefault(LatestVersionInfo())
}
