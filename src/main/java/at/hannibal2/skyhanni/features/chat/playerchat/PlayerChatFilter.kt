package at.hannibal2.skyhanni.features.chat.playerchat

import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.MultiFilter
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

        val data = event.getConstant("PlayerChatFilter")
        for (category in data["filters"].asJsonArray) {
            val jsonObject = category.asJsonObject
            val description = jsonObject["description"].asString
            val filter = MultiFilter()
            filter.load(jsonObject)
            filters[description] = filter

            countCategories++
            countFilters += filter.count()
        }

        LorenzUtils.debug("Loaded $countFilters filters in $countCategories categories from repo")
    }
}
