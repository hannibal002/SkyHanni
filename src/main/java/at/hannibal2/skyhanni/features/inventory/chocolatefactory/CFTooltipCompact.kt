package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi.partyModeReplace
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.network.chat.Component
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CFTooltipCompact {
    private val config get() = CFApi.config

    private var lastClick = SimpleTimeMark.farPast()
    private var lastHover = SimpleTimeMark.farPast()
    private var tooltipToHover = listOf<Component>()

    @HandleEvent
    fun onToolTip(event: ToolTipTextEvent) {
        if (!CFApi.inChocolateFactory) return

        if (config.tooltipMove) {
            if ((event.slot?.index ?: 100) <= 44) {
                lastHover = SimpleTimeMark.now()
                tooltipToHover = event.toolTip.toList().map { partyModeReplace(it) }
                event.cancel()
            } else {
                lastHover = SimpleTimeMark.farPast()
            }
            return
        }

        onCompactClick(event)
    }

    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!CFApi.inChocolateFactory) return
        if (config.tooltipMove) {
            if (lastHover.passedSince() < 1.seconds) {
                config.tooltipMovePosition.renderRenderables(tooltipToHover.map { Renderable.text(it) }, posLabel = "Tooltip Move")
            }
        }
    }

    private fun onCompactClick(event: ToolTipTextEvent) {
        if (!config.compactOnClick) return

        val itemStack = event.itemStack
        val lore = itemStack.getLoreComponent()
        if (!lore.any { it.string == "Click to uncover the meaning of life!" }) return
        if (lastClick.passedSince() >= 1.seconds && !config.compactOnClickAlways) return
        val list = mutableListOf<Component>()
        list.add(itemStack.hoverName)
        lore.getOrNull(5)?.let {
            list.add(it)
        }
        event.toolTip.clear()
        event.toolTip.addAll(list)
        return
    }

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {

        if (CFApi.inChocolateFactory) {
            if (event.slotId == 13) {
                lastClick = SimpleTimeMark.now()
            }
        }
    }
}
