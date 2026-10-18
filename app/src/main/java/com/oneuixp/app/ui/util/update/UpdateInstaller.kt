package com.oneuixp.app.ui.util.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.oneuixp.app.oneuiXPApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

private const val UPDATE_INSTALL_ACTION = "com.oneuixp.app.action.UPDATE_INSTALL_STATUS"

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
        val apk = File(updateDir, "OneuiXP_${info.versionName}.apk")
        if (apk.exists()) apk.delete()

        val request = Request.Builder()
            .url(info.downloadUrl)
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "OneuiXP-Android")
            .build()

        val digest = MessageDigest.getInstance("SHA-256")
        oneuiXPApp.okhttpClient.newCall(request).execute().use { response ->
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

            val statusIntent = Intent(context, UpdateInstallReceiver::class.java).apply {
                action = UPDATE_INSTALL_ACTION
                putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
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

class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != UPDATE_INSTALL_ACTION) return

        when (val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE,
        )) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT) as? Intent
                }

                if (confirmation == null) {
                    Toast.makeText(
                        context,
                        "系统没有返回安装确认界面",
                        Toast.LENGTH_LONG,
                    ).show()
                    return
                }

                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val opened = runCatching { context.startActivity(confirmation) }.isSuccess
                if (!opened) {
                    Toast.makeText(
                        context,
                        "无法打开 Samsung 系统安装确认界面",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                val relaunched = launchIntent?.let { launch ->
                    launch.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    )
                    runCatching {
                        context.startActivity(launch)
                        true
                    }.getOrDefault(false)
                } == true
                val message = if (relaunched) {
                    "OneuiXP 更新安装完成，正在重新打开应用"
                } else {
                    "OneuiXP 更新安装完成，请返回桌面重新打开应用"
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }

            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    ?.takeIf { it.isNotBlank() }
                    ?: "安装失败（状态码：$status）"
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}
