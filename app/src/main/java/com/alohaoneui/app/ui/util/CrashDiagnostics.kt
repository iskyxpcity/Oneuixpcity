package com.alohaoneui.app.ui.util

import android.app.Application
import android.os.Process
import com.alohaoneui.app.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

/**
 * Captures uncaught exceptions from the AlohaOneUI app process.
 *
 * The record is written synchronously before delegating to Android's original
 * handler, so it survives the process being terminated. It is intentionally
 * kept until a later crash replaces it; exporting diagnostics never deletes it.
 */
object CrashDiagnostics {
    private const val FILE_NAME = "alohaoneui_last_uncaught_exception.txt"
    private const val MAX_REPORT_CHARS = 512 * 1024

    @Volatile
    private var installed = false

    @Synchronized
    fun install(application: Application) {
        if (installed) return

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        val crashHandler = Thread.UncaughtExceptionHandler { thread, throwable ->
            // Never let a diagnostic failure suppress Android's normal crash handling.
            runCatching {
                persistCrash(application.noBackupFilesDir, thread, throwable)
            }

            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }

        Thread.setDefaultUncaughtExceptionHandler(crashHandler)
        installed = true
    }

    private fun persistCrash(directory: File, thread: Thread, throwable: Throwable) {
        val stackWriter = StringWriter()
        PrintWriter(stackWriter).use { writer ->
            throwable.printStackTrace(writer)
        }

        val report = buildString {
            appendLine("AlohaOneUI app-process uncaught exception")
            appendLine("Captured: ${OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}")
            appendLine("Package: ${BuildConfig.APPLICATION_ID}")
            appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Process ID: ${Process.myPid()}")
            appendLine("Thread: ${thread.name} (id=${thread.id})")
            appendLine()
            appendLine("[Complete exception stack trace]")
            append(stackWriter.toString())
        }.let { value ->
            if (value.length <= MAX_REPORT_CHARS) value
            else value.take(MAX_REPORT_CHARS) + "\n[Stack trace truncated at $MAX_REPORT_CHARS characters]\n"
        }

        if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory) {
            error("Unable to create crash diagnostics directory")
        }

        val destination = File(directory, FILE_NAME)
        FileOutputStream(destination, false).use { stream ->
            stream.write(report.toByteArray(Charsets.UTF_8))
            stream.fd.sync()
        }
    }

    /** Returns the most recent saved app-process crash, if one exists. */
    fun readLatestCrash(applicationContext: android.content.Context): String? =
        runCatching {
            val file = File(applicationContext.noBackupFilesDir, FILE_NAME)
            if (!file.isFile || file.length() <= 0L) return@runCatching null
            file.inputStream().bufferedReader(Charsets.UTF_8).use { reader ->
                val content = reader.readText()
                if (content.length <= MAX_REPORT_CHARS) content
                else content.take(MAX_REPORT_CHARS) + "\n[Stored crash record truncated]\n"
            }
        }.getOrNull()
}
