package com.alohaoneui.app.ui.util.update

import android.app.PendingIntent
import android.content.Context
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.provider.Settings
import com.alohaoneui.app.R
import com.alohaoneui.app.alohaOneUIApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

private const val UPDATE_INSTALL_ACTION = "com.alohaoneui.app.action.UPDATE_INSTALL_STATUS"

sealed interface UpdateDownloadResult {
    data class Success(val apk: File) : UpdateDownloadResult
    data class Error(val message: String) : UpdateDownloadResult
}

sealed interface UpdateInstallResult {
    data object Submitted : UpdateInstallResult
    data object PermissionRequired : UpdateInstallResult
    data class Error(val message: String) : UpdateInstallResult
}

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

private fun signerDigests(info: PackageInfo): Set<String> =
    info.signingInfo
        ?.apkContentsSigners
        ?.map { sha256(it.toByteArray()) }
        ?.toSet()
        .orEmpty()

@Suppress("DEPRECATION")
private fun verifyDownloadedApk(
    context: Context,
    apk: File,
    expectedVersionName: String,
): String? {
    val packageManager = context.packageManager
    val archiveInfo = packageManager.getPackageArchiveInfo(
        apk.absolutePath,
        PackageManager.GET_SIGNING_CERTIFICATES,
    ) ?: return "下载的 APK 无法读取"

    if (archiveInfo.packageName != context.packageName) {
        return "更新包包名不匹配"
    }
    if (archiveInfo.versionName != expectedVersionName) {
        return "更新包版本不匹配"
    }

    val installedInfo = packageManager.getPackageInfo(
        context.packageName,
        PackageManager.GET_SIGNING_CERTIFICATES,
    )
    val installedSigners = signerDigests(installedInfo)
    val archiveSigners = signerDigests(archiveInfo)
    if (installedSigners.isEmpty() || archiveSigners.isEmpty() || installedSigners != archiveSigners) {
        return "更新包签名校验失败"
    }
    return null
}

suspend fun downloadAndVerifyUpdate(
    context: Context,
    info: LatestVersionInfo,
): UpdateDownloadResult = withContext(Dispatchers.IO) {
    if (info.downloadUrl.isBlank() || info.sha256.length != 64 || info.versionName.isBlank()) {
        return@withContext UpdateDownloadResult.Error("更新信息不完整")
    }

    runCatching {
        val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apk = File(updateDir, "OneUI_XP_${info.versionName}.apk")
        if (apk.exists()) apk.delete()

        val request = Request.Builder()
            .url(info.downloadUrl)
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "AlohaOneUI-Android")
            .build()

        val digest = MessageDigest.getInstance("SHA-256")
        alohaOneUIApp.okhttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext UpdateDownloadResult.Error("下载失败：HTTP ${response.code}")
            }
            FileOutputStream(apk).use { output ->
                response.body.byteStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                    output.fd.sync()
                }
            }
        }

        if (!apk.isFile || apk.length() <= 0L) {
            apk.delete()
            return@withContext UpdateDownloadResult.Error("下载的 APK 文件为空")
        }

        val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actualSha256.equals(info.sha256, ignoreCase = true)) {
            apk.delete()
            return@withContext UpdateDownloadResult.Error("更新包 SHA-256 校验失败")
        }

        val verificationError = verifyDownloadedApk(context, apk, info.versionName)
        if (verificationError != null) {
            apk.delete()
            return@withContext UpdateDownloadResult.Error(verificationError)
        }

        UpdateDownloadResult.Success(apk)
    }.getOrElse { throwable ->
        UpdateDownloadResult.Error(
            throwable.message?.takeIf { it.isNotBlank() } ?: "下载更新失败"
        )
    }
}

fun submitUpdateSession(context: Context, apk: File): UpdateInstallResult {
    val canInstall = runCatching {
        context.packageManager.canRequestPackageInstalls()
    }.getOrDefault(false)
    if (!canInstall) return UpdateInstallResult.PermissionRequired

    val packageInstaller = context.packageManager.packageInstaller
    var sessionId: Int? = null

    return try {
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        ).apply {
            setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
            }
        }

        sessionId = packageInstaller.createSession(params)
        packageInstaller.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("base.apk", 0, apk.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }

            // Deliver the installer result to the real foreground UI instead of a
            // transparent trampoline. MainActivity can then launch the system confirmation
            // while it is foregrounded and is itself relaunched on install success.
            val statusIntent = Intent(
                context,
                com.alohaoneui.app.ui.MainActivity::class.java,
            ).apply {
                action = UPDATE_INSTALL_ACTION
                putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                sessionId,
                statusIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(pendingIntent.intentSender)
        }
        UpdateInstallResult.Submitted
    } catch (throwable: Throwable) {
        sessionId?.let { id ->
            runCatching { packageInstaller.abandonSession(id) }
        }
        UpdateInstallResult.Error(
            throwable.message?.takeIf { it.isNotBlank() }
                ?: "无法创建系统安装会话"
        )
    }
}

