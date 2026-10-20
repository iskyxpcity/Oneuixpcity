package com.oneuixp.app.ui.util

import com.oneuixp.app.BuildConfig
import com.oneuixp.app.oneuiXPApp
import com.oneuixp.app.ui.util.update.LatestVersionInfo
import com.oneuixp.app.ui.util.update.compareOneuiXPVersions
import okhttp3.Request
import java.net.URI
import java.util.Locale

private const val PUBLIC_RELEASE_REPO = "iSkyJIE/OneuiXP-Releases"
private const val LEGACY_PACKAGE_NAME = "com.oneuixp.app"
private val oneAlohaApkName = Regex("""(?i)^OneALOHA(?:[_ -][A-Za-z0-9][A-Za-z0-9._ -]*)?\.apk$""")

/** Only assets hosted by the official public release repository are trusted. */
private fun isOfficialReleaseAssetUrl(value: String): Boolean = runCatching {
    val uri = URI(value)
    uri.scheme?.equals("https", ignoreCase = true) == true &&
        uri.host?.equals("github.com", ignoreCase = true) == true &&
        uri.path.orEmpty().lowercase(Locale.ROOT)
            .startsWith("/iskyjie/oneuixp-releases/releases/download/")
}.getOrDefault(false)

/** Checks the official public feed. Older OneUI XP installs may be offered a separate-app migration. */
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
            val versionName = release.optString("tag_name").removePrefix("v").removePrefix("V")
            val assets = release.optJSONArray("assets") ?: return@use LatestVersionInfo()
            val parsedAssets = (0 until assets.length()).map { assets.getJSONObject(it) }

            // A different package ID cannot update com.oneuixp.app in place. Offer the new
            // product as a migration download before comparing its version with the old app.
            if (oneuiXPApp.packageName.equals(LEGACY_PACKAGE_NAME, ignoreCase = true)) {
                val migrationAsset = parsedAssets.firstOrNull { asset ->
                    oneAlohaApkName.matches(asset.optString("name")) &&
                        isOfficialReleaseAssetUrl(asset.optString("browser_download_url"))
                }
                if (migrationAsset != null) {
                    val fallbackVersion = migrationAsset.optString("name")
                        .removeSuffix(".apk")
                        .removePrefix("OneALOHA_")
                        .removePrefix("OneALOHA-")
                    return@use LatestVersionInfo(
                        versionName = versionName.ifBlank { fallbackVersion },
                        downloadUrl = migrationAsset.optString("browser_download_url"),
                        changelog = release.optString("body"),
                        isMigration = true,
                    )
                }
            }

            // Supports the legacy date-based 1.0.261020[.patch] format and standard numeric
            // versions such as 2.0.0, so the renamed product can continue using this updater.
            if (compareOneuiXPVersions(versionName, BuildConfig.VERSION_NAME) <= 0) {
                return@use LatestVersionInfo()
            }
            val components = versionName.split(".").map {
                it.toIntOrNull() ?: return@use LatestVersionInfo()
            }
            if (components.size !in 3..4) return@use LatestVersionInfo()
            val comparableCode = (
                components[0].toLong() * 100_000_000L +
                    components[1].toLong() * 1_000_000L +
                    components[2].toLong() +
                    components.getOrElse(3) { 0 }.toLong()
                ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

            val expectedApkNames = setOf(
                "OneUI_XP_${versionName}.apk",
                "OneuiXP_${versionName}.apk", // Keep already-published legacy assets compatible.
                "OneALOHA_${versionName}.apk",
            ).map { it.lowercase(Locale.ROOT) }.toSet()
            val apkAsset = parsedAssets.firstOrNull { asset ->
                asset.optString("name").lowercase(Locale.ROOT) in expectedApkNames &&
                    isOfficialReleaseAssetUrl(asset.optString("browser_download_url"))
            } ?: return@use LatestVersionInfo()

            val digest = apkAsset.optString("digest")
                .removePrefix("sha256:")
                .lowercase(Locale.ROOT)
            if (!digest.matches(Regex("[0-9a-f]{64}"))) return@use LatestVersionInfo()

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
