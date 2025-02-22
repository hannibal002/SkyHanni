package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.utils.LorenzUtils.isAnyOf

/** The events get send on change of the widget and on island switch */
open class WidgetUpdateEvent(
    val widget: TabWidget,
    val lines: List<String>,
) : SkyHanniEvent() {

    override fun post(): Boolean = post(widget)
    override fun post(onError: (Throwable) -> Unit): Boolean = post(widget, onError)

    @Deprecated("Consider using the @OnlyWidget annotation instead", ReplaceWith(""))
    fun isWidget(widgetType: TabWidget) = widget == widgetType

    @Deprecated("Consider using the @OnlyWidget annotation instead", ReplaceWith(""))
    fun isWidget(vararg widgetType: TabWidget) = widget.isAnyOf(*widgetType)

    fun isClear() = lines.isEmpty()
}
