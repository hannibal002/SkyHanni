package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.EnumUtils.isAnyOf
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.network.chat.Component

/** The events get send on change of the widget and on island switch */
@PrimaryFunction("onWidgetUpdate")
class WidgetUpdateEvent(
    val widget: TabWidget,
    val lines: List<Component>,
) : SkyHanniEvent() {

    val cleanLines by lazy { lines.map { it.string.removeColor() } }

    fun isWidget(widgetType: TabWidget) = widget == widgetType
    fun isWidget(vararg widgetType: TabWidget) = widget.isAnyOf(*widgetType)

    fun isClear() = lines.isEmpty()
}
