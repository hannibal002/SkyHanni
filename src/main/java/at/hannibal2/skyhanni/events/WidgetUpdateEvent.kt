package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.data.model.TabWidget
import at.hannibal2.hanni.hannimodule.PrimaryFunction
import at.hannibal2.hanni.utils.EnumUtils.isAnyOf

/** The events get send on change of the widget and on island switch */
@PrimaryFunction("onWidgetUpdate")
class WidgetUpdateEvent(
    val widget: TabWidget,
    val lines: List<String>,
) : HanniEvent() {

    fun isWidget(widgetType: TabWidget) = widget == widgetType
    fun isWidget(vararg widgetType: TabWidget) = widget.isAnyOf(*widgetType)

    fun isClear() = lines.isEmpty()
}
