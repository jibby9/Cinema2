package com.example

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log

/**
 * Data class representing all extracted details from an incoming Android Intent.
 * This will be displayed in the on-screen debug panel to assist with debugging Stremio integrations.
 */
data class ParsedIntentInfo(
    val action: String?,
    val mimeType: String?,
    val dataUri: String?,
    val extraText: String?,
    val extraStreamUri: String?,
    val clipDataItems: List<String>,
    val extrasDump: Map<String, String>,
    val resolvedPlayableUri: String?,
    val rawDetailsDump: String
)

object IntentParser {
    private const val TAG = "IntentParser"

    // Regex to find http/https URLs in text
    private val URL_REGEX = "https?://[a-zA-Z0-9\\-_\\.\\*\\?\\/\\+=&%#:@!$,;~]*".toRegex()

    /**
     * Inspects an incoming Intent and extracts all metadata, extras, and selects the best playable URI.
     */
    fun parse(intent: Intent?): ParsedIntentInfo {
        if (intent == null) {
            return ParsedIntentInfo(
                action = null,
                mimeType = null,
                dataUri = null,
                extraText = null,
                extraStreamUri = null,
                clipDataItems = emptyList(),
                extrasDump = emptyMap(),
                resolvedPlayableUri = null,
                rawDetailsDump = "No intent received"
            )
        }

        val action = intent.action
        val mimeType = intent.type
        val dataUriStr = intent.dataString

        // 1. Parse EXTRA_TEXT
        val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)

        // 2. Parse EXTRA_STREAM (could be a Uri)
        val extraStreamUri = getExtraStreamUri(intent)?.toString()

        // 3. Parse ClipData Items
        val clipDataItems = getClipDataItems(intent)

        // 4. Dump all extras keys and values
        val extrasDump = getExtrasDump(intent.extras)

        // 5. Select the best Playable URI
        val resolvedPlayableUri = resolvePlayableUri(
            dataUriStr,
            extraText,
            extraStreamUri,
            clipDataItems
        )

        // Build a detailed text representation for the debug display
        val dumpBuilder = StringBuilder()
        dumpBuilder.append("INTENT ACTION: ${action ?: "N/A"}\n")
        dumpBuilder.append("MIME TYPE: ${mimeType ?: "N/A"}\n")
        dumpBuilder.append("DATA URI: ${dataUriStr ?: "N/A"}\n")
        dumpBuilder.append("EXTRA_TEXT: ${extraText ?: "N/A"}\n")
        dumpBuilder.append("EXTRA_STREAM: ${extraStreamUri ?: "N/A"}\n")
        
        if (clipDataItems.isNotEmpty()) {
            dumpBuilder.append("\n=== CLIP DATA ===\n")
            clipDataItems.forEachIndexed { index, item ->
                dumpBuilder.append("[$index] $item\n")
            }
        }

        if (extrasDump.isNotEmpty()) {
            dumpBuilder.append("\n=== ALL EXTRAS ===\n")
            extrasDump.forEach { (key, value) ->
                dumpBuilder.append("$key: $value\n")
            }
        } else {
            dumpBuilder.append("\n=== NO EXTRAS FOUND ===\n")
        }

        dumpBuilder.append("\n=== PLAY RESOLUTION ===\n")
        dumpBuilder.append("Resolved Playable URI: ${resolvedPlayableUri ?: "None resolved"}\n")

