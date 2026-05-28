package com.example

import java.util.UUID

data class SportsEvent(
    val id: String = UUID.randomUUID().toString(),
    val sport: String, // "Football", "Boxing", "UFC", "Basketball", "American Football", "Baseball", "Hockey", "Tennis", "Darts", "Snooker", "Motorsport"
    val competition: String,
    val title: String,
    val dateTimeMs: Long,
    val teamA: String?,
    val teamB: String?,
    val description: String? = null
) {
    fun getStatus(): String {
        val now = System.currentTimeMillis()
        val duration = when (sport) {
            "Football" -> 120 * 60 * 1000L // 2 hours
            "Basketball" -> 150 * 60 * 1000L // 2.5 hours
            "Motorsport" -> 180 * 60 * 1000L // 3) hours
            "UFC", "Boxing" -> 180 * 60 * 1000L
            else -> 120 * 60 * 1000L
        }
        return when {
            now < dateTimeMs -> "UPCOMING"
            now > (dateTimeMs + duration) -> "FINISHED"
            else -> "LIVE"
        }
    }
}

enum class MatchConfidence {
    EXACT,     // Match on EPG show or specific team + channel names
    LIKELY,    // Match on sport/competition or specific channel keywords
    POSSIBLE   // General sports channel
}

data class ChannelMatchResult(
    val channel: IptvChannel,
    val confidence: MatchConfidence,
    val matchedReason: String
)

interface SportsRepository {
    fun getFeaturedEvents(): List<SportsEvent>
    fun refreshEvents()
}

class StaticSportsRepository : SportsRepository {
    private var eventsList = listOf<SportsEvent>()

    init {
        generateEvents()
    }

    override fun getFeaturedEvents(): List<SportsEvent> = eventsList

    override fun refreshEvents() {
        generateEvents()
    }

    private fun generateEvents() {
        val now = System.currentTimeMillis()
        eventsList = listOf(
            // Football / Soccer
            SportsEvent(
                sport = "Football",
                competition = "Premier League",
                title = "Arsenal vs Chelsea",
                dateTimeMs = now - 30 * 60 * 1000L, // Started 30 mins ago (LIVE)
                teamA = "Arsenal",
                teamB = "Chelsea",
                description = "Live London derby from the Emirates Stadium."
            ),
            SportsEvent(
                sport = "Football",
                competition = "UEFA Champions League",
                title = "Real Madrid vs Manchester City",
                dateTimeMs = now + 90 * 60 * 1000L, // In 1.5 hours
                teamA = "Real Madrid",
                teamB = "Manchester City",
                description = "Semi-final first leg of Europe's premier club competition."
            ),
            SportsEvent(
                sport = "Football",
                competition = "La Liga",
                title = "FC Barcelona vs Atletico Madrid",
                dateTimeMs = now + 24 * 3600 * 1000L, // Tomorrow
                teamA = "FC Barcelona",
                teamB = "Atletico Madrid",
                description = "Crunch match in the race for the Spanish title."
            ),
            // UFC / MMA
            SportsEvent(
                sport = "UFC",
                competition = "UFC 309",
                title = "Jon Jones vs Stipe Miocic",
                dateTimeMs = now - 60 * 60 * 1000L, // Started 1 hour ago (LIVE)
                teamA = "Jon Jones",
                teamB = "Stipe Miocic",
                description = "Heavyweight Championship Main Event."
            ),
            SportsEvent(
                sport = "UFC",
                competition = "UFC Fight Night",
                title = "Islam Makhachev vs Dustin Poirier",
                dateTimeMs = now + 4 * 3600 * 1000L, // In 4 hours
                teamA = "Islam Makhachev",
                teamB = "Dustin Poirier",
                description = "Lightweight Championship showdown."
            ),
            // Boxing
            SportsEvent(
                sport = "Boxing",
                competition = "Heavyweight Title",
                title = "Tyson Fury vs Oleksandr Usyk",
                dateTimeMs = now + 6 * 3600 * 1000L, // In 6 hours
                teamA = "Tyson Fury",
                teamB = "Oleksandr Usyk",
                description = "Undisputed Heavyweight Champion Clash."
            ),
            // Basketball
            SportsEvent(
                sport = "Basketball",
                competition = "NBA Playoffs",
                title = "Boston Celtics vs Dallas Mavericks",
                dateTimeMs = now + 45 * 60 * 1000L, // In 45 mins
                teamA = "Boston Celtics",
                teamB = "Dallas Mavericks",
                description = "Game 7 of the NBA Grand Finals live stream."
            ),
            SportsEvent(
                sport = "Basketball",
                competition = "NBA regular",
                title = "Los Angeles Lakers vs Golden State Warriors",
                dateTimeMs = now + 30 * 3600 * 1000L, // Tomorrow
                teamA = "Los Angeles Lakers",
                teamB = "Golden State Warriors",
                description = "Lebron James hosts Steph Curry in an epic Western Conference showdown."
            ),
            // Motorsport
            SportsEvent(
                sport = "Motorsport",
                competition = "Formula 1",
                title = "Monaco Grand Prix - Main Race",
                dateTimeMs = now - 15 * 60 * 1000L, // Started 15 mins ago (LIVE)
                teamA = "Red Bull Racing",
                teamB = "Scuderia Ferrari",
                description = "The ultimate test of precision on the narrow streets of Monte Carlo."
            ),
            // American Football
            SportsEvent(
                sport = "American Football",
                competition = "NFL Regular Season",
                title = "Kansas City Chiefs vs San Francisco 49ers",
                dateTimeMs = now + 120 * 60 * 1000L, // In 2 hours
                teamA = "Kansas City Chiefs",
                teamB = "San Francisco 49ers",
                description = "Super Bowl rematch event."
            ),
            // Baseball
            SportsEvent(
                sport = "Baseball",
                competition = "MLB Regular Season",
                title = "New York Yankees vs Boston Red Sox",
                dateTimeMs = now + 3 * 3600 * 1000L,
                teamA = "New York Yankees",
                teamB = "Boston Red Sox",
                description = "The great baseball rivalry."
            ),
            // Hockey
            SportsEvent(
                sport = "Hockey",
                competition = "NHL Stanley Cup",
                title = "Edmonton Oilers vs Florida Panthers",
                dateTimeMs = now + 8 * 3600 * 1000L,
                teamA = "Edmonton Oilers",
                teamB = "Florida Panthers",
                description = "Ice hockey cup final spectacle."
            ),
            // Tennis
            SportsEvent(
                sport = "Tennis",
                competition = "Wimbledon",
                title = "Carlos Alcaraz vs Novak Djokovic",
                dateTimeMs = now + 2 * 24 * 3600 * 1000L, // Two days later
                teamA = "Carlos Alcaraz",
                teamB = "Novak Djokovic",
                description = "Wimbledon Men's Singles Final match."
            ),
            // Darts
            SportsEvent(
                sport = "Darts",
                competition = "PDC World Championship",
                title = "Luke Littler vs Luke Humphries",
                dateTimeMs = now + 5 * 3600 * 1000L,
                teamA = "Luke Littler",
                teamB = "Luke Humphries",
                description = "Legendary darting masters face off."
            ),
            // Snooker
            SportsEvent(
                sport = "Snooker",
                competition = "The Masters",
                title = "Ronnie O'Sullivan vs Judd Trump",
                dateTimeMs = now + 10 * 3600 * 1000L,
                teamA = "Ronnie O'Sullivan",
                teamB = "Judd Trump",
                description = "World snooker absolute elite clash."
            )
        )
    }
}

