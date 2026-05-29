package com.example

import java.util.UUID
import java.time.Instant
import java.util.Locale
import android.util.Log

// Static logging / diagnostics object to trace fixture validation and duplicate filtering
object FixtureDiagnostics {
    private val logs = mutableListOf<String>()

    @Synchronized
    fun log(message: String) {
        val currentTimestamp = Instant.now().toString()
        val logLine = "[$currentTimestamp] $message"
        logs.add(logLine)
        Log.d("FixtureDiagnostics", message)
    }

    @Synchronized
    fun getLogs(): List<String> {
        return logs.toList()
    }

    @Synchronized
    fun clear() {
        logs.clear()
    }
}

data class SportsEvent(
    val id: String = UUID.randomUUID().toString(),
    val sport: String, // "Football", "Boxing", "UFC", "Basketball", "American Football", "Baseball", "Hockey", "Tennis", "Darts", "Snooker", "Motorsport"
    val competition: String,
    val title: String,
    val dateTimeMs: Long,
    val teamA: String?,
    val teamB: String?,
    val description: String? = null,
    val confidence: Float = 1.0f,
    val providersMatched: List<String> = emptyList(),
    val isValidated: Boolean = true
) {
    fun getStatus(): String {
        val now = System.currentTimeMillis()
        val duration = when (sport) {
            "Football" -> 120 * 60 * 1000L // 2 hours
            "Basketball" -> 150 * 60 * 1000L // 2.5 hours
            "Motorsport" -> 180 * 60 * 1000L // 3 hours
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

// Representing dirty raw incoming telemetry feed item from multiple sources
data class RawFeedFixture(
    val provider: String,
    val sport: String,
    val competition: String,
    val homeTeam: String?,
    val awayTeam: String?,
    val title: String,
    val startTimeIso: String, // ISO-8601 string
    val trustLevel: Float,
    val remarks: String? = null
)

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
        FixtureDiagnostics.clear()
        FixtureDiagnostics.log("=== Initiating Sports Fixture Validation Pipeline ===")

        val now = System.currentTimeMillis()

        // Simulated noisy incoming feeds in local and global timezone variants containing duplicates and loose titles
        val rawFeeds = listOf(
            // Primary feed
            RawFeedFixture(
                provider = "Global_Sports_Data_API",
                sport = "Football",
                competition = "Premier League",
                homeTeam = "Arsenal",
                awayTeam = "Chelsea",
                title = "Arsenal vs Chelsea",
                startTimeIso = Instant.ofEpochMilli(now - 30 * 60 * 1000L).toString(), // Started 30 mins ago
                trustLevel = 0.98f,
                remarks = "Premium fixture stream"
            ),
            // Duplicate overlapping fixture with slightly dirty names from high-trust feed
            RawFeedFixture(
                provider = "Feed_Sports_Matrix",
                sport = "Football",
                competition = "English Premier League",
                homeTeam = "Arsenal FC",
                awayTeam = "Chelsea FC",
                title = "Arsenal FC vs Chelsea FC",
                startTimeIso = Instant.ofEpochMilli(now - 30 * 60 * 1000L).toString(),
                trustLevel = 0.95f
            ),
            // Another high trust fixture
            RawFeedFixture(
                provider = "Global_Sports_Data_API",
                sport = "Football",
                competition = "UEFA Champions League",
                homeTeam = "Real Madrid",
                awayTeam = "Manchester City",
                title = "Real Madrid vs Manchester City",
                startTimeIso = Instant.ofEpochMilli(now + 90 * 60 * 1000L).toString(), // In 1.5 hours
                trustLevel = 0.99f
            ),
            // Duplicate with timezone offset mismatch simulation - 10 min drift
            RawFeedFixture(
                provider = "Feed_Sports_Matrix",
                sport = "Football",
                competition = "Champions League - Live",
                homeTeam = "Real Madrid CF",
                awayTeam = "Man City",
                title = "Real Madrid CF v Manchester City",
                startTimeIso = Instant.ofEpochMilli(now + 90 * 60 * 1000L + 10 * 60 * 1000L).toString(),
                trustLevel = 0.92f
            ),
            // Low confidence incorrect fixture (missing teams) - should be suppressed
            RawFeedFixture(
                provider = "Amateur_SportsGroup_Feed",
                sport = "Football",
                competition = "Unknown Cup",
                homeTeam = null,
                awayTeam = "",
                title = "Some Unranked Football Match",
                startTimeIso = Instant.ofEpochMilli(now + 4 * 3600 * 1000L).toString(),
                trustLevel = 0.35f,
                remarks = "User submitted"
            ),
            // Valid boxing
            RawFeedFixture(
                provider = "Global_Sports_Data_API",
                sport = "Boxing",
                competition = "Heavyweight Title",
                homeTeam = "Tyson Fury",
                awayTeam = "Oleksandr Usyk",
                title = "Tyson Fury vs Oleksandr Usyk",
                startTimeIso = Instant.ofEpochMilli(now + 6 * 3600 * 1000L).toString(),
                trustLevel = 0.95f
            ),
            // Valid UFC Live now
            RawFeedFixture(
                provider = "Global_Sports_Data_API",
                sport = "UFC",
                competition = "UFC 309",
                homeTeam = "Jon Jones",
                awayTeam = "Stipe Miocic",
                title = "Jon Jones vs Stipe Miocic",
                startTimeIso = Instant.ofEpochMilli(now - 60 * 60 * 1000L).toString(),
                trustLevel = 0.97f
            ),
            // Valid Esports Event
            RawFeedFixture(
                provider = "Feed_Sports_Matrix",
                sport = "Basketball",
                competition = "NBA Playoffs",
                homeTeam = "Boston Celtics",
                awayTeam = "Dallas Mavericks",
                title = "Boston Celtics vs Dallas Mavericks",
                startTimeIso = Instant.ofEpochMilli(now + 45 * 60 * 1000L).toString(),
                trustLevel = 0.99f
            ),
            // Overlapping slightly mismatching timezone drift
            RawFeedFixture(
                provider = "Feed_Sport_Beta",
                sport = "Basketball",
                competition = "NBA",
                homeTeam = "Celtics",
                awayTeam = "Mavericks",
                title = "Boston vs Dallas",
                startTimeIso = Instant.ofEpochMilli(now + 42 * 60 * 1000L).toString(),
                trustLevel = 0.85f
            ),
            // Valid Motorsport
            RawFeedFixture(
                provider = "Global_Sports_Data_API",
                sport = "Motorsport",
                competition = "Formula 1",
                homeTeam = "Red Bull Racing",
                awayTeam = "Scuderia Ferrari",
                title = "Monaco Grand Prix - Main Race",
                startTimeIso = Instant.ofEpochMilli(now - 15 * 60 * 1000L).toString(),
                trustLevel = 0.92f
            ),
            // Tennis
            RawFeedFixture(
                provider = "Global_Sports_Data_API",
                sport = "Tennis",
                competition = "Wimbledon",
                homeTeam = "Carlos Alcaraz",
                awayTeam = "Novak Djokovic",
                title = "Carlos Alcaraz vs Novak Djokovic",
                startTimeIso = Instant.ofEpochMilli(now + 2 * 24 * 3600 * 1000L).toString(),
                trustLevel = 0.96f
            ),
            // Another amateur feed with high drift that should be deduplicated elegantly
            RawFeedFixture(
                provider = "Amateur_SportsGroup_Feed",
                sport = "Tennis",
                competition = "Wimbledon Final",
                homeTeam = "Alcaraz",
                awayTeam = "Djokovic",
                title = "Alcaraz vs Djokovic",
                startTimeIso = Instant.ofEpochMilli(now + 2 * 24 * 3600 * 1000L + 5 * 60 * 1000L).toString(),
                trustLevel = 0.50f
            ),
            // Low trust unknown boxing event which has very shady confidence mapping
            RawFeedFixture(
                provider = "Low_Trust_Feed_X",
                sport = "Boxing",
                competition = "Local Fight",
                homeTeam = "Unknown Fighter A",
                awayTeam = "Unknown Fighter B",
                title = "Unverified Local Matchup",
                startTimeIso = Instant.ofEpochMilli(now + 12 * 3600 * 1000L).toString(),
                trustLevel = 0.40f
            )
        )

        // PROCESSING PIPELINE:
        val validatedEvents = mutableListOf<SportsEvent>()

        for (raw in rawFeeds) {
            FixtureDiagnostics.log("Processing incoming fixture: '${raw.title}' [Provider: ${raw.provider}]")

            // 1. Strict Validation Rules Check
            if (raw.sport.isBlank()) {
                FixtureDiagnostics.log("  -> REJECTED: Missing sport category.")
                continue
            }
            if (raw.homeTeam.isNullOrBlank() || raw.awayTeam.isNullOrBlank()) {
                FixtureDiagnostics.log("  -> REJECTED: Home team or away team is empty.")
                continue
            }

            // Timezone safe parsers using standard Instant parsing
            val eventTimeMs = try {
                Instant.parse(raw.startTimeIso).toEpochMilli()
            } catch (e: Exception) {
                FixtureDiagnostics.log("  -> REJECTED: Unable to parse ISO kickoff timestamp '${raw.startTimeIso}' safely.")
                continue
            }

            // 2. Compute Confidence Score for Single Fixture
            var currentConfidence = raw.trustLevel

            // Penalty for amateur or suspicious setups
            if (raw.competition.lowercase().contains("unknown") || raw.competition.lowercase().contains("unverified")) {
                currentConfidence -= 0.20f
            }
            if (raw.homeTeam.lowercase().contains("unknown") || raw.awayTeam.lowercase().contains("unknown")) {
                currentConfidence -= 0.30f
            }

            // Suppress if confidence is too low (e.g. below 0.65 threshold)
            if (currentConfidence < 0.65f) {
                FixtureDiagnostics.log("  -> SUPPRESSED: Low initial confidence block (${String.format(Locale.US, "%.2f", currentConfidence)}) for '${raw.title}'.")
                continue
            }

            // 3. Duplicate and Time Drift Matching Check
            val isDuplicate = validatedEvents.any { existing ->
                // Check if they belong to same sport
                val sameSport = existing.sport.lowercase() == raw.sport.lowercase()

                // Check proximity window (kickoffs within 2 hours of each other)
                val timeDifferenceDelta = Math.abs(existing.dateTimeMs - eventTimeMs)
                val isWithinTimeWindow = timeDifferenceDelta < (2 * 3600 * 1000L) // 2 Hour overlap window

                if (sameSport && isWithinTimeWindow) {
                    // Token-based team similarity calculation to prevent fuzzy matching failures on generic flags like "FC"
                    val simA = calculateTokenSimilarity(existing.teamA ?: "", raw.homeTeam)
                    val simB = calculateTokenSimilarity(existing.teamB ?: "", raw.awayTeam)
                    val isFuzzyMatch = (simA > 0.70f && simB > 0.70f)

                    if (isFuzzyMatch) {
                        FixtureDiagnostics.log("  -> IDENTIFIED DUPLICATE: Similarity detected against '${existing.title}'. Sim: ${String.format(Locale.US, "%.2f / %.2f", simA, simB)}, Time Drift: ${timeDifferenceDelta / 60000} mins.")
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }

            if (isDuplicate) {
                // Find existing items to merge origin provider telemetry and maximize confidence score
                val targetIndex = validatedEvents.indexOfFirst { existing ->
                    existing.sport.lowercase() == raw.sport.lowercase() &&
                    calculateTokenSimilarity(existing.teamA ?: "", raw.homeTeam ?: "") > 0.70f &&
                    calculateTokenSimilarity(existing.teamB ?: "", raw.awayTeam ?: "") > 0.70f &&
                    Math.abs(existing.dateTimeMs - eventTimeMs) < (2 * 3600 * 1000L)
                }

                if (targetIndex != -1) {
                    val existing = validatedEvents[targetIndex]
                    // If current raw feed has higher trust, we update key fields (preferring the higher trust kickoff time and league names!)
                    if (currentConfidence > existing.confidence) {
                        FixtureDiagnostics.log("  -> MERGE & UPGRADE: Upgraded details from duplicate stream.")
                        validatedEvents[targetIndex] = existing.copy(
                            competition = if (raw.competition.length > existing.competition.length) raw.competition else existing.competition,
                            dateTimeMs = eventTimeMs, // Update to more accurate provider time
                            confidence = currentConfidence,
                            providersMatched = existing.providersMatched + raw.provider
                        )
                    } else {
                        FixtureDiagnostics.log("  -> DEDUPLICATED: Primary item has superior trust. Skipping raw item.")
                        validatedEvents[targetIndex] = existing.copy(
                            providersMatched = existing.providersMatched + raw.provider
                        )
                    }
                }
                continue
            }

            // Approved and validated fixture! Record to state
            val canonicalEvent = SportsEvent(
                id = UUID.randomUUID().toString(),
                sport = raw.sport,
                competition = raw.competition,
                title = "${raw.homeTeam} vs ${raw.awayTeam}",
                dateTimeMs = eventTimeMs,
                teamA = raw.homeTeam,
                teamB = raw.awayTeam,
                description = "Live event certified by AI scheduling. Conf: ${String.format(Locale.US, "%.2f", currentConfidence)}.",
                confidence = currentConfidence,
                providersMatched = listOf(raw.provider),
                isValidated = true
            )

            validatedEvents.add(canonicalEvent)
            FixtureDiagnostics.log("  -> APPROVED & STORED: '${canonicalEvent.title}' [Conf: ${String.format(Locale.US, "%.2f", currentConfidence)}]")
        }

        FixtureDiagnostics.log("=== Complete: ${validatedEvents.size} verified sports fixtures stored ===")
        eventsList = validatedEvents.sortedBy { it.dateTimeMs }
    }

    // Advanced token similarity scorer to avoid simple false positives
    private fun calculateTokenSimilarity(s1: String, s2: String): Float {
        val clean1 = s1.lowercase().replace("fc", "").replace("cf", "").replace("united", "").replace("city", "").trim()
        val clean2 = s2.lowercase().replace("fc", "").replace("cf", "").replace("united", "").replace("city", "").trim()

        val tokens1 = clean1.split(" ", "_", "-").filter { it.isNotBlank() && it.length > 2 }.toSet()
        val tokens2 = clean2.split(" ", "_", "-").filter { it.isNotBlank() && it.length > 2 }.toSet()

        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            return if (clean1.contains(clean2) || clean2.contains(clean1)) 0.80f else 0.0f
        }

        val intersectionCount = tokens1.intersect(tokens2).size
        return (intersectionCount.toFloat() / Math.max(tokens1.size, tokens2.size))
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
        
        val teamANorm = event.teamA?.lowercase()?.trim() ?: ""
        val teamBNorm = event.teamB?.lowercase()?.trim() ?: ""
        val titleNorm = event.title.lowercase().trim()
        val compNorm = event.competition.lowercase().trim()
        val sportNorm = event.sport.lowercase().trim()

        val catMap = categories.associate { it.id to it.name.lowercase() }

        // Compile clean team keyword matcher: avoids false positive from "FC" or "United"
        fun checkMatch(text: String): Boolean {
            val lower = text.lowercase()
            
            // Substring checks of specific team tokens
            val checkTokens = listOf(teamANorm, teamBNorm).filter { it.length > 3 }
            for (tok in checkTokens) {
                // If text contains the team token we count as matched
                if (lower.contains(tok)) return true
            }
            if (lower.contains(titleNorm)) return true
            return false
        }

        // Broad sport keywords map
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
                // If EPG runs within the window of the event (dateTimeMs +/- 2 hours)
                val windowStart = event.dateTimeMs - 3600000L * 2
                val windowEnd = event.dateTimeMs + 3600000L * 3
                val overlap = Math.max(epg.startMs, windowStart) < Math.min(epg.endMs, windowEnd)
                
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
                        matchedReason = "Channel matches event teams"
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
                        matchedReason = "Category is Specific competition: '${catNameLower.uppercase()}'"
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
                val matchesSubComp = chNameLower.contains(sportNorm) || (sportNorm == "football" && chNameLower.contains("premier league"))
                if (matchesSubComp) {
                    results.add(
                        ChannelMatchResult(
                            channel = channel,
                            confidence = MatchConfidence.LIKELY,
                            matchedReason = "Dedicated sport stream: '${event.sport}'"
                        )
                    )
                } else {
                    results.add(
                        ChannelMatchResult(
                            channel = channel,
                            confidence = MatchConfidence.POSSIBLE,
                            matchedReason = "General Sports Broadcast"
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
                        matchedReason = "General sport category"
                    )
                )
            }
        }

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
