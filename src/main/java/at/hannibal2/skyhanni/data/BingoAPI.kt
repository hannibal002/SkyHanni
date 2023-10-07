package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.jsonobjects.BingoRanks
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object BingoAPI {
    private var ranks = mapOf<String, Int>()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        event.getConstant<BingoRanks>("BingoRanks")?.let {
            ranks = it.ranks
        }
    }

    fun getRank(text: String) = ranks.entries.find { text.contains(it.key) }?.value

    fun getIcon(searchRank: Int) = ranks.entries.find { it.value == searchRank }?.key

}