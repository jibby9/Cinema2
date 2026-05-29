package com.example

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

// --- LATEST LIVE SPORTS DATA MODEL ---

data class TickerScore(
    val teamA: String,
    val teamB: String,
    val scoreA: Int,
    val scoreB: Int,
    val gameClock: String, // e.g. "72'", "Round 3", "Q4 05:22"
    val sport: String,
    val status: String // "LIVE", "UPCOMING", "FINISHED"
)

data class TickerStat(
    val label: String, // e.g. "Possession", "Shots (On Target)", "Corners", "Fouls", "Yellow/Red Cards", "Strikes", "Takedowns"
    val valueA: String,
    val valueB: String
)

data class TickerTimelineEvent(
    val id: String = UUID.randomUUID().toString(),
    val gameTime: String, // e.g. "45'", "12'", "Round 2 1:30"
    val title: String, // e.g. "GOAL!", "Yellow Card", "Substitution", "Knockdown", "Corner Kick"
    val detail: String, // e.g. "Bukayo Saka (Arsenal)", "Casemiro (Foul)", etc.
    val relativeTimeSec: Int, // seconds elapsed since event started in real life (to calculate delay alignment)
    val teamSide: String? = null // "A" or "B" (used for coloring/aligning updates)
)

interface LiveTickerRepository {
    fun getRealTimeScore(eventId: String): TickerScore
    fun getRealTimeStats(eventId: String): List<TickerStat>
    fun getRealTimeTimeline(eventId: String): List<TickerTimelineEvent>
}

// Custom stream delay configuration model
data class TickerSyncConfig(
    val isEnabled: Boolean = true,
    val delaySeconds: Int = 30, // Default 30 seconds stream delay
    val autoDelayDetection: Boolean = false,
    val tickerPosition: TickerPosition = TickerPosition.BOTTOM
)

enum class TickerPosition {
    TOP, BOTTOM
}

class StaticLiveTickerRepository : LiveTickerRepository {
    
    // Generates real-time streams based on the current system time to simulate a truly live broadcast
    override fun getRealTimeScore(eventId: String): TickerScore {
        val secondOfDay = (System.currentTimeMillis() / 1000) % 86400
        val gameMinute = ((secondOfDay / 15) % 90).toInt() + 1 // speed-up to match game progression realistically
        
        return when {
            eventId.contains("Jones") || eventId.contains("309") -> {
                val roundNum = ((gameMinute / 5) % 5) + 1
                val secondsRemaining = 300 - ((secondOfDay % 300) % 300).toInt()
                val minForm = secondsRemaining / 60
                val secForm = secondsRemaining % 60
                TickerScore(
                    teamA = "Jon Jones",
                    teamB = "Stipe Miocic",
                    scoreA = 2,
                    scoreB = 0,
                    gameClock = "Round $roundNum - %d:%02d".format(minForm, secForm),
                    sport = "UFC",
                    status = "LIVE"
                )
            }
            eventId.contains("Fury") || eventId.contains("Usyk") -> {
                val roundNum = ((gameMinute / 3) % 12) + 1
                val secondsRemaining = 180 - ((secondOfDay % 180) % 180).toInt()
                val minForm = secondsRemaining / 60
                val secForm = secondsRemaining % 60
                TickerScore(
                    teamA = "Tyson Fury",
                    teamB = "Oleksandr Usyk",
                    scoreA = 4,
                    scoreB = 4,
                    gameClock = "Round $roundNum - %d:%02d".format(minForm, secForm),
                    sport = "Boxing",
                    status = "LIVE"
                )
            }
            eventId.contains("Celtics") || eventId.contains("NBA") -> {
                val quarterNum = ((gameMinute / 12) % 4) + 1
                val quarterMin = 11 - (gameMinute % 12)
                val quarterSec = (60 - (secondOfDay % 60)).toInt() % 60
                val pointsA = 80 + (gameMinute * 1.8).toInt()
                val pointsB = 78 + (gameMinute * 1.9).toInt()
                TickerScore(
                    teamA = "Boston Celtics",
                    teamB = "Dallas Mavericks",
                    scoreA = pointsA,
                    scoreB = pointsB,
                    gameClock = "Q$quarterNum %d:%02d".format(quarterMin, quarterSec),
                    sport = "Basketball",
                    status = "LIVE"
                )
            }
            eventId.contains("Formula 1") || eventId.contains("Monaco") -> {
                val lap = ((secondOfDay / 10) % 78).toInt() + 1
                TickerScore(
                    teamA = "Max Verstappen",
                    teamB = "Charles Leclerc",
                    scoreA = 1, // Leader position
                    scoreB = 2, // Pos 2 Gap (+1.4s)
                    gameClock = "Lap $lap/78",
                    sport = "Motorsport",
                    status = "LIVE"
                )
            }
            else -> {
                // Default Football / Soccer simulation
                val goalsA = if (gameMinute > 15 && gameMinute < 65) 1 else if (gameMinute >= 65) 2 else 0
                val goalsB = if (gameMinute > 42) 1 else 0
                TickerScore(
                    teamA = "Arsenal",
                    teamB = "Chelsea",
                    scoreA = goalsA,
                    scoreB = goalsB,
                    gameClock = "$gameMinute'",
                    sport = "Football",
                    status = "LIVE"
                )
            }
        }
    }

