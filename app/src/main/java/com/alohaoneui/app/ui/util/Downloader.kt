package com.alohaoneui.app.ui.util

import com.alohaoneui.app.BuildConfig
import com.alohaoneui.app.alohaOneUIApp
import com.alohaoneui.app.ui.util.update.LatestVersionInfo
import com.alohaoneui.app.ui.util.update.compareAlohaOneUIVersions
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.util.Locale

private const val LEGACY_RELEASE_REPO = "iSkyJIE/OneuiXP-Releases"
private const val ALOHA_RELEASE_REPO = "iSkyJIE/AlohaOneUI"
private const val LEGACY_PACKAGE_NAME = "com.oneuixp.app"
private val supportedAlohaApkPrefix = Regex("""(?i)^(?:AlohaOneUI|OneUIAloha)(?:[_ -][A-Za-z0-9][A-Za-z0-9._ -]*)?\.apk$""")

private fun isSupportedVersionName(value: String): Boolean =
    value.trim().removePrefix("v").removePrefix("V")
        .matches(Regex("""\d+(?:\.\d+){2,3}"""))

/** Only HTTPS download assets from the configured official release repository are trusted. */
private fun isOfficialReleaseAssetUrl(value: String, repository: String): Boolean = runCatching {
    val parts = repository.split("/", limit = 2)
    if (parts.size != 2) return false
    val expectedPrefix = "/${parts[0].lowercase(Locale.ROOT)}/${parts[1].lowercase(Locale.ROOT)}/releases/download/"
    val uri = URI(value)
    uri.scheme?.equals("https", ignoreCase = true) == true &&
        uri.host?.equals("github.com", ignoreCase = true) == true &&
        uri.path.orEmpty().lowercase(Locale.ROOT).startsWith(expectedPrefix)
}.getOrDefault(false)

/** Accept both new AlohaOneUI APK names and legacy OneUIAloha names, case-insensitively. */
private fun isSupportedAlohaApkForVersion(assetName: String, versionName: String): Boolean {
    if (!supportedAlohaApkPrefix.matches(assetName)) return false
    val versionedName = Regex(
        """(?i)^(?:AlohaOneUI|OneUIAloha)[_ -]${Regex.escape(versionName)}\.apk$"""
    )
    return assetName.equals("AlohaOneUI.apk", ignoreCase = true) ||
        assetName.equals("OneUIAloha.apk", ignoreCase = true) ||
        versionedName.matches(assetName)
}

private fun fetchLatestRelease(repository: String): JSONObject? = runCatching {
    val endpoint = "https://api.github.com/repos/$repository/releases/latest"
    alohaOneUIApp.okhttpClient.newCall(
        Request.Builder().url(endpoint)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Aloha-OneUI-Android-Updater")
            .build()
    ).execute().use { response ->
        if (!response.isSuccessful) null else JSONObject(response.body.string())
    }
}.getOrNull()

/**
 * Old OneUI XP builds first look for a stable Aloha OneUI release and offer it as a separate-app
 * migration. If no Aloha OneUI release/asset is published yet, they continue checking the legacy
 * OneUI XP feed. Once the app package is renamed, this same updater uses the Aloha OneUI feed.
 */
fun checkNewVersion(): LatestVersionInfo {
    if (!isNetworkAvailable(alohaOneUIApp)) return LatestVersionInfo()

    val isLegacyApp = alohaOneUIApp.packageName.equals(LEGACY_PACKAGE_NAME, ignoreCase = true)

    if (isLegacyApp) {
        val alohaRelease = fetchLatestRelease(ALOHA_RELEASE_REPO)
        if (alohaRelease != null) {
            val versionName = alohaRelease.optString("tag_name")
                .removePrefix("v").removePrefix("V")
            val assets = alohaRelease.optJSONArray("assets")
            if (isSupportedVersionName(versionName) && assets != null) {
                val migrationAsset = (0 until assets.length())
                    .map { assets.getJSONObject(it) }
                    .firstOrNull { asset ->
                        isSupportedAlohaApkForVersion(asset.optString("name"), versionName) &&
                            isOfficialReleaseAssetUrl(
                                asset.optString("browser_download_url"),
                                ALOHA_RELEASE_REPO,
                            )
                    }
                if (migrationAsset != null) {
                    return LatestVersionInfo(
                        versionName = versionName,
                        downloadUrl = migrationAsset.optString("browser_download_url"),
                        changelog = alohaRelease.optString("body"),
                        isMigration = true,
                    )
                }
            }
        }
    }

    val releaseRepository = if (isLegacyApp) LEGACY_RELEASE_REPO else ALOHA_RELEASE_REPO
    val release = fetchLatestRelease(releaseRepository) ?: return LatestVersionInfo()
    val versionName = release.optString("tag_name").removePrefix("v").removePrefix("V")
    if (!isSupportedVersionName(versionName)) return LatestVersionInfo()
    if (compareAlohaOneUIVersions(versionName, BuildConfig.VERSION_NAME) <= 0) {
        return LatestVersionInfo()
    }

    val components = versionName.split(".").map {
        it.toIntOrNull() ?: return LatestVersionInfo()
    }
    if (components.size !in 3..4) return LatestVersionInfo()
    val comparableCode = (
        components[0].toLong() * 100_000_000L +
            components[1].toLong() * 1_000_000L +
            components[2].toLong() +
            components.getOrElse(3) { 0 }.toLong()
        ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    val assets = release.optJSONArray("assets") ?: return LatestVersionInfo()
    val expectedLegacyNames = setOf(
        "OneUI_XP_${versionName}.apk",
        "OneUIAloha_${versionName}.apk",
        "AlohaOneUI_${versionName}.apk",
    ).map { it.lowercase(Locale.ROOT) }.toSet()
    val apkAsset = (0 until assets.length())
        .map { assets.getJSONObject(it) }
        .firstOrNull { asset ->
            val name = asset.optString("name")
            val correctName = if (isLegacyApp) {
                name.lowercase(Locale.ROOT) in expectedLegacyNames
            } else {
                isSupportedAlohaApkForVersion(name, versionName)
            }
            correctName && isOfficialReleaseAssetUrl(
                asset.optString("browser_download_url"),
                releaseRepository,
            )
        } ?: return LatestVersionInfo()

    val digest = apkAsset.optString("digest")
        .removePrefix("sha256:")
        .lowercase(Locale.ROOT)
    if (!digest.matches(Regex("[0-9a-f]{64}"))) return LatestVersionInfo()

    return LatestVersionInfo(
        versionCode = comparableCode,
        versionName = versionName,
        downloadUrl = apkAsset.optString("browser_download_url"),
        changelog = release.optString("body"),
        sha256 = digest,
    )
}
