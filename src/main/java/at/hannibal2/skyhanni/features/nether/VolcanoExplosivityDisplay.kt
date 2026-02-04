package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.network.chat.Component

@SkyHanniModule
object VolcanoExplosivityDisplay {

    private val config get() = SkyHanniMod.feature.crimsonIsle
    private var display: Component = Component.empty()

    @HandleEvent
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!isEnabled()) return
        if (!event.isWidget(TabWidget.VOLCANO)) return

        if (event.isClear()) {
            display = Component.empty()
            return
        }

        TabWidget.VOLCANO.pattern.matchGroup(event.lines.first(), "status")?.let {
            display = componentBuilder {
                append("§bVolcano Explosivity§7: ")
                append(it)
            }
        }
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.positionVolcano.renderRenderable(Renderable.text(display), posLabel = "Volcano Explosivity")
    }

    private fun isEnabled() = IslandType.CRIMSON_ISLE.isCurrent() && config.volcanoExplosivity
}