    override fun getRealTimeStats(eventId: String): List<TickerStat> {
        val secondOfDay = (System.currentTimeMillis() / 1000) % 86400
        val progress = (secondOfDay % 100).toInt()
        
        return when {
            eventId.contains("Jones") || eventId.contains("309") || eventId.contains("UFC") -> {
                listOf(
                    TickerStat("Significant Strikes", "${42 + progress / 4} / 68", "${31 + progress / 5} / 55"),
                    TickerStat("Takedowns", "2/4", "0/1"),
                    TickerStat("Submission Attempts", "1", "0"),
                    TickerStat("Control Time", "2:41", "0:12"),
                    TickerStat("Leg Kicks", "8", "12")
                )
            }
            eventId.contains("Fury") || eventId.contains("Usyk") || eventId.contains("Boxing") -> {
                listOf(
                    TickerStat("Jabs Landed", "${58 + progress / 5}", "${71 + progress / 4}"),
                    TickerStat("Power Punches", "${84 + progress / 3}", "${72 + progress / 3}"),
                    TickerStat("Total Strikes", "${142 + progress / 2}", "${143 + progress / 2}"),
                    TickerStat("Knockdowns", "0", "1")
                )
            }
            eventId.contains("Celtics") || eventId.contains("NBA") -> {
                listOf(
                    TickerStat("Field Goal %", "${48 + progress % 3}%", "${45 + progress % 4}%"),
                    TickerStat("3-Pointers", "12/28", "10/31"),
                    TickerStat("Rebounds", "${34 + progress / 10}", "${31 + progress / 9}"),
                    TickerStat("Assists", "${21 + progress / 12}", "${18 + progress / 11}"),
                    TickerStat("Turnovers", "${8 + progress / 25}", "${11 + progress / 20}")
                )
            }
            eventId.contains("Formula 1") || eventId.contains("Monaco") -> {
                listOf(
                    TickerStat("Leader Gap", "Leader", "+1.872s"),
                    TickerStat("Pitstops", "1", "1"),
                    TickerStat("Fastest Lap", "1:15.284", "1:15.011"),
                    TickerStat("Tyre Compound", "Hard (22 Laps)", "Hard (24 Laps)"),
                    TickerStat("Top Speed", "298 km/h", "302 km/h")
                )
            }
            else -> {
                // Football default stats
                val posA = 52 + (progress % 5) - 2
                val posB = 100 - posA
                val shotsA = 8 + progress / 12
                val shotsB = 5 + progress / 15
                listOf(
                    TickerStat("Possession", "$posA%", "$posB%"),
                    TickerStat("Shots (On Target)", "$shotsA (${shotsA / 2})", "$shotsB (${shotsB / 3})"),
                    TickerStat("Fouls", "8", "12"),
                    TickerStat("Corners", "5", "3"),
                    TickerStat("Yellow / Red Cards", "1/0", "3/0")
                )
            }
        }
    }

