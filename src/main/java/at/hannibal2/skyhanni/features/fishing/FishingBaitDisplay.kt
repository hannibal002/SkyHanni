package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.fishing.BaitUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal

@SkyHanniModule
object FishingBaitDisplay {
    private val config get() = SkyHanniMod.feature.fishing.fishingBaitDisplay

    private var display = emptyList<Renderable>()
    private var bait: BaitDisplayEntry? = null

    private data class BaitDisplayEntry(
        val itemStack: SafeItemStack?,
        val displayName: String,
        val amount: Int?,
    )

    private val noBaitEntry = BaitDisplayEntry(
        itemStack = null,
        displayName = "§cNo Bait",
        amount = null,
    )

    @HandleEvent
    fun onBaitUpdate(event: BaitUpdateEvent) {
        bait = getBaitDisplayEntry(event.itemStack, event.baitType, event.amount)
        updateDisplay()
    }

    @HandleEvent
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!FishingApi.holdingRod) return
        if (bait == null) return
        if (display.isEmpty()) return

        config.position.renderRenderable(
            Renderable.horizontal(
                display,
                spacing = 1,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            ),
            posLabel = "Fishing Bait Display",
        )
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.showIcon) {
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList {
        val bait = bait ?: return@buildList

        if (config.showIcon.get()) {
            bait.itemStack?.let {
                addItemStack(it, scale = 1.0)
            }
        }
        bait.amount?.let {
            addString("§b${it.addSeparators()}x")
        }
        val namePrefix = if (isEmpty()) "" else " "
        addString("$namePrefix${bait.displayName}")
    }

    private fun getBaitDisplayEntry(
        itemStack: SafeItemStack,
        baitType: FishingApi.BaitType?,
        amount: Int,
    ): BaitDisplayEntry {
        if (baitType == null) {
            return noBaitEntry
        }

        val icon = itemStack.copy().also { it.count = 1 }

        return BaitDisplayEntry(
            itemStack = icon,
            displayName = baitType.displayName,
            amount = amount,
        )
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled
}
