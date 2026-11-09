package com.alohaoneui.app.ui.util

import android.app.Application
import android.os.Process
import android.util.Log
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
 * The handler writes to two independent app-private locations before delegating
 * to Android's original handler. Diagnostic exports read either surviving copy.
 * Exporting or reopening the app never deletes a crash record.
 */
object CrashDiagnostics {
    private const val TAG = "AlohaCrashDiagnostics"
    private const val CRASH_FILE = "alohaoneui_last_uncaught_exception.txt"
    private const val STATUS_FILE = "alohaoneui_crash_capture_status.txt"
    private const val FAILURE_FILE = "alohaoneui_crash_capture_failure.txt"
    private const val MAX_REPORT_CHARS = 512 * 1024

    @Volatile
    private var installed = false

    @Synchronized
    fun install(application: Application) {
        if (installed) return

        val installStatus = buildString {
            appendLine("Crash diagnostics initialization")
            appendLine("Initialized: ${now()}")
            appendLine("Package: ${BuildConfig.APPLICATION_ID}")
            appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Process ID: ${Process.myPid()}")
            appendLine("Default handler before install: ${Thread.getDefaultUncaughtExceptionHandler()?.javaClass?.name ?: "none"}")
        }

        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        val crashHandler = Thread.UncaughtExceptionHandler { thread, throwable ->
            val errors = persistCrash(application, thread, throwable)
            if (errors.isNotEmpty()) {
                val failure = buildString {
                    appendLine("Crash handler was invoked, but one or more crash-record writes failed.")
                    appendLine("Captured: ${now()}")
                    appendLine("Package: ${BuildConfig.APPLICATION_ID}")
                    appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    errors.forEach { appendLine("- $it") }
                }
                Log.e(TAG, failure, throwable)
                persistAuxiliaryFile(application, FAILURE_FILE, failure)
            }

            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }

        Thread.setDefaultUncaughtExceptionHandler(crashHandler)
        installed = Thread.getDefaultUncaughtExceptionHandler() === crashHandler

        val status = installStatus + buildString {
            appendLine("Handler registration verified: $installed")
            appendLine("Installed handler: ${Thread.getDefaultUncaughtExceptionHandler()?.javaClass?.name ?: "none"}")
            appendLine("Storage targets: noBackupFilesDir and filesDir")
        }
        persistAuxiliaryFile(application, STATUS_FILE, status)
        if (!installed) Log.e(TAG, "Failed to register the default uncaught exception handler.")
    }

    private fun persistCrash(application: Application, thread: Thread, throwable: Throwable): List<String> {
        val stackWriter = StringWriter()
        PrintWriter(stackWriter).use { writer -> throwable.printStackTrace(writer) }

        val report = buildString {
            appendLine("AlohaOneUI app-process uncaught exception")
            appendLine("Captured: ${now()}")
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

        val errors = mutableListOf<String>()
        storageDirectories(application).forEach { directory ->
            try {
                writeAtomically(File(directory, CRASH_FILE), report)
            } catch (error: Exception) {
                errors += "${directory.absolutePath}: ${error.javaClass.simpleName}: ${error.message.orEmpty()}"
            }
        }
        return errors
    }

    private fun persistAuxiliaryFile(application: Application, name: String, content: String) {
        storageDirectories(application).forEach { directory ->
            runCatching { writeAtomically(File(directory, name), content) }
                .onFailure { Log.e(TAG, "Unable to write $name to ${directory.absolutePath}", it) }
        }
    }

    private fun storageDirectories(application: Application): List<File> =
        listOf(application.noBackupFilesDir, application.filesDir).distinctBy { it.absolutePath }

    private fun writeAtomically(destination: File, content: String) {
        val directory = destination.parentFile
            ?: error("Missing parent directory for ${destination.absolutePath}")
        if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory) {
            error("Unable to create directory ${directory.absolutePath}")
        }

        val temporary = File(directory, "${destination.name}.tmp")
        FileOutputStream(temporary, false).use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
            stream.fd.sync()
        }
        if (!temporary.renameTo(destination)) {
            // Keep a direct-write fallback for filesystems where rename replacement fails.
            FileOutputStream(destination, false).use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
                stream.fd.sync()
            }
            temporary.delete()
        }
    }

    private fun now(): String = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    /** Returns the newest surviving app-process crash record, if one exists. */
    fun readLatestCrash(applicationContext: android.content.Context): String? {
        val files = listOf(
            File(applicationContext.noBackupFilesDir, CRASH_FILE),
            File(applicationContext.filesDir, CRASH_FILE),
        ).filter { it.isFile && it.length() > 0L }
        val file = files.maxByOrNull { it.lastModified() } ?: return null
        return runCatching {
            file.inputStream().bufferedReader(Charsets.UTF_8).use { reader ->
                val content = reader.readText()
                buildString {
                    appendLine("Record source: ${file.absolutePath}")
                    append(content.take(MAX_REPORT_CHARS))
                    if (content.length > MAX_REPORT_CHARS) appendLine("\n[Stored crash record truncated]")
                }
            }
        }.getOrNull()
    }

    /** Reports handler initialization and any previously recorded persistence failure. */
    fun readCaptureStatus(applicationContext: android.content.Context): String {
        val roots = listOf(applicationContext.noBackupFilesDir, applicationContext.filesDir)
        val status = roots.map { File(it, STATUS_FILE) }
            .filter { it.isFile && it.length() > 0L }
            .maxByOrNull { it.lastModified() }
        val failure = roots.map { File(it, FAILURE_FILE) }
            .filter { it.isFile && it.length() > 0L }
            .maxByOrNull { it.lastModified() }

        return buildString {
            appendLine("Handler status:")
            if (status == null) appendLine("No initialization status file found.")
            else append(status.readText(Charsets.UTF_8))
            appendLine()
            appendLine("Last crash-record write failure:")
            if (failure == null) appendLine("No crash-record write failure has been recorded.")
            else append(failure.readText(Charsets.UTF_8))
        }
    }
}
