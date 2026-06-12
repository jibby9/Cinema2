package com.example

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.concurrent.TimeUnit

object EpgRefreshManager {
    private const val TAG = "EpgRefreshManager"
    private const val PREFS_NAME = "epg_refresh_prefs"
    private const val KEY_LAST_REFRESH_TIME_GLOBAL = "last_refresh_time_global"
    private const val KEY_LAST_REFRESH_TIME_PREFIX = "last_refresh_time_"

    // Debounce / throttle interval for app-foreground refresh (15 minutes)
    private const val FOREGROUND_THROTTLE_MS = 15 * 60 * 1000L

    fun getLastRefreshTimeGlobal(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_REFRESH_TIME_GLOBAL, 0L)
    }

    fun setLastRefreshTimeGlobal(context: Context, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_REFRESH_TIME_GLOBAL, timestamp).apply()
    }

    fun getLastRefreshTimeForSource(context: Context, sourceId: String): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_REFRESH_TIME_PREFIX + sourceId, 0L)
    }

    fun setLastRefreshTimeForSource(context: Context, sourceId: String, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_REFRESH_TIME_PREFIX + sourceId, timestamp).apply()
    }

    /**
     * Helper to retrieve all configured sources (both M3u and Xtream).
     */
    suspend fun fetchAllConfigs(context: Context): Pair<List<M3UConfig>, List<XtreamAccount>> {
        val prefManager = PlayerPreferenceManager(context)
        val m3uJson = try {
            prefManager.m3uPlaylistsJson.first()
        } catch (e: Exception) {
            null
        }

        val xtreamJson = try {
            prefManager.xtreamAccountsJson.first()
        } catch (e: Exception) {
            null
        }

        val m3uList = deserializeM3uConfigs(m3uJson)
        val xtreamList = deserializeAccounts(xtreamJson)

        return Pair(m3uList, xtreamList)
    }

    private fun deserializeAccounts(json: String?): List<XtreamAccount> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<XtreamAccount>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    XtreamAccount(
                        name = obj.getString("name"),
                        serverUrl = obj.getString("serverUrl"),
                        username = obj.getString("username"),
                        password = obj.getString("password"),
                        isActive = obj.optBoolean("isActive", false)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing accounts", e)
        }
        return list
    }

    private fun deserializeM3uConfigs(json: String?): List<M3UConfig> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<M3UConfig>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    M3UConfig(
                        name = obj.getString("name"),
                        playlistUrl = obj.getString("playlistUrl"),
                        epgUrl = obj.optString("epgUrl").takeIf { it.isNotBlank() },
                        isActive = obj.optBoolean("isActive", false)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing playlists", e)
        }
        return list
    }

    /**
     * Refreshes the EPG for ALL configured sources.
     * Keeps track of which sources succeeded and updates the timestamp cache.
     */
    suspend fun refreshAllEpgSources(context: Context, force: Boolean = false): Boolean {
        Log.d(TAG, "Starting refreshAllEpgSources (force=$force)")
        if (!force) {
            val lastGlobal = getLastRefreshTimeGlobal(context)
            val elapsed = System.currentTimeMillis() - lastGlobal
            if (elapsed < FOREGROUND_THROTTLE_MS) {
                Log.d(TAG, "EPG refresh throttled. Last global refresh was only ${elapsed / 1000} seconds ago.")
                return false
            }
        }

        var anySuccess = false
        try {
            val (m3uList, xtreamList) = fetchAllConfigs(context)
            Log.d(TAG, "Found ${m3uList.size} M3U playlists and ${xtreamList.size} Xtream accounts.")

            // Refresh M3U sources
            for (m3u in m3uList) {
                val epgUrl = m3u.epgUrl
                if (!epgUrl.isNullOrBlank() &&
                    (epgUrl.startsWith("http://", ignoreCase = true) || epgUrl.startsWith("https://", ignoreCase = true))) {
                    val sourceId = "m3u_" + java.util.UUID.nameUUIDFromBytes(m3u.playlistUrl.toByteArray()).toString()
                    Log.d(TAG, "Refreshing EPG for M3U playlist: ${m3u.name} (SourceId: $sourceId)")
                    try {
                        IptvClient.fetchUrlStream(epgUrl).use { stream ->
                            val parsed = IptvParser.parseXmltv(stream)
                            if (parsed.isNotEmpty()) {
                                saveEpgToSpecificCache(context, parsed, sourceId)
                                setLastRefreshTimeForSource(context, sourceId, System.currentTimeMillis())
                                anySuccess = true
                                Log.d(TAG, "Saved EPG for ${m3u.name}: ${parsed.size} shows loaded.")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to refresh EPG for M3U ${m3u.name}", e)
                    }
                }
            }

            // Refresh Xtream sources
            for (xtream in xtreamList) {
                if (xtream.serverUrl.isNotBlank() && xtream.username.isNotBlank()) {
                    val sourceId = "xtream_" + java.util.UUID.nameUUIDFromBytes("${xtream.serverUrl}_${xtream.username}".toByteArray()).toString()
                    Log.d(TAG, "Refreshing EPG for Xtream Account: ${xtream.name} (SourceId: $sourceId)")
                    val directXmlEpgUrl = "${xtream.serverUrl}/xmltv.php?username=${xtream.username}&password=${xtream.password}"
                    try {
                        IptvClient.fetchUrlStream(directXmlEpgUrl).use { stream ->
                            val parsed = IptvParser.parseXmltv(stream)
                            if (parsed.isNotEmpty()) {
                                saveEpgToSpecificCache(context, parsed, sourceId)
                                setLastRefreshTimeForSource(context, sourceId, System.currentTimeMillis())
                                anySuccess = true
                                Log.d(TAG, "Saved XMLTV EPG for Xtream ${xtream.name}: ${parsed.size} shows loaded.")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "XMLTV reload failed for Xtream ${xtream.name}, skipping background refresh since short EPG requires active channels list.", e)
                    }
                }
            }

            if (anySuccess) {
                setLastRefreshTimeGlobal(context, System.currentTimeMillis())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in refreshAllEpgSources", e)
        }

        return anySuccess
    }

    private fun saveEpgToSpecificCache(context: Context, programmes: List<EpgProgramme>, sourceId: String) {
        try {
            val file = File(context.cacheDir, "epg_cache_$sourceId.json")
            val arr = org.json.JSONArray()
            for (p in programmes) {
                val obj = org.json.JSONObject()
                obj.put("channelId", p.channelId)
                obj.put("title", p.title)
                obj.put("startMs", p.startMs)
                obj.put("endMs", p.endMs)
                obj.put("description", p.description ?: "")
                arr.put(obj)
            }
            file.writeText(arr.toString())
            Log.d(TAG, "Successfully cached EPG for source $sourceId: ${programmes.size} items.")
            
            // Also write to old epg_cache.json if default is requested or needed
            val legacyFile = File(context.cacheDir, "epg_cache.json")
            legacyFile.writeText(arr.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing source EPG to file cache", e)
        }
    }

    /**
     * Schedules the unique periodic background work for EPG refresh every 8 hours.
     */
    fun schedulePeriodicRefresh(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<EpgBackgroundWorker>(8, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "UniqueEpgRefreshWork",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
        Log.d(TAG, "Scheduled unique periodic EPG background refresh worker (every 8 hours).")
    }
}
