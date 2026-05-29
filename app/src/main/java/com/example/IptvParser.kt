package com.example

import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.InputStream
import java.io.StringReader
import java.util.Calendar
import java.util.TimeZone

object IptvParser {
    private const val TAG = "IptvParser"

    /**
     * Parses an M3U playlist from an InputStream (either raw or downloaded).
     * Returns a pair of categories and live channels.
     */
    fun parseM3u(inputStream: InputStream): Pair<List<IptvCategory>, List<IptvChannel>> {
        val channels = mutableListOf<IptvChannel>()
        val categoriesSet = mutableSetOf<String>()
        val maxChannelsLimit = 10000

        try {
            // Setup robust UTF-8 decoder that replaces unrecognized/malformed characters
            val decoder = java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPLACE)
            
            java.io.BufferedReader(java.io.InputStreamReader(inputStream, decoder)).use { reader ->
                var currentChannelName = ""
                var currentLogoUrl: String? = null
                var currentCategory = "Uncategorized"
                var currentEpgId: String? = null
                var currentChannelId: String? = null

                var line: String? = reader.readLine()
                var linesCount = 0

                while (line != null) {
                    linesCount++
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) {
                        line = reader.readLine()
                        continue
                    }

                    if (trimmed.startsWith("#EXTM3U", ignoreCase = true)) {
                        // EXTM3U start line, can contain key-value pairs but we skip by default
                        line = reader.readLine()
                        continue
                    }

                    if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                        try {
                            // Reset line details
                            currentChannelName = ""
                            currentLogoUrl = null
                            currentCategory = "Uncategorized"
                            currentEpgId = null
                            currentChannelId = null

                            // Parse attributes, e.g., #EXTINF:-1 tvg-id="CNN" tvg-name="CNN US" tvg-logo="url" group-title="News",CNN US
                            val infoPart = trimmed.substringAfter("#EXTINF:")
                            val commaIdx = infoPart.lastIndexOf(',')
                            currentChannelName = if (commaIdx != -1) {
                                infoPart.substring(commaIdx + 1).trim()
                            } else {
                                "Unknown Channel"
                            }

                            // Regex to parse key-value attributes like tvg-id="foo" or group-title="bar"
                            val metaPart = if (commaIdx != -1) infoPart.substring(0, commaIdx) else infoPart

                            // Extract specific elements with helper
                            currentEpgId = extractAttribute(metaPart, "tvg-id")
                            val tvgName = extractAttribute(metaPart, "tvg-name")
                            currentLogoUrl = extractAttribute(metaPart, "tvg-logo") ?: extractAttribute(metaPart, "tvg-screenshot")
                            
                            val categoryMatch = extractAttribute(metaPart, "group-title")
                            if (categoryMatch != null && categoryMatch.isNotBlank()) {
                                currentCategory = categoryMatch
                            }
                            
                            if (currentEpgId.isNullOrBlank()) {
                                currentEpgId = tvgName ?: currentChannelName
                            }
                            
                            // Fallback channel unique ID
                            currentChannelId = extractAttribute(metaPart, "channel-id") ?: currentEpgId
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing EXTINF info on line $linesCount: '$trimmed'", e)
                        }
                    } else if (!trimmed.startsWith("#")) {
                        // Any line that does not start with '#' inside M3U represents the stream URL
                        try {
                            val streamUrl = trimmed
                            if (streamUrl.isNotBlank()) {
                                if (channels.size >= maxChannelsLimit) {
                                    Log.w(TAG, "Playlist is too large! Truncated import at $maxChannelsLimit channels for app stability.")
                                    break
                                }

                                val finalId = currentChannelId ?: java.util.UUID.nameUUIDFromBytes(streamUrl.toByteArray()).toString()
                                val verifiedName = if (currentChannelName.isNotBlank()) currentChannelName else "Channel ${channels.size + 1}"
                                
                                categoriesSet.add(currentCategory)
                                channels.add(
                                    IptvChannel(
                                        id = finalId,
                                        name = verifiedName,
                                        logoUrl = currentLogoUrl?.takeIf { it.isNotBlank() },
                                        streamUrl = streamUrl,
                                        categoryId = currentCategory,
                                        epgId = currentEpgId?.takeIf { it.isNotBlank() },
                                        isFavorite = false
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Exception parsing stream URL line $linesCount: '$trimmed'", e)
                        }
                    }
                    line = reader.readLine()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error parsing M3U playlist stream source", e)
        }

        val categories = categoriesSet.map { name ->
            IptvCategory(id = name, name = name, type = "live")
        }.sortedBy { it.name }

        Log.d(TAG, "M3U Parsing fully completed successfully. Loaded ${channels.size} live channels in ${categories.size} categories.")
        return Pair(categories, channels)
    }

    private fun extractAttribute(source: String, attrName: String): String? {
        return try {
            val pattern = """$attrName\s*=\s*"([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
            val match = pattern.find(source)
            if (match != null) {
                return match.groupValues[1].trim()
            }
            val patternNoQuotes = """$attrName\s*=\s*([^,\s]*)""".toRegex(RegexOption.IGNORE_CASE)
            val matchNoQuotes = patternNoQuotes.find(source)
            matchNoQuotes?.groupValues[1]?.trim()
        } catch (e: Exception) {
            Log.w(TAG, "Exception extracting attribute $attrName", e)
            null
        }
    }

    /**
     * Parses an XMLTV XML file.
     * Returns a list of parsed EPG programmes.
     */
    fun parseXmltv(inputStream: InputStream): List<EpgProgramme> {
        val programmes = mutableListOf<EpgProgramme>()
        val parser = Xml.newPullParser()
        try {
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType
            
            var currentChannelId: String? = null
            var startTimeStr: String? = null
            var endTimeStr: String? = null
            var title: String? = null
            var desc: String? = null

            // We only keep programmes for current and brief upcoming days
            val thresholdTime = System.currentTimeMillis() - 4 * 3600 * 1000 // Keep past 4 hours

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name == "programme") {
                            currentChannelId = parser.getAttributeValue(null, "channel")
                            startTimeStr = parser.getAttributeValue(null, "start")
                            endTimeStr = parser.getAttributeValue(null, "stop")
                            title = null
                            desc = null
                        } else if (name == "title" && currentChannelId != null) {
                            title = parser.nextText()
                        } else if (name == "desc" && currentChannelId != null) {
                            desc = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name == "programme") {
                            try {
                                if (currentChannelId.isNullOrBlank()) {
                                    Log.w(TAG, "Skipping EPG programme row: missing channelId")
                                } else if (title.isNullOrBlank()) {
                                    Log.w(TAG, "Skipping EPG programme row for channel '$currentChannelId': missing or blank title")
                                } else {
                                    val startMs = parseXmltvDate(startTimeStr)
                                    if (startMs == null) {
                                        Log.w(TAG, "Skipping EPG programme row for channel '$currentChannelId': missing or malformed start time '$startTimeStr'")
                                    } else {
                                        val endMs = parseXmltvDate(endTimeStr) ?: (startMs + 30 * 60 * 1000L).also {
                                            Log.d(TAG, "EPG programme end time is missing or malformed for channel '$currentChannelId'. Using fallback of start + 30 minutes ('$endTimeStr')")
                                        }
                                        
                                        if (endMs > thresholdTime) { // Filter old schedules to avoid OOM
                                            programmes.add(
                                                EpgProgramme(
                                                    channelId = currentChannelId,
                                                    title = title,
                                                    startMs = startMs,
                                                    endMs = endMs,
                                                    description = desc
                                                )
                                            )
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error handling END_TAG 'programme' parsing row, skipping entry.", e)
                            }
                            currentChannelId = null
                            startTimeStr = null
                            endTimeStr = null
                            title = null
                            desc = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing XMLTV EPG", e)
        }
        return programmes
    }

    /**
     * Parse XMLTV formatted dates: 20260528070000 +0000 or 20260528070000
     */
    fun parseXmltvDate(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) {
            return null
        }
        try {
            val cleanStr = dateStr.trim().replace("\\s+".toRegex(), " ")
            val parts = cleanStr.split(" ")
            val mainPart = parts[0] // "20260528070000"
            
            if (mainPart.length < 8) {
                Log.w(TAG, "Malformed XMLTV date (too short): '$dateStr'")
                return null
            }

            val year = mainPart.substring(0, 4).toIntOrNull() ?: return null
            val month = mainPart.substring(4, 6).toIntOrNull() ?: return null
            val day = mainPart.substring(6, 8).toIntOrNull() ?: return null
            
            if (month < 1 || month > 12 || day < 1 || day > 31) {
                Log.w(TAG, "Malformed XMLTV date (out of range): '$dateStr'")
                return null
            }

            val hour = if (mainPart.length >= 10) mainPart.substring(8, 10).toIntOrNull() ?: 0 else 0
            val minute = if (mainPart.length >= 12) mainPart.substring(10, 12).toIntOrNull() ?: 0 else 0
            val second = if (mainPart.length >= 14) mainPart.substring(12, 14).toIntOrNull() ?: 0 else 0

            if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59) {
                Log.w(TAG, "Malformed XMLTV date (time out of range): '$dateStr'")
                return null
            }
            
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.clear()
            calendar.set(year, month - 1, day, hour, minute, second)
            
            var timeMs = calendar.timeInMillis
            
            if (parts.size > 1) {
                val offsetPart = parts[1] // "+0000" or "-0500"
                if (offsetPart.length == 5 && (offsetPart.startsWith("+") || offsetPart.startsWith("-"))) {
                    val sign = if (offsetPart.startsWith("+")) 1 else -1
                    val hoursOffset = offsetPart.substring(1, 3).toIntOrNull()
                    val minutesOffset = offsetPart.substring(3, 5).toIntOrNull()
                    if (hoursOffset != null && minutesOffset != null) {
                        val totalOffsetMs = (hoursOffset * 3600 + minutesOffset * 60) * 1000L * sign
                        timeMs -= totalOffsetMs // Normalize back to UTC epoch
                    } else {
                        Log.w(TAG, "Malformed XMLTV date timezone offset numbers: '$dateStr'")
                    }
                } else {
                    Log.w(TAG, "Malformed XMLTV date timezone offset format: '$dateStr'")
                }
            }
            return timeMs
        } catch (e: Exception) {
            Log.w(TAG, "Exception parsing XMLTV date: '$dateStr'", e)
            return null
        }
    }
}