fun openUnknownSourcesSettings(context: Context): Boolean = runCatching {
    val intent = Intent(
        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:${context.packageName}"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}.isSuccess

/**
 * Transparent activity used as the PackageInstaller status callback.
 *
 * This activity is launched by the system-owned PendingIntent, so it can handle the
 * required confirmation while foregrounded and open the app only for STATUS_SUCCESS.
 * Failed/cancelled installs are deliberately silent.
 */
private const val UPDATE_INSTALL_STATUS_TAG = "AlohaOneUI-UpdateInstall"

/**
 * Handles PackageInstaller's result on the real MainActivity. Avoid routing from a
 * transparent background trampoline because Android 14+ can block the second activity
 * launch that opens the native package-installer confirmation UI.
 */
fun handleUpdateInstallStatus(activity: Activity, resultIntent: Intent): Boolean {
    if (resultIntent.action != UPDATE_INSTALL_ACTION) return false

    // Consume the one-shot callback action, so returning from system installer doesn't
    // re-process the same result and re-open the confirmation or update dialog.
    activity.setIntent(Intent(activity, com.alohaoneui.app.ui.MainActivity::class.java))

    val status = resultIntent.getIntExtra(
        PackageInstaller.EXTRA_STATUS,
        PackageInstaller.STATUS_FAILURE,
    )
    val statusMessage = resultIntent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()
    Log.i(UPDATE_INSTALL_STATUS_TAG, "Installer callback status=$status message=$statusMessage")

    when (status) {
        PackageInstaller.STATUS_PENDING_USER_ACTION -> {
            val confirmation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                resultIntent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                resultIntent.getParcelableExtra(Intent.EXTRA_INTENT) as? Intent
            }

            if (confirmation == null) {
                Log.e(UPDATE_INSTALL_STATUS_TAG, "Installer returned no confirmation Intent")
                AlertDialog.Builder(activity)
                    .setTitle(R.string.update_dialog_install)
                    .setMessage(R.string.update_install_confirmation_missing_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            } else {
                confirmation.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP,
                )
                // Post until MainActivity has attached its window and is foreground-ready.
                activity.window.decorView.post {
                    try {
                        activity.startActivity(confirmation)
                    } catch (throwable: Throwable) {
                        Log.e(UPDATE_INSTALL_STATUS_TAG, "Could not open installer confirmation", throwable)
                        AlertDialog.Builder(activity)
                            .setTitle(R.string.update_dialog_install)
                            .setMessage(R.string.update_install_confirmation_fallback_message)
                            .setPositiveButton(R.string.confirm) { _, _ ->
                                runCatching { activity.startActivity(confirmation) }
                                    .onFailure {
                                        Log.e(UPDATE_INSTALL_STATUS_TAG, "Retry opening installer confirmation failed", it)
                                        AlertDialog.Builder(activity)
                                            .setTitle(R.string.update_dialog_install)
                                            .setMessage(R.string.update_install_confirmation_missing_message)
                                            .setPositiveButton(android.R.string.ok, null)
                                            .show()
                                    }
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }
                }
            }
        }

        PackageInstaller.STATUS_SUCCESS -> {
            // The PendingIntent has already brought MainActivity to the foreground.
            // That current activity is the post-install relaunch; do not start a second task.
            Log.i(UPDATE_INSTALL_STATUS_TAG, "Update installed successfully; MainActivity is resumed")
        }

        PackageInstaller.STATUS_FAILURE_ABORTED -> {
            // This commonly represents user cancellation. Keep cancellation silent.
            Log.i(UPDATE_INSTALL_STATUS_TAG, "Installer aborted; staying silent")
        }

        else -> {
            val details = statusMessage.ifBlank { "安装器返回状态码：$status" }
            Log.e(UPDATE_INSTALL_STATUS_TAG, "Update failed: $details")
            AlertDialog.Builder(activity)
                .setTitle(R.string.update_dialog_install)
                .setMessage(activity.getString(R.string.update_install_failed_message, details))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }
    return true
}