object SportsChannelMatcher {
    fun matchEventToChannels(
        event: SportsEvent,
        channels: List<IptvChannel>,
        categories: List<IptvCategory>,
        epgList: List<EpgProgramme>
    ): List<ChannelMatchResult> {
        val results = mutableListOf<ChannelMatchResult>()
        
        val teamANorm = event.teamA?.lowercase() ?: ""
        val teamBNorm = event.teamB?.lowercase() ?: ""
        val titleNorm = event.title.lowercase()
        val compNorm = event.competition.lowercase()
        val sportNorm = event.sport.lowercase()

        // Create a fast lookup for categories
        val catMap = categories.associate { it.id to it.name.lowercase() }

        // Compile aliases for matching
        val aliases = mapOf(
            "manchester united" to listOf("man utd", "manchester utd", "man united", "mutv"),
            "manchester city" to listOf("man city", "manchester city", "man city"),
            "real madrid" to listOf("real madrid", "rmtv"),
            "fc barcelona" to listOf("barcelona", "barca", "fcbarcelona"),
            "formula 1" to listOf("f1", "formula 1", "formula one", "motorsport", "motor"),
            "ufc" to listOf("ufc", "mma", "ultimate fighting", "cage warriors", "fight"),
            "boxing" to listOf("boxen", "boxing", "matchroom", "canelo", "fury", "box office")
        )

        fun checkMatch(text: String): Boolean {
            val lower = text.lowercase()
            // Direct substring
            if (teamANorm.isNotEmpty() && lower.contains(teamANorm)) return true
            if (teamBNorm.isNotEmpty() && lower.contains(teamBNorm)) return true
            if (lower.contains(titleNorm)) return true

            // Check aliases
            for ((key, valueList) in aliases) {
                if (teamANorm.contains(key) || teamBNorm.contains(key) || titleNorm.contains(key)) {
                    for (alias in valueList) {
                        if (lower.contains(alias)) return true
                    }
                }
            }
            return false
        }

        // Broad matches for secondary categorization/likely channel checking
        val sportKeywords = when (sportNorm) {
            "football" -> listOf("sport", "football", "soccer", "sky sports", "tnt", "premier league", "laliga")
            "ufc" -> listOf("sports", "ufc", "fight", "tnt sports", "box office", "mma", "action")
            "boxing" -> listOf("sports", "boxing", "fight", "tnt sports", "box office", "sky sports", "action")
            "basketball" -> listOf("sports", "nba", "basketball", "espn", "tnt sports")
            "motorsport" -> listOf("sports", "f1", "sky sports", "motorsport", "motor")
            "american football" -> listOf("sports", "nfl", "espn", "sky sports")
            "baseball" -> listOf("sports", "mlb", "espn")
            "hockey" -> listOf("sports", "nhl", "espn")
            "tennis" -> listOf("sports", "tennis", "eurosport")
            "darts" -> listOf("sports", "darts", "sky sports")
            "snooker" -> listOf("sports", "snooker", "eurosport")
            else -> listOf("sports", sportNorm)
        }

        for (channel in channels) {
            val chNameLower = channel.name.lowercase()
            val catNameLower = catMap[channel.categoryId] ?: ""

            // 1. Check exact/highly likely match in the current channel's EPG program around this time!
            val channelEpgs = epgList.filter { it.channelId == channel.id || it.channelId == channel.epgId }
            var foundEpgExactMatch = false
            var matchedShowTitle = ""

            for (epg in channelEpgs) {
                // If the EPG runs within the window of the event (e.g. within 3 hours starting from event's dateTimeMs +/- 2 hours)
                val windowStart = event.dateTimeMs - 3600000L * 2 // 2 hours before
                val windowEnd = event.dateTimeMs + 3600000L * 3 // 3 hours after
                val overlap = maxOf(epg.startMs, windowStart) < minOf(epg.endMs, windowEnd)
                
                if (overlap) {
                    val epgTitleLower = epg.title.lowercase()
                    val epgDescLower = epg.description?.lowercase() ?: ""
                    
                    if (checkMatch(epgTitleLower) || checkMatch(epgDescLower)) {
                        foundEpgExactMatch = true
                        matchedShowTitle = epg.title
                        break
                    }
                }
            }

            if (foundEpgExactMatch) {
                results.add(
                    ChannelMatchResult(
                        channel = channel,
                        confidence = MatchConfidence.EXACT,
                        matchedReason = "EPG Live Show: '$matchedShowTitle'"
                    )
                )
                continue
            }

            // 2. Check channel name contains exact team names, or event titles
            if (checkMatch(chNameLower)) {
                results.add(
                    ChannelMatchResult(
                        channel = channel,
                        confidence = MatchConfidence.EXACT,
                        matchedReason = "Channel name matches team/event"
                    )
                )
                continue
            }

            // 3. Double Check category name matching (e.g. UFC events in "UFC" or "Sports" group)
            val isSportGroup = catNameLower.contains("sport") || catNameLower.contains(sportNorm)
            val isSpecificCompGroup = compNorm.isNotEmpty() && catNameLower.contains(compNorm)

            if (isSpecificCompGroup && sportKeywords.any { chNameLower.contains(it) }) {
                results.add(
                    ChannelMatchResult(
                        channel = channel,
                        confidence = MatchConfidence.LIKELY,
                        matchedReason = "Channel is in specific competition group: '${catNameLower.uppercase()}'"
                    )
                )
                continue
            }

            // 4. Likely match: Channel name contains the competition name + general sport matches
            if (chNameLower.contains(compNorm) && compNorm.length > 2) {
                results.add(
                    ChannelMatchResult(
                        channel = channel,
                        confidence = MatchConfidence.LIKELY,
                        matchedReason = "Channel broadcasts competition: '${event.competition}'"
                    )
                )
                continue
            }

            // 5. Likely match: Channel name is a dedicated channel for that specific sport or sport keywords
            val matchesSportKeywords = sportKeywords.any { chNameLower.contains(it) }
            val isSportGroupChannel = catNameLower.contains("sport") || catNameLower.contains(sportNorm)
            
            if (matchesSportKeywords && isSportGroupChannel) {
                // If it's a dedicated sport channel, e.g. contains Sky Sports Premier League and event is Football
                val matchesSubComp = chNameLower.contains(sportNorm) || (sportNorm == "football" && chNameLower.contains("premier league"))
                if (matchesSubComp) {
                    results.add(
                        ChannelMatchResult(
                            channel = channel,
                            confidence = MatchConfidence.LIKELY,
                            matchedReason = "Channel matches sport keywords: '${event.sport}'"
                        )
                    )
                } else {
                    results.add(
                        ChannelMatchResult(
                            channel = channel,
                            confidence = MatchConfidence.POSSIBLE,
                            matchedReason = "General Sports channel"
                        )
                    )
                }
                continue
            }

            // 6. Possible general sports channels as fallback
            if (chNameLower.contains("sport") || catNameLower.contains("sport")) {
                results.add(
                    ChannelMatchResult(
                        channel = channel,
                        confidence = MatchConfidence.POSSIBLE,
                        matchedReason = "General sport category fallback"
                    )
                )
            }
        }

        // Sort results: EXACT first, then LIKELY, then POSSIBLE, then by channel name
        return results.distinctBy { it.channel.id }.sortedWith(
            compareBy<ChannelMatchResult> {
                when (it.confidence) {
                    MatchConfidence.EXACT -> 0
                    MatchConfidence.LIKELY -> 1
                    MatchConfidence.POSSIBLE -> 2
                }
            }.thenBy { it.channel.name }
        )
    }
}
