package at.hannibal2.skyhanni.api.event.predicates

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent

object GenericEventPredicate : EventPredicateProvider() {

    override fun getPredicate(data: EventData): EventPredicate? {
        val generic = data.generic ?: return null
        return { event, _ -> event is GenericSkyHanniEvent<*> && generic.isAssignableFrom(event.type) }
    }
}
