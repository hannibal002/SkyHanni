package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.jsonobjects.BingoRanksJson
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object BingoAPI {
    private var ranks = mapOf<String, Int>()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        event.getConstant<BingoRanksJson>("BingoRanks")?.let { data ->
            ranks = data.ranks
            SkyHanniMod.repo.successfulConstants.add("BingoRanks")
        } ?: run {
            SkyHanniMod.repo.unsuccessfulConstants.add("BingoRanks")
        }
    }

    fun getRank(text: String) = ranks.entries.find { text.contains(it.key) }?.value

    fun getIcon(searchRank: Int) = ranks.entries.find { it.value == searchRank }?.key

}