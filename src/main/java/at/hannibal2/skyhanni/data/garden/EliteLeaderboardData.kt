package at.hannibal2.skyhanni.data.garden

import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboard
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardPlayer
import at.hannibal2.skyhanni.utils.SimpleTimeMark

@Suppress("EmptyClassBlock")
data class EliteLeaderboardData(
    var lastUpdate: SimpleTimeMark = SimpleTimeMark.farPast(),
    var shouldRefresh: Boolean = true,
    var lastPlayer: EliteLeaderboardPlayer? = null,
    val nextPlayers: MutableList<EliteLeaderboardPlayer> = mutableListOf(),
    var apiData: EliteLeaderboard? = null,
    var isUnranked: Boolean = false,
    var rankGoal: Int? = null,
    val passedPlayers: MutableList<String> = mutableListOf()
)
