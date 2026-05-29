package com.example

import android.app.Application
import android.util.Log

class CinemaApplication : Application() {
    companion object {
        private const val TAG = "CinemaApplication"
        lateinit var instance: CinemaApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception caught globally on thread: ${thread.name}", throwable)
            try {
                CrashReporter.saveCrashReport(this, thread, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write uncaught crash report file", e)
            } finally {
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable)
                } else {
                    // Fallback to exiting process normally
                    android.os.Process.killProcess(android.os.Process.myPid())
                    java.lang.System.exit(1)
                }
            }
        }
    }
}
