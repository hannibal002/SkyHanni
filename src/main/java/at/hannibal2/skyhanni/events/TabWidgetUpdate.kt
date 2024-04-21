package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.model.TabWidget

open class TabWidgetUpdate(
    val widget: TabWidget,
) : LorenzEvent() {
    class NewValues(
        widget: TabWidget,
        val lines: List<String>
    ) : TabWidgetUpdate(widget)

    class Clear(
        widget: TabWidget,
    ) : TabWidgetUpdate(widget)

    fun isEventFor(widgetType: TabWidget) = widget == widgetType
}
