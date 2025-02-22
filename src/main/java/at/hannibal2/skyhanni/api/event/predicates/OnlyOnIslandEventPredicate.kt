package at.hannibal2.skyhanni.api.event.predicates

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.CollectionUtils.toEnumSet
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland

object OnlyOnIslandEventPredicate : EventPredicateProvider() {
    override fun isCached(): Boolean = true

    override fun getPredicate(data: EventData): EventPredicate? {
        when {
            data.options.onlyOnIsland != IslandType.ANY -> {
                val island = data.options.onlyOnIsland
                return { _, _ -> island.isInIsland() }
            }

            data.options.onlyOnIslands.isNotEmpty() -> {
                val set = data.options.onlyOnIslands.toList().toEnumSet()
                return { _, _ -> inAnyIsland(set) }
            }
        }
        return null
    }

}
