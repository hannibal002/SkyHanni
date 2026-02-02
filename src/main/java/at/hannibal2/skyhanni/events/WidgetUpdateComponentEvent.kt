package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.model.TabWidgetComponent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.EnumUtils.isAnyOf
import net.minecraft.network.chat.Component

/** The events get send on change of the widget and on island switch */
@PrimaryFunction("onWidgetUpdate")
class WidgetUpdateComponentEvent(
    val widget: TabWidgetComponent,
    val lines: List<Component>,
) : SkyHanniEvent() {

    fun isWidget(widgetType: TabWidgetComponent) = widget == widgetType
    fun isWidget(vararg widgetType: TabWidgetComponent) = widget.isAnyOf(*widgetType)

    fun isClear() = lines.isEmpty()
}
