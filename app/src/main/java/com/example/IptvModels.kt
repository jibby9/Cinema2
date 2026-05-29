package com.example

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class XtreamAccount(
    val name: String,
    val serverUrl: String,
    val username: String,
    val password: String,
    val isActive: Boolean = false
)

@JsonClass(generateAdapter = true)
data class M3UConfig(
    val name: String,
    val playlistUrl: String,
    val epgUrl: String? = null,
    val isActive: Boolean = false
)

data class IptvChannel(
    val id: String, // Stream ID or URL hash
    val name: String,
    val logoUrl: String? = null,
    val streamUrl: String,
    val categoryId: String = "",
    val epgId: String? = null,
    val isFavorite: Boolean = false
)

data class IptvCategory(
    val id: String,
    val name: String,
    val type: String = "live" // "live", "vod", "series"
)

data class EpgProgramme(
    val channelId: String,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val description: String? = null
)

data class SearchResultEpg(
    val programme: EpgProgramme,
    val channel: IptvChannel?
)

enum class CategorySortMode(val displayName: String) {
    PROVIDER("Playlist Default"),
    CUSTOM("Custom Order"),
    NAME_AZ("Name A-Z"),
    FAVORITES_FIRST("Favorites First")
}

enum class ChannelSortMode(val displayName: String) {
    PROVIDER("Playlist Default"),
    CUSTOM("Custom Order"),
    NAME_AZ("Name A-Z"),
    CHANNEL_NUMBER("Channel Number"),
    FAVORITES_FIRST("Favorites First")
}

@JsonClass(generateAdapter = true)
data class IptvHistoryItem(
    val id: String,
    val name: String,
    val logoUrl: String? = null,
    val streamUrl: String,
    val categoryId: String = "",
    val epgId: String? = null,
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
) {
    fun toIptvChannel(isFav: Boolean = false): IptvChannel {
        return IptvChannel(
            id = id,
            name = name,
            logoUrl = logoUrl,
            streamUrl = streamUrl,
            categoryId = categoryId,
            epgId = epgId,
            isFavorite = isFav
        )
    }
}
