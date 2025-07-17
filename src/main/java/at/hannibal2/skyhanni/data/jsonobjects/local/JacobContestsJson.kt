package at.hannibal2.skyhanni.data.jsonobjects.local

import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteFarmingContest
import com.google.gson.annotations.Expose

data class JacobContestsJson(
    @Expose var knownContests: List<EliteFarmingContest> = listOf()
)
