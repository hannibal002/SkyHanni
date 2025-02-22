package at.hannibal2.skyhanni.api.event.predicates

object CancellableEventPredicate : EventPredicateProvider() {
    override fun getPredicate(data: EventData): EventPredicate? {
        if (data.options.receiveCancelled) return null
        return { event, _ -> !event.isCancelled }
    }
}
