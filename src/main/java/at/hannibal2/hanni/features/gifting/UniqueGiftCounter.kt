package at.hannibal2.hanni.features.gifting

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.ProfileStorageData
import at.hannibal2.hanni.data.WinterApi
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.InventoryFullyOpenedEvent
import at.hannibal2.hanni.events.IslandChangeEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.NumberUtil.formatInt
import at.hannibal2.hanni.utils.RegexUtils.firstMatcher
import at.hannibal2.hanni.utils.RenderUtils.renderString
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object UniqueGiftCounter {

    private val config get() = HanniMod.feature.event.gifting.uniqueGiftCounter
    private val storage get() = ProfileStorageData.playerSpecific?.winter

    private val giftedAmountPattern by RepoPattern.pattern(
        "event.winter.uniqugifts.counter.amount",
        "§7Unique Players Gifted: §a(?<amount>.*)",
    )

    private var display = ""

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Generow") return
        val item = event.inventoryItems[40] ?: return

        val storage = storage ?: return

        giftedAmountPattern.firstMatcher(item.getLore()) {
            val amount = group("amount").formatInt()
            storage.amountGifted = amount
            update()
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        update()
    }

    fun addUniqueGift() {
        val storage = storage ?: return
        storage.amountGifted++
        update()
    }

    private fun update() {
        val storage = storage ?: return

        val amountGifted = storage.amountGifted
        val max = 600
        val hasMax = amountGifted >= max
        val color = if (hasMax) "§a" else "§e"
        display = "§7Unique Players Gifted: $color$amountGifted/$max"
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        config.position.renderString(
            display,
            posLabel = "Unique Gift Counter",
        )
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled && WinterApi.isDecember() &&
        InventoryUtils.itemInHandId.endsWith("_GIFT")
}
