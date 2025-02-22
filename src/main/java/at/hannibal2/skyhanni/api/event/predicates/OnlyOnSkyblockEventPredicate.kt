package at.hannibal2.skyhanni.api.event.predicates

import at.hannibal2.skyhanni.utils.LorenzUtils

object OnlyOnSkyblockEventPredicate : EventPredicateProvider() {
    override fun isCached(): Boolean = true

    override fun getPredicate(data: EventData): EventPredicate? {
        if (!data.options.onlyOnSkyblock) return null
        return { _, _ -> LorenzUtils.inSkyBlock }
    }
}
