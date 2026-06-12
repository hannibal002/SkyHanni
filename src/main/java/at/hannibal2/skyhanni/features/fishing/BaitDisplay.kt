package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.fishing.BaitUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.features.fishing.FishingApi.BaitType

@SkyHanniModule
object BaitDisplay {

    private val config get() = SkyHanniMod.feature.fishing.baitDisplay

    private var display = emptyList<Renderable>()
    private var baitType: BaitType? = null
    private var amount = 0

    private fun updateDisplay() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList {
        val bait = baitType ?: return@buildList
        val itemStack = bait.internalName.getItemStackOrNull() ?: return@buildList

        if (config.showIcon) {
            addItemStack(itemStack, scale = 1.0)
        }
        addString("§b${amount.addSeparators()}x")
        addString(" §6${bait.displayName}")
    }

    @HandleEvent
    fun onBaitUpdate(event: BaitUpdateEvent) {
        baitType = event.baitType
        amount = event.amount
        updateDisplay()
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (display.isEmpty()) return
        config.position.renderRenderable(
            Renderable.horizontal(
                display,
                spacing = 1,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            ),
            posLabel = "Bait Display",
        )
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        display = emptyList()
        baitType = null
        amount = 0
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled
}
