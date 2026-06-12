package com.example

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashReporter {
    private const val TAG = "CrashReporter"
    private const val DIRECTORY_NAME = "crash_reports"
    private const val MAX_REPORTS = 20

    fun getCrashReportsDirectory(context: Context): File {
        val dir = File(context.filesDir, DIRECTORY_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun saveCrashReport(context: Context, thread: Thread, throwable: Throwable) {
        try {
            val dir = getCrashReportsDirectory(context)
            
            // Enforce limit first
            cleanOldReports(dir)

            // Dynamic filename with safe timestamp
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
            val timestampStr = dateFormat.format(Date())
            val filename = "crash_${timestampStr}_${System.currentTimeMillis()}.txt"
            val reportFile = File(dir, filename)

            // Gather crash detailed information
            val stringWriter = StringWriter()
            throwable.printStackTrace(PrintWriter(stringWriter))
            val stackTrace = stringWriter.toString()

            val pm = context.packageManager
            var versionName = "unknown"
            var versionCode = -1L
            try {
                val packageInfo = pm.getPackageInfo(context.packageName, 0)
                versionName = packageInfo.versionName ?: "unknown"
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get package info", e)
            }

            val processName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                android.app.Application.getProcessName()
            } else {
                "unknown"
            }

            val builder = StringBuilder().apply {
                append("--- CRASH REPORT ---\n")
                append("Timestamp: ").append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())).append("\n")
                append("App Version Name: ").append(versionName).append("\n")
                append("App Version Code: ").append(versionCode).append("\n")
                append("Android SDK: ").append(Build.VERSION.SDK_INT).append("\n")
                append("Android OS Version: ").append(Build.VERSION.RELEASE).append("\n")
                append("Device Brand: ").append(Build.BRAND).append("\n")
                append("Device Manufacturer: ").append(Build.MANUFACTURER).append("\n")
                append("Device Model: ").append(Build.MODEL).append("\n")
                append("Process Name: ").append(processName).append("\n")
                append("Thread: ").append(thread.name).append(" (ID: ").append(thread.id).append(")\n")
                append("Exception Class: ").append(throwable.javaClass.name).append("\n")
                append("Exception Message: ").append(throwable.localizedMessage ?: "none").append("\n")
                append("\n--- STACK TRACE ---\n")
                append(stackTrace)
            }

            reportFile.writeText(builder.toString())
            Log.d(TAG, "Saved crash report to ${reportFile.absolutePath}")
            
            // Clean once more to guarantee post-crash limits
            cleanOldReports(dir)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash report", e)
        }
    }

    private fun cleanOldReports(dir: File) {
        try {
            val files = dir.listFiles() ?: return
            if (files.size >= MAX_REPORTS) {
                // Sort by date modified or filename timestamp (which matches chronological since it's yyyy-MM-dd_HH-mm-ss)
                val sortedFiles = files.sortedBy { it.name }
                val deleteCount = files.size - MAX_REPORTS + 1
                for (i in 0 until deleteCount) {
                    if (i < sortedFiles.size) {
                        sortedFiles[i].delete()
                        Log.d(TAG, "Deleted old crash report to maintain limit: ${sortedFiles[i].name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean old reports", e)
        }
    }

    fun listReports(context: Context): List<CrashReportItem> {
        val dir = getCrashReportsDirectory(context)
        val files = dir.listFiles() ?: return emptyList()
        return files.map { file ->
            val content = try { file.readText() } catch (e: Exception) { "Load error: ${e.localizedMessage}" }
            val firstLine = content.lines().firstOrNull { it.startsWith("Exception Class:") }
                ?.substringAfter("Exception Class:")?.trim() ?: "Unknown Exception"
            val messageLine = content.lines().firstOrNull { it.startsWith("Exception Message:") }
                ?.substringAfter("Exception Message:")?.trim() ?: "No Message"
            val dateStr = file.name.removePrefix("crash_").substringBeforeLast("_")
                .replace("_", " ")
            CrashReportItem(
                file = file,
                fileName = file.name,
                dateTime = dateStr,
                exceptionClass = firstLine,
                message = messageLine,
                fullContent = content
            )
        }.sortedByDescending { it.file.lastModified() }
    }

    fun clearAllReports(context: Context) {
        val dir = getCrashReportsDirectory(context)
        val files = dir.listFiles() ?: return
        for (f in files) {
            f.delete()
        }
        Log.d(TAG, "Cleared all crash reports")
    }

    fun deleteReport(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            false
        }
    }
}

data class CrashReportItem(
    val file: File,
    val fileName: String,
    val dateTime: String,
    val exceptionClass: String,
    val message: String,
    val fullContent: String
)
