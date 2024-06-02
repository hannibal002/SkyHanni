package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.model.TabWidget

open class WidgetUpdateEvent(
    private val widget: TabWidget,
) : LorenzEvent() {
    class NewValues(
        widget: TabWidget,
        val lines: List<String>
    ) : WidgetUpdateEvent(widget)

    class Clear(
        widget: TabWidget,
    ) : WidgetUpdateEvent(widget)

    fun isWidget(widgetType: TabWidget) = widget == widgetType
}
