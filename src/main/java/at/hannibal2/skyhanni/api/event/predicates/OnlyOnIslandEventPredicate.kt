package at.hannibal2.skyhanni.api.event.predicates

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.SkyBlockUtils.inAnyIsland
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.toEnumSet

object OnlyOnIslandEventPredicate : EventPredicateProvider() {
    override fun isCached(): Boolean = true

    override fun getPredicate(data: EventData): EventPredicate? {
        when {
            data.options.onlyOnIsland != IslandType.ANY -> {
                val island = data.options.onlyOnIsland
                return { _, _ -> island.isCurrent() }
            }

            data.options.onlyOnIslands.isNotEmpty() -> {
                val set = data.options.onlyOnIslands.toList().toEnumSet()
                return { _, _ -> inAnyIsland(set) }
            }
        }
        return null
    }

}