    override fun getRealTimeTimeline(eventId: String): List<TickerTimelineEvent> {
        // We output a rich list of static timeline events with relative times (in seconds since game began).
        // The delay synchronization system will buffer these and only release them when event's original elapsed real-world time matches!
        // We'll calibrate relative elapsed seconds so that we have events spreading across hours. This ensures some are already displayed and some are upcoming.
        val baseList = when {
            eventId.contains("Jones") || eventId.contains("309") || eventId.contains("UFC") -> {
                listOf(
                    TickerTimelineEvent("e1", "Round 1 1:12", "FIGHT START", "Main event match commenced.", 10, null),
                    TickerTimelineEvent("e2", "Round 1 2:15", "Takedown", "Jon Jones executes clean double-leg takedown.", 135, "A"),
                    TickerTimelineEvent("e3", "Round 1 4:10", "Submission Attempt", "Jon Jones attempts Guillotine choke.", 250, "A"),
                    TickerTimelineEvent("e4", "Round 2 0:00", "Round 2 Starts", "Second round underway.", 320, null),
                    TickerTimelineEvent("e5", "Round 2 1:30", "Significant Strike", "Stipe Miocic lands clean right hook counter.", 410, "B"),
                    TickerTimelineEvent("e6", "Round 2 3:45", "Knockdown", "Jon Jones drops Miocic with a massive body kick!", 545, "A"),
                    TickerTimelineEvent("e7", "Round 3 0:00", "Round 3 Starts", "Championship round 3 begins.", 620, null),
                    TickerTimelineEvent("e8", "Round 3 2:15", "TKO / TECHNICAL KNOCKOUT", "Jon Jones finishes the match with heavy ground & pound!", 755, "A")
                )
            }
            eventId.contains("Fury") || eventId.contains("Usyk") || eventId.contains("Boxing") -> {
                listOf(
                    TickerTimelineEvent("b1", "Round 1 0:00", "BOUT BEGINS", "Heavyweight championship fight bell.", 5, null),
                    TickerTimelineEvent("b2", "Round 2 1:15", "Significant Strike", "Oleksandr Usyk lands sharp lead jab.", 255, "B"),
                    TickerTimelineEvent("b3", "Round 4 2:05", "Knockdown", "Tyson Fury is rocked and falls back, gets counted to 8!", 605, "B"),
                    TickerTimelineEvent("b4", "Round 6 1:50", "Warning", "Referee issues strong warning to Fury for low blow.", 950, "A"),
                    TickerTimelineEvent("b5", "Round 8 2:50", "Clinched Strike", "Tyson Fury scores big right uppercut inside.", 1310, "A")
                )
            }
            eventId.contains("Celtics") || eventId.contains("NBA") -> {
                listOf(
                    TickerTimelineEvent("n1", "Q1 12:00", "TIPOFT", "Saka wins the tipoff, Celtics possession.", 10, "A"),
                    TickerTimelineEvent("n2", "Q1 09:12", "3-Pointer", "Jayson Tatum sinks pristine step-back 3 pointer.", 175, "A"),
                    TickerTimelineEvent("n3", "Q1 05:40", "Slam Dunk", "Kyrie Irving powers past defense for a superb reverse jam.", 380, "B"),
                    TickerTimelineEvent("n4", "Q2 11:30", "Technical Foul", "Luka Doncic receives tech foul for arguing with referee.", 720, "B"),
                    TickerTimelineEvent("n5", "Q2 04:10", "Highlight Play", "Jaylen Brown triggers fastbreak and completes standard alley-oop.", 1150, "A")
                )
            }
            eventId.contains("Formula 1") || eventId.contains("Monaco") -> {
                listOf(
                    TickerTimelineEvent("m1", "Lap 1", "RACE START", "The five lights are out and we are green in Monaco!", 10, null),
                    TickerTimelineEvent("m2", "Lap 2", "Fastest Lap", "Charles Leclerc sets initial fastest lap 1:16.822.", 95, "B"),
                    TickerTimelineEvent("m3", "Lap 15", "Pit Stop", "Max Verstappen pits for Hard tires (2.4 seconds).", 900, "A"),
                    TickerTimelineEvent("m4", "Lap 22", "Yellow Sector", "Yellow flag in Sector 2 as Hamilton clips the barrier.", 1320, null),
                    TickerTimelineEvent("m5", "Lap 45", "Fastest Lap", "Oscar Piastri clocks record lap 1:15.011.", 2700, "B")
                )
            }
            else -> {
                // Default Football Timeline
                listOf(
                    TickerTimelineEvent("f1", "01'", "KICKOFF", "Match starts at the Emirates Stadium.", 10, null),
                    TickerTimelineEvent("f2", "12'", "Yellow Card", "Enzo Fernandez (Chelsea) booked for sliding tackle.", 720, "B"),
                    TickerTimelineEvent("f3", "27'", "GOAL!", "Arsenal 1 - [0] Bukayo Saka. Curled beauty from edge of box!", 1620, "A"),
                    TickerTimelineEvent("f4", "38'", "Corner Kick", "Chelsea forces close defense clearance.", 2280, "B"),
                    TickerTimelineEvent("f5", "45+2'", "HALF TIME", "Arsenal leads 1-0 in a fast paced derby.", 2750, null),
                    TickerTimelineEvent("f6", "46'", "Substitution", "Raheem Sterling replaces Mudryk.", 2900, "B"),
                    TickerTimelineEvent("f7", "58'", "GOAL!", "Arsenal 1 - [1] Nicolas Jackson header from James cross.", 3480, "B"),
                    TickerTimelineEvent("f8", "72'", "GOAL!", "Arsenal [2] - 1 Martin Odegaard penalty shot!", 4320, "A"),
                    TickerTimelineEvent("f9", "88'", "Red Card", "Conor Gallagher gets second yellow card! Off!", 5280, "B")
                )
            }
        }
        return baseList
    }
}

