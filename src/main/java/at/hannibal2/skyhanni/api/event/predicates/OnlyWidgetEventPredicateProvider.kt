package at.hannibal2.skyhanni.api.event.predicates

import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.utils.CollectionUtils.toEnumSet
import at.hannibal2.skyhanni.utils.ReflectionUtils.getAnnotation

/**
 * [WidgetUpdateEvent]s annotated with this will only receive updates of the specified widgets.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class OnlyWidget(
    /**
     * What TabWidgets this event should receive updates for.
     */
    vararg val widgets: TabWidget,
)

object OnlyWidgetEventPredicateProvider : EventPredicateProvider() {

    override fun getPredicate(data: EventData): EventPredicate? {
        val annotation = data.method.getAnnotation<OnlyWidget>() ?: return null
        val widgets = annotation.widgets.toList().toEnumSet()
        if (widgets.isEmpty()) return null
        else if (widgets.size == 1) {
            val widget = widgets.first()
            return { _, context -> context is TabWidget && context == widget }
        }
        return { _, context -> context is TabWidget && context in widgets }
    }
}
