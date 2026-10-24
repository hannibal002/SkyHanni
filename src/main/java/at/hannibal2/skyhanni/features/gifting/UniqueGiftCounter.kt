package at.hannibal2.skyhanni.features.gifting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.WinterApi
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object UniqueGiftCounter {

    private val config get() = SkyHanniMod.feature.event.gifting.uniqueGiftCounter
    private val storage get() = ProfileStorageData.playerSpecific?.winter

    /**
     * REGEX-TEST: Unique Players Gifted: 600
     */
    private val giftedAmountPattern by RepoPattern.pattern(
        "event.winter.uniqugifts.counter.amount.colorless",
        "Unique Players Gifted: (?<amount>.*)",
    )

    /**
     * REGEX-TEST: Generow
     */
    private val generowInventoryPattern by RepoPattern.pattern(
        "event.winter.uniqugifts.inventory.name",
        "Generow",
    )

    private var display: Renderable? = null

    @HandleEvent
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!generowInventoryPattern.matches(event.inventoryName)) return
        val item = event.inventoryItems[40] ?: return
        val storage = storage ?: return

        giftedAmountPattern.firstMatcher(item.getCleanLore()) {
            val amount = group("amount").formatInt()
            storage.amountGifted = amount
            update()
        }
    }

    @HandleEvent
    private fun onIslandChange() {
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
        display = Renderable.text("§7Unique Players Gifted: $color$amountGifted/$max")
    }

    @HandleEvent
    private fun onGuiRenderOverlay() {
        if (!isEnabled()) return
        val display = display ?: return

        config.position.renderRenderable(
            display,
            posLabel = "Unique Gift Counter",
        )
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled && WinterApi.isDecember() &&
        InventoryUtils.itemInHandId.endsWith("_GIFT")
}