// --- DELAYED SPORTS BUFFER ENGINE ---

class LiveSportsTickerManager(
    private val repository: LiveTickerRepository = StaticLiveTickerRepository()
) {
    // Event loop for simulating ticks and buffer projection
    private val _scoreFlow = MutableStateFlow<TickerScore?>(null)
    val scoreFlow: StateFlow<TickerScore?> = _scoreFlow.asStateFlow()

    private val _statsFlow = MutableStateFlow<List<TickerStat>>(emptyList())
    val statsFlow: StateFlow<List<TickerStat>> = _statsFlow.asStateFlow()

    private val _timelineFlow = MutableStateFlow<List<TickerTimelineEvent>>(emptyList())
    val timelineFlow: StateFlow<List<TickerTimelineEvent>> = _timelineFlow.asStateFlow()

    private var currentEventId: String? = null
    
    // Config state
    private val _syncConfig = MutableStateFlow(TickerSyncConfig())
    val syncConfig: StateFlow<TickerSyncConfig> = _syncConfig.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var activeTickerRunnable: Runnable? = null
    
    // Track when the user started tuned/mock start of current active video playback
    private var streamPlaybackSessionStartMs: Long = System.currentTimeMillis()

    fun updateConfig(newConfig: TickerSyncConfig) {
        _syncConfig.value = newConfig
        triggerRecalibrate()
    }

    fun startTrackingEvent(eventId: String) {
        currentEventId = eventId
        streamPlaybackSessionStartMs = System.currentTimeMillis()
        restartPolling()
    }

    fun stopTracking() {
        activeTickerRunnable?.let { handler.removeCallbacks(it) }
        activeTickerRunnable = null
        currentEventId = null
    }

    fun triggerRecalibrate() {
        // Calibrates playback session so that timing alignment is forced
        streamPlaybackSessionStartMs = System.currentTimeMillis()
        refreshProjections()
    }

    private fun restartPolling() {
        activeTickerRunnable?.let { handler.removeCallbacks(it) }
        
        val runnable = object : Runnable {
            override fun run() {
                refreshProjections()
                handler.postDelayed(this, 1000) // update ticker projection state dynamically every second safely!
            }
        }
        activeTickerRunnable = runnable
        handler.post(runnable)
    }

    private fun refreshProjections() {
        val eventId = currentEventId ?: return
        val config = _syncConfig.value

        // Retrieve real-time absolute raw states from repository
        val rawScore = repository.getRealTimeScore(eventId)
        val rawStats = repository.getRealTimeStats(eventId)
        val rawTimeline = repository.getRealTimeTimeline(eventId)

        // If spoiler-safe delay matches is disabled, project the live state immediately without buffer delay
        if (!config.isEnabled) {
            _scoreFlow.value = rawScore
            _statsFlow.value = rawStats
            _timelineFlow.value = rawTimeline
            return
        }

        // --- CALCULATE STREAM PLAYTIME PROJECTED BY CONFIG DELAY ---
        // Let's compute how many seconds have elapsed in real-world time since the user started watching/monitoring.
        // We subtract the streamDelay config to see what events are allowed to be revealed!
        val elapsedRealWorldSec = ((System.currentTimeMillis() - streamPlaybackSessionStartMs) / 1000).toInt()
        
        // Let's safe-guard so that we always show at least the initial event
        val delayedElapsedSec = maxOf(10, elapsedRealWorldSec - config.delaySeconds)

        // Filter the timeline play-by-play events that should be visible under this delayed elapsed playtime
        val filteredTimeline = rawTimeline.filter { it.relativeTimeSec <= delayedElapsedSec }
        _timelineFlow.value = filteredTimeline

        // Project delayed live scores. The overall score depends heavily on which goals or points occurred before our visible timeline window!
        val derivedScore = calculateDerivedScore(rawScore, filteredTimeline)
        _scoreFlow.value = derivedScore

        // Derive stats safely based on timeline coverage ratio
        val totalRawTimelineCount = rawTimeline.size
        val visibleRatio = if (totalRawTimelineCount > 0) {
            filteredTimeline.size.toFloat() / totalRawTimelineCount.toFloat()
        } else {
            1.0f
        }
        
        val derivedStats = rawStats.map { stat ->
            val valANum = stat.valueA.filter { it.isDigit() }.toIntOrNull() ?: 0
            val valBNum = stat.valueB.filter { it.isDigit() }.toIntOrNull() ?: 0
            if (valANum > 0 || valBNum > 0) {
                // Scale values proportionally with timeline progress to keep stats fully synced with delayed timeline
                val currentA = maxOf(0, (valANum * visibleRatio).toInt())
                val currentB = maxOf(0, (valBNum * visibleRatio).toInt())
                val isPercentage = stat.valueA.contains("%")
                
                if (isPercentage) {
                    stat // Keep percentage as is
                } else {
                    TickerStat(
                        label = stat.label,
                        valueA = currentA.toString() + stat.valueA.replace(valANum.toString(), ""),
                        valueB = currentB.toString() + stat.valueB.replace(valBNum.toString(), "")
                    )
                }
            } else {
                stat
            }
        }
        _statsFlow.value = derivedStats
    }

    private fun calculateDerivedScore(raw: TickerScore, visibleTimeline: List<TickerTimelineEvent>): TickerScore {
        // Let's scan our visible timeline events. The current score is derived by simply counting the visible "GOAL!" / "Knockdown" updates!
        // This ensures the user NEVER sees "2 - 1" in the score ticker if the second goal event hasn't elapsed in their delayed stream yet!
        val scoreA = visibleTimeline.count { it.title == "GOAL!" && it.teamSide == "A" }
        val scoreB = visibleTimeline.count { it.title == "GOAL!" && it.teamSide == "B" }
        
        // For combat sports (UFC/Boxing), count knockdowns or round indicators
        val knockdownA = visibleTimeline.count { it.title == "Knockdown" && it.teamSide == "A" }
        val knockdownB = visibleTimeline.count { it.title == "Knockdown" && it.teamSide == "B" }

        // Find the clock corresponding to the last visible item to match the timeline progress!
        val latestShow = visibleTimeline.lastOrNull()
        val latestClock = latestShow?.gameTime ?: "01'"

        return when (raw.sport) {
            "Football" -> {
                raw.copy(scoreA = scoreA, scoreB = scoreB, gameClock = latestClock)
            }
            "UFC", "Boxing" -> {
                raw.copy(scoreA = knockdownA, scoreB = knockdownB, gameClock = latestClock)
            }
            "Basketball" -> {
                // NBA scales points directly with visible elapsed countdown
                raw
            }
            else -> {
                raw.copy(gameClock = latestClock)
            }
        }
    }
}
