package at.hannibal2.skyhanni.api.event.predicates

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import java.lang.reflect.Method

typealias EventPredicate = (event: SkyHanniEvent, context: Any?) -> Boolean

class EventData(
    val method: Method,
    val options: HandleEvent,
    val generic: Class<*>?,
)

abstract class EventPredicateProvider {
    /** Indicates whether the result of this provider will be cached every tick. */
    open fun isCached(): Boolean = false
    abstract fun getPredicate(data: EventData): EventPredicate?

    companion object {
        private val providers = listOf(
            CancellableEventPredicate,
            GenericEventPredicate,
            OnlyOnIslandEventPredicate,
            OnlyOnSkyblockEventPredicate,
            OnlyWidgetEventPredicateProvider,
        )

        /**
         * Returns a list of pairs of [EventPredicate] and [Boolean], where
         * the boolean indicates if the result of the EventPredicate should be cached every tick.
         */
        fun getEventPredicates(
            method: Method,
            options: HandleEvent,
            generic: Class<*>?,
        ): List<Pair<EventPredicate, Boolean>> {
            val eventData = EventData(method, options, generic)
            return providers.mapNotNull { it.getPredicate(eventData)?.to(it.isCached()) }
        }
    }
}
