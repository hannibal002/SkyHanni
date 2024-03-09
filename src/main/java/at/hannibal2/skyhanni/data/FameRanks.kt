package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.data.jsonobjects.repo.FameRankJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object FameRanks {
    var fameRanks = emptyMap<String, FameRank>()
        private set

    fun getFameRankByNameOrNull(name: String) = fameRanks[name]

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val ranks = event.getConstant<FameRankJson>("FameRank")
        fameRanks = ranks.fame_rank.values.map { FameRank(it.name, it.fame_required, it.bits_multiplier, it.votes) }
            .associateBy { it.name }
    }
}

data class FameRank(
    val name: String,
    val fameRequired: Int,
    val bitsMultiplier: Double,
    val electionVotes: Int
)
