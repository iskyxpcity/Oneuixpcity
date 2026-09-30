package com.oneuixp.app.ui.util.update

data class LatestVersionInfo(
    val versionCode: Int = 0,
    val downloadUrl: String = "",
    val changelog: String = "",
    val versionName: String = "",
    val sha256: String = "",
)

private fun parseOneuiXPVersion(value: String): List<Int>? {
    val parts = value.trim().removePrefix("v").split(".")
    if (parts.size != 3) return null
    val numbers = parts.map { it.toIntOrNull() ?: return null }
    val (major, minor, date) = numbers
    if (major !in 0..20 || minor !in 0..99 || date !in 0..999999) return null
    return numbers
}

/**
 * Compare release names such as 1.0.260929.
 * Android versionCode is only the monotonic installer code and intentionally
 * does not participate in the network release comparison.
 */
fun compareOneuiXPVersions(left: String, right: String): Int {
    val lhs = parseOneuiXPVersion(left) ?: return 0
    val rhs = parseOneuiXPVersion(right) ?: return 0
    for (index in lhs.indices) {
        val compared = lhs[index].compareTo(rhs[index])
        if (compared != 0) return compared
    }
    return 0
}
