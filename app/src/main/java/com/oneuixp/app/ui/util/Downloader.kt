package com.oneuixp.app.ui.util

import com.oneuixp.app.BuildConfig
import com.oneuixp.app.oneuiXPApp
import com.oneuixp.app.ui.util.update.LatestVersionInfo
import com.oneuixp.app.ui.util.update.compareOneuiXPVersions
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.Locale

private const val LEGACY_RELEASE_REPO = "iSkyJIE/OneuiXP-Releases"
private const val ALOHA_RELEASE_REPO = "iSkyJIE/AlohaOneUI"
private const val LEGACY_PACKAGE_NAME = "com.oneuixp.app"
private const val BRIDGE_VERSION_NAME = "1.0.261022"
private const val BRIDGE_VERSION_CODE = 13
private const val BRIDGE_APK_ASSET_NAME = "OneuiXP_1.0.261022.apk"
private val oneUiAlohaApkPrefix = Regex("""(?i)^(?:AlohaOneUI|OneUIAloha)(?:[._ -][A-Za-z0-9][A-Za-z0-9._ -]*)?\.apk$""")

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

/**
 * Accept official Aloha APK names for the release tag. Also accept the legacy
 * manually-uploaded alias AlohaOneUI.0.xxxxx.apk when the tag is 1.0.xxxxx.
 */
private fun isOneUiAlohaApkForVersion(assetName: String, versionName: String): Boolean {
    if (!oneUiAlohaApkPrefix.matches(assetName)) return false
    val canonicalNames = listOf(
        "AlohaOneUI_${versionName}.apk",
        "AlohaOneUI-${versionName}.apk",
        "AlohaOneUI ${versionName}.apk",
        "OneUIAloha_${versionName}.apk",
        "OneUIAloha-${versionName}.apk",
        "OneUIAloha ${versionName}.apk",
    ).toMutableSet()
    if (versionName.startsWith("1.")) {
        canonicalNames += "AlohaOneUI.${versionName.removePrefix("1.")}.apk"
    }
    val acceptedNames = canonicalNames.map { it.lowercase(Locale.ROOT) }.toSet()
    return assetName.lowercase(Locale.ROOT) in acceptedNames ||
        assetName.equals("AlohaOneUI.apk", ignoreCase = true) ||
        assetName.equals("OneUIAloha.apk", ignoreCase = true)
}

/** Fetch published Release entries including pre-releases; drafts are never used. */
private fun fetchReleaseList(repository: String): List<JSONObject> = runCatching {
    val endpoint = "https://api.github.com/repos/$repository/releases?per_page=30"
    oneuiXPApp.okhttpClient.newCall(
        Request.Builder().url(endpoint)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "OneUI-Android-Updater")
            .build()
    ).execute().use { response ->
        if (!response.isSuccessful) {
            emptyList()
        } else {
            val releases = JSONArray(response.body.string())
            (0 until releases.length()).map { releases.getJSONObject(it) }
        }
    }
}.getOrDefault(emptyList())

private fun fetchLatestRelease(repository: String): JSONObject? = runCatching {
    val endpoint = "https://api.github.com/repos/$repository/releases/latest"
    oneuiXPApp.okhttpClient.newCall(
        Request.Builder().url(endpoint)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "OneUI-Android-Updater")
            .build()
    ).execute().use { response ->
        if (!response.isSuccessful) null else JSONObject(response.body.string())
    }
}.getOrNull()

/**
 * Resolve the one-time bridge update for legacy OneUI XP installations.
 *
 * Older installations must receive the bridge from the legacy release feed before they are ever
 * offered the separate Aloha OneUI app. The exact versioned asset name is required because this
 * is the filename recognized by the legacy updater.
 */
