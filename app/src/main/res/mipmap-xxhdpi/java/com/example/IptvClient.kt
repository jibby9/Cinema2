package com.example

import android.util.Base64
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

object IptvClient {
    private const val TAG = "IptvClient"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Authenticates with an Xtream server, returning true if credentials are valid.
     */
    fun testXtreamConnection(serverUrl: String, username: String, password: String): Boolean {
        try {
            val formattedUrl = sanitizeServerUrl(serverUrl)
            val url = "$formattedUrl/player_api.php?username=$username&password=$password"
            
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val bodyStr = response.body?.string() ?: return false
                
                // Server returns a wrapper object on login with user_info block
                if (bodyStr.contains("user_info")) {
                    val jsonObj = JSONObject(bodyStr)
                    val userInfo = jsonObj.optJSONObject("user_info")
                    if (userInfo != null) {
                        val auth = userInfo.optInt("auth", 1)
                        if (auth == 0) return false
                        return true
                    }
                }
                // Some older panels return a different JSON or error structure
                return bodyStr.contains("server_info")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Xtream Login connection check failed", e)
            return false
        }
    }

    /**
     * Fetches live categories from Xtream server.
     */
    fun fetchXtreamCategories(serverUrl: String, username: String, password: String): List<IptvCategory> {
        val categories = mutableListOf<IptvCategory>()
        try {
            val formattedUrl = sanitizeServerUrl(serverUrl)
            val url = "$formattedUrl/player_api.php?username=$username&password=$password&action=get_live_categories"
            
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val bodyStr = response.body?.string() ?: return emptyList()
                
                val arr = JSONArray(bodyStr)
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val catId = item.getString("category_id")
                    val catName = item.getString("category_name")
                    categories.add(IptvCategory(id = catId, name = catName, type = "live"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching categories from Xtream", e)
        }
        return categories.sortedBy { it.name }
    }

    /**
     * Fetches live channels from Xtream server.
     */
    fun fetchXtreamChannels(serverUrl: String, username: String, password: String): List<IptvChannel> {
        val channels = mutableListOf<IptvChannel>()
        try {
            val formattedUrl = sanitizeServerUrl(serverUrl)
            val url = "$formattedUrl/player_api.php?username=$username&password=$password&action=get_live_streams"
            
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val bodyStr = response.body?.string() ?: return emptyList()
                
                val arr = JSONArray(bodyStr)
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val streamId = item.getInt("stream_id")
                    val name = item.optString("name", "Channel $streamId")
                    val logoUrl = item.optString("stream_icon").takeIf { !it.isNullOrBlank() && it != "null" }
                    val catId = item.optString("category_id", "")
                    val epgChannelId = item.optString("epg_channel_id").takeIf { !it.isNullOrBlank() && it != "null" }
                    
                    // Create direct Xtream stream URL
                    val streamUrl = "$formattedUrl/live/$username/$password/$streamId.ts"
                    
                    channels.add(
                        IptvChannel(
                            id = streamId.toString(),
                            name = name,
                            logoUrl = logoUrl,
                            streamUrl = streamUrl,
                            categoryId = catId,
                            epgId = epgChannelId ?: name
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching streams from Xtream", e)
        }
        return channels
    }

    /**
     * Fetches short EPG details for a specific channel from Xtream.
     */
    fun fetchXtreamShortEpg(serverUrl: String, username: String, password: String, streamId: String): List<EpgProgramme> {
        val programmes = mutableListOf<EpgProgramme>()
        try {
            val formattedUrl = sanitizeServerUrl(serverUrl)
            val url = "$formattedUrl/player_api.php?username=$username&password=$password&action=get_short_epg&stream_id=$streamId"
            
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val bodyStr = response.body?.string() ?: return emptyList()
                
                val mainObj = JSONObject(bodyStr)
                val epgList = mainObj.optJSONArray("epg_listings") ?: return emptyList()
                
                for (i in 0 until epgList.length()) {
                    val item = epgList.getJSONObject(i)
                    val titleFormatted = decodeBase64IfPossible(item.optString("title"))
                    val descFormatted = decodeBase64IfPossible(item.optString("description"))
                    
                    val startStr = item.optString("start")
                    val endStr = item.optString("end")
                    
                    val startMs = parseLocalDateTimeOrTimestamp(startStr)
                    val endMs = parseLocalDateTimeOrTimestamp(endStr)
                    
                    programmes.add(
                        EpgProgramme(
                            channelId = streamId,
                            title = titleFormatted,
                            startMs = startMs,
                            endMs = endMs,
                            description = descFormatted
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Xtream short EPG", e)
        }
        return programmes
    }

    /**
     * Downloads an external M3U file or XMLTV resource and provides a decompressing input stream of its text contents.
     */
    fun fetchUrlStream(targetUrl: String): InputStream {
        val request = Request.Builder().url(targetUrl).build()
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw java.io.IOException("HTTP download error: ${response.code}")
        }
        
        val body = response.body ?: throw java.io.IOException("Empty response body")
        val responseStream = body.byteStream()
        
        val isGzip = targetUrl.endsWith(".gz") || response.header("Content-Encoding") == "gzip"
        return if (isGzip) {
            GZIPInputStream(responseStream)
        } else {
            responseStream
        }
    }

    private fun sanitizeServerUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        if (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length - 1)
        }
        return clean
    }

    private fun decodeBase64IfPossible(text: String?): String {
        if (text == null) return ""
        if (text.isBlank()) return ""
        // Check if string is base64
        return try {
            val decodedBytes = Base64.decode(text, Base64.DEFAULT)
            val decodedStr = String(decodedBytes, Charsets.UTF_8)
            // Double check: if text contains non-printable characters, keep original
            if (decodedStr.all { it.isWhitespace() || it.code in 32..126 || it.code in 160..255 }) {
                decodedStr
            } else {
                text
            }
        } catch (e: Exception) {
            text
        }
    }

    private fun parseLocalDateTimeOrTimestamp(timeStr: String): Long {
        if (timeStr.isBlank()) return System.currentTimeMillis()
        try {
            if (timeStr.matches(Regex("\\d+"))) {
                return timeStr.toLong() * 1000L
            }
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(timeStr)
            if (date != null) {
                return date.time
            }
        } catch (e: Exception) {
            // Ignore - fallback below
        }
        return System.currentTimeMillis()
    }
}
