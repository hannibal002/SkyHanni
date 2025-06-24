package at.hannibal2.skyhanni.data.jsonobjects.local

import at.hannibal2.skyhanni.features.garden.contest.EliteDevApi.EliteFarmingContest
import at.hannibal2.skyhanni.utils.KSerializable
import com.google.gson.annotations.Expose

@KSerializable
data class JacobContestsJson(
    @Expose var knownContests: List<EliteFarmingContest> = listOf()
)
