plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.compose.compiler) apply false
}

extra["androidMinSdkVersion"] = 31
extra["androidTargetSdkVersion"] = 36
extra["androidCompileSdkVersion"] = 37
extra["androidCompileSdkVersionMinor"] = 0
extra["androidBuildToolsVersion"] = "37.0.0"
extra["androidCompileNdkVersion"] = libs.versions.ndk.get()
extra["androidSourceCompatibility"] = JavaVersion.VERSION_21
extra["androidTargetCompatibility"] = JavaVersion.VERSION_21
extra["managerVersionCode"] = getVersionCode()
extra["managerVersionName"] = getVersionName()

fun getGitCommitCount(): Int {
    val process = Runtime.getRuntime().exec(arrayOf("git", "rev-list", "--count", "HEAD"))
    return process.inputStream.bufferedReader().use { it.readText().trim().toInt() }
}

fun getGitDescribe(): String {
    val process = Runtime.getRuntime().exec(arrayOf("git", "describe", "--tags", "--always"))
    return process.inputStream.bufferedReader().use { it.readText().trim() }
}

/**
 * OneuiXP has not made a formal public release yet.
 * Keep the explicit versionCode small and monotonic, starting at 1.
 * Increment oneuixpVersionCode in gradle.properties for future releases.
 */
fun getVersionCode(): Int {
    val explicit = (findProperty("oneuixpVersionCode") as? String)?.toIntOrNull()
    if (explicit != null && explicit > 0) return explicit

    val parts = getVersionName().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
    if (parts.size == 3) {
        val (major, minor, date) = parts
        if (major in 0..20 && minor in 0..99 && date in 0..999999) {
            return major * 100_000_000 + minor * 1_000_000 + date
        }
    }
    return 30000 + getGitCommitCount()
}

fun getVersionName(): String {
    return (findProperty("oneuixpVersionName") as? String)?.takeIf { it.isNotBlank() }
        ?: getGitDescribe()
}