private fun fetchLegacyBridgeUpdate(): LatestVersionInfo {
    val release = fetchReleaseList(LEGACY_RELEASE_REPO).firstOrNull {
        !it.optBoolean("draft") &&
            it.optString("tag_name").removePrefix("v").removePrefix("V") == BRIDGE_VERSION_NAME
    } ?: return LatestVersionInfo()

    val assets = release.optJSONArray("assets") ?: return LatestVersionInfo()
    val asset = (0 until assets.length())
        .map { assets.getJSONObject(it) }
        .firstOrNull {
            it.optString("name").equals(BRIDGE_APK_ASSET_NAME, ignoreCase = true) &&
                isOfficialReleaseAssetUrl(
                    it.optString("browser_download_url"),
                    LEGACY_RELEASE_REPO,
                )
        } ?: return LatestVersionInfo()

    val digest = asset.optString("digest")
        .removePrefix("sha256:")
        .lowercase(Locale.ROOT)
    // Without the digest of the exact published APK, don't offer an update that cannot be verified.
    if (!digest.matches(Regex("[0-9a-f]{64}"))) return LatestVersionInfo()

    return LatestVersionInfo(
        versionCode = BRIDGE_VERSION_CODE,
        versionName = BRIDGE_VERSION_NAME,
        downloadUrl = asset.optString("browser_download_url"),
        changelog = release.optString("body"),
        sha256 = digest,
    )
}

/**
 * Legacy versions below 1.0.261022 can only receive the bridge from the legacy release feed.
 * Once the bridge is installed, the same package name switches to the Aloha OneUI migration feed.
 */
fun checkNewVersion(): LatestVersionInfo {
    if (!isNetworkAvailable(oneuiXPApp)) return LatestVersionInfo()

    val isLegacyApp = oneuiXPApp.packageName.equals(LEGACY_PACKAGE_NAME, ignoreCase = true)

    if (isLegacyApp && compareOneuiXPVersions(BuildConfig.VERSION_NAME, BRIDGE_VERSION_NAME) < 0) {
        return fetchLegacyBridgeUpdate()
    }

    if (isLegacyApp) {
        // The current public feed may mark a manually uploaded formal APK as a pre-release.
        // /releases/latest returns 404 in that case, so scan published entries instead.
        val alohaRelease = fetchReleaseList(ALOHA_RELEASE_REPO)
            .asSequence()
            .filterNot { it.optBoolean("draft") }
            .filter {
                isSupportedVersionName(
                    it.optString("tag_name").removePrefix("v").removePrefix("V")
                )
            }
            .sortedWith(Comparator { left, right ->
                val leftVersion = left.optString("tag_name").removePrefix("v").removePrefix("V")
                val rightVersion = right.optString("tag_name").removePrefix("v").removePrefix("V")
                compareOneuiXPVersions(rightVersion, leftVersion)
            })
            .firstOrNull { release ->
                val versionName = release.optString("tag_name").removePrefix("v").removePrefix("V")
                val assets = release.optJSONArray("assets") ?: return@firstOrNull false
                (0 until assets.length()).any { index ->
                    val asset = assets.getJSONObject(index)
                    isOneUiAlohaApkForVersion(asset.optString("name"), versionName) &&
                        isOfficialReleaseAssetUrl(
                            asset.optString("browser_download_url"),
                            ALOHA_RELEASE_REPO,
                        )
                }
            }

        if (alohaRelease != null) {
            val versionName = alohaRelease.optString("tag_name")
                .removePrefix("v").removePrefix("V")
            val assets = alohaRelease.optJSONArray("assets")
            if (assets != null) {
                val migrationAsset = (0 until assets.length())
                    .map { assets.getJSONObject(it) }
                    .firstOrNull { asset ->
                        isOneUiAlohaApkForVersion(asset.optString("name"), versionName) &&
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
        // After the bridge is installed, never fall back to the legacy feed; Aloha OneUI is the
        // only intended update path for this package.
        return LatestVersionInfo()
    }

    val releaseRepository = ALOHA_RELEASE_REPO
    val release = fetchLatestRelease(releaseRepository) ?: return LatestVersionInfo()
    val versionName = release.optString("tag_name").removePrefix("v").removePrefix("V")
    if (!isSupportedVersionName(versionName)) return LatestVersionInfo()
    if (compareOneuiXPVersions(versionName, BuildConfig.VERSION_NAME) <= 0) {
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
        "OneuiXP_${versionName}.apk",
    ).map { it.lowercase(Locale.ROOT) }.toSet()
    val apkAsset = (0 until assets.length())
        .map { assets.getJSONObject(it) }
        .firstOrNull { asset ->
            val name = asset.optString("name")
            val correctName = if (isLegacyApp) {
                name.lowercase(Locale.ROOT) in expectedLegacyNames
            } else {
                isOneUiAlohaApkForVersion(name, versionName)
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
