package at.hannibal2.skyhanni.features.chat.playerchat

import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.MultiFilter
import at.hannibal2.skyhanni.utils.jsonobjects.PlayerChatFilterJson
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PlayerChatFilter {

    companion object {

        private val filters = mutableMapOf<String, MultiFilter>()

        fun shouldChatFilter(original: String): Boolean {
            val message = original.lowercase()
            for (filter in filters) {
                filter.value.matchResult(message)?.let {
                    return true
                }
            }

            return false
        }
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        filters.clear()
        var countCategories = 0
        var countFilters = 0

        val playerChatFilter = event.getConstant<PlayerChatFilterJson>("PlayerChatFilter")
        for (category in playerChatFilter.filters) {
            val description = category.description
            val filter = MultiFilter()
            filter.load(category)
            filters[description] = filter

            countCategories++
            countFilters += filter.count()
        }
    }
}
