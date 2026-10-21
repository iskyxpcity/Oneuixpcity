package com.alohaoneui.app.ui.util.update

data class LatestVersionInfo(
    val versionCode: Int = 0,
    val downloadUrl: String = "",
    val changelog: String = "",
    val versionName: String = "",
    val sha256: String = "",
    /** True when the URL installs a different package and must be opened externally. */
    val isMigration: Boolean = false,
)

private fun parseVersion(value: String): List<Int>? {
    val normalized = value.trim().removePrefix("v").removePrefix("V")
    val parts = normalized.split(".")
    if (parts.size !in 3..4) return null
    return parts.map { it.toIntOrNull() ?: return null }
}

/**
 * Compare numeric three- or four-part versions. Supports the legacy date-based
 * names (1.0.261019[.1]) and standard names such as 2.0.0 after a product rename.
 * Android versionCode remains independent from the network release comparison.
 */
fun compareAlohaOneUIVersions(left: String, right: String): Int {
    val lhs = parseVersion(left) ?: return 0
    val rhs = parseVersion(right) ?: return 0
    val length = maxOf(lhs.size, rhs.size)
    for (index in 0 until length) {
        val compared = lhs.getOrElse(index) { 0 }.compareTo(rhs.getOrElse(index) { 0 })
        if (compared != 0) return compared
    }
    return 0
}