        return ParsedIntentInfo(
            action = action,
            mimeType = mimeType,
            dataUri = dataUriStr,
            extraText = extraText,
            extraStreamUri = extraStreamUri,
            clipDataItems = clipDataItems,
            extrasDump = extrasDump,
            resolvedPlayableUri = resolvedPlayableUri,
            rawDetailsDump = dumpBuilder.toString()
        )
    }

    /**
     * Resolves the best candidate URI for video playback from all parsed components.
     * Rules can be modified here to adjust parsing order:
     */
    fun resolvePlayableUri(
        dataUriStr: String?,
        extraText: String?,
        extraStreamUri: String?,
        clipDataItems: List<String>
    ): String? {
        // Priority 1: Data URI of the Intent (Standard ACTION_VIEW payload)
        if (!dataUriStr.isNullOrBlank() && isValidPlaybackUri(dataUriStr)) {
            Log.d(TAG, "Resolved via intent data URI: $dataUriStr")
            return dataUriStr
        }

        // Priority 2: Uri inside EXTRA_STREAM (Shared file/item)
        if (!extraStreamUri.isNullOrBlank() && isValidPlaybackUri(extraStreamUri)) {
            Log.d(TAG, "Resolved via EXTRA_STREAM: $extraStreamUri")
            return extraStreamUri
        }

        // Priority 3: Extract URI from EXTRA_TEXT (Shared text containing raw video link)
        if (!extraText.isNullOrBlank()) {
            val extractedUrl = extractFirstUrl(extraText)
            if (extractedUrl != null && isValidPlaybackUri(extractedUrl)) {
                Log.d(TAG, "Resolved via EXTRA_TEXT URL extraction: $extractedUrl")
                return extractedUrl
            }
        }

        // Priority 4: Search ClipData elements
        for (item in clipDataItems) {
            if (isValidPlaybackUri(item)) {
                Log.d(TAG, "Resolved via ClipData item URI: $item")
                return item
            }
            // If the item is text, try extracting a URL from it
            val extracted = extractFirstUrl(item)
            if (extracted != null && isValidPlaybackUri(extracted)) {
                Log.d(TAG, "Resolved via ClipData text extraction: $extracted")
                return extracted
            }
        }

        return null
    }

    /**
     * Checks if a string acts as a plausible video player source (http, https, content, file).
     * Add any customized checking rules or blacklisted keywords here if needed.
     */
    private fun isValidPlaybackUri(uriString: String): Boolean {
        val trimmed = uriString.trim()
        return trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("content://", ignoreCase = true) ||
                trimmed.startsWith("file://", ignoreCase = true)
    }

    /**
     * Uses regular expressions to extract the first HTTP/HTTPS web links in text.
     */
    private fun extractFirstUrl(text: String): String? {
        return URL_REGEX.find(text)?.value
    }

    /**
     * Extracts EXTRA_STREAM safely since it could be deprecated or different types.
     */
    @Suppress("DEPRECATION")
    private fun getExtraStreamUri(intent: Intent): Uri? {
        return try {
            if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                        ?: intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.firstOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing EXTRA_STREAM", e)
            null
        }
    }

    /**
     * Safe extraction of clipData elements.
     */
    private fun getClipDataItems(intent: Intent): List<String> {
        val list = mutableListOf<String>()
        val clipData: ClipData? = intent.clipData
        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                val item = clipData.getItemAt(i)
                val uri = item.uri
                val text = item.text
                if (uri != null) {
                    list.add(uri.toString())
                } else if (!text.isNullOrBlank()) {
                    list.add(text.toString())
                }
            }
        }
        return list
    }

    /**
     * Safe fallback string dump representing all extra values inside a bundle bundle.
     */
    private fun getExtrasDump(bundle: Bundle?): Map<String, String> {
        if (bundle == null) return emptyMap()
        val dump = mutableMapOf<String, String>()
        try {
            for (key in bundle.keySet()) {
                val value = bundle.get(key)
                dump[key] = value?.toString() ?: "null"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error dumping extras", e)
            dump["Error"] = e.localizedMessage ?: "Exception parsing"
        }
        return dump
    }

    /**
     * Extracts optional request headers from the incoming intent extras.
     * Supports standard Android media player formats (Bundle, String arrays, Map, etc.).
     */
    fun extractHeaders(intent: Intent?): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        if (intent == null) return headers

        try {
            // 1. Check for standard Bundle extra "headers" (VLC / standard pattern)
            val headersBundle = intent.getBundleExtra("headers")
            if (headersBundle != null) {
                for (key in headersBundle.keySet()) {
                    val value = headersBundle.getString(key)
                    if (value != null) {
                        headers[key] = value
                    }
                }
            }

            // 2. Check for string array extra "headers" (MX Player pattern: ["Key1", "Value1", "Key2", "Value2", ...])
            val headersArray = intent.getStringArrayExtra("headers")
            if (headersArray != null && headersArray.size >= 2) {
                for (i in 0 until headersArray.size - 1 step 2) {
                    val key = headersArray[i]
                    val value = headersArray[i + 1]
                    if (!key.isNullOrBlank() && !value.isNullOrBlank()) {
                        headers[key.trim()] = value.trim()
                    }
                }
            }

            // 3. Check for "http-header-fields" (Some other common video players)
            val httpHeaderFields = intent.getStringArrayExtra("http-header-fields")
            if (httpHeaderFields != null && httpHeaderFields.size >= 2) {
                for (i in 0 until httpHeaderFields.size - 1 step 2) {
                    val key = httpHeaderFields[i]
                    val value = httpHeaderFields[i + 1]
                    if (!key.isNullOrBlank() && !value.isNullOrBlank()) {
                        headers[key.trim()] = value.trim()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting custom headers from intent", e)
        }

        return headers
    }
}
