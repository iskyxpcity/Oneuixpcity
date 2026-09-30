package com.oneuixp.app.ui.util

import com.oneuixp.app.BuildConfig
import com.oneuixp.app.oneuiXPApp
import com.oneuixp.app.ui.util.update.LatestVersionInfo
import com.oneuixp.app.ui.util.update.compareOneuiXPVersions
import okhttp3.Request

private const val PUBLIC_RELEASE_REPO = "iSkyJIE/OneuiXP-Releases"

/** Only the public binary-release repository is checked. */
fun checkNewVersion(): LatestVersionInfo {
    if (!isNetworkAvailable(oneuiXPApp)) return LatestVersionInfo()
    val endpoint = "https://api.github.com/repos/$PUBLIC_RELEASE_REPO/releases/latest"
    return runCatching {
        oneuiXPApp.okhttpClient.newCall(
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

            if (compareOneuiXPVersions(versionName, BuildConfig.VERSION_NAME) <= 0) {
                return@use LatestVersionInfo()
            }

            val expectedApkName = "OneuiXP_${versionName}.apk"
            val assets = release.optJSONArray("assets") ?: return@use LatestVersionInfo()
            val apkAsset = (0 until assets.length())
                .map { assets.getJSONObject(it) }
                .firstOrNull { asset ->
                    asset.optString("name") == expectedApkName &&
                        asset.optString("browser_download_url").isNotBlank()
                } ?: return@use LatestVersionInfo()

            val digest = apkAsset.optString("digest")
                .removePrefix("sha256:")
                .lowercase()
            if (digest.length != 64) return@use LatestVersionInfo()

            val comparableCode = major * 100_000_000 + minor * 1_000_000 + date
            LatestVersionInfo(
                versionCode = comparableCode,
                versionName = versionName,
                downloadUrl = apkAsset.optString("browser_download_url"),
                changelog = release.optString("body"),
                sha256 = digest,
            )
        }
    }.getOrDefault(LatestVersionInfo())
}
