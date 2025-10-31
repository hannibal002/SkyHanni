package at.hannibal2.hanni.features.inventory.bazaar

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ItemUtils
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.OSUtils
import at.hannibal2.hanni.utils.SimpleTimeMark
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.init.Items
import kotlin.time.Duration.Companion.seconds

@HanniModule
object BazaarOpenPriceWebsite {

    private val config get() = HanniMod.feature.inventory.bazaar
    private var lastClick = SimpleTimeMark.farPast()

    private val item by lazy {
        ItemUtils.createItemStack(
            Items.paper,
            "§bPrice History",
            "§8(From Hanni)",
            "",
            "§7Click here to open",
            "§7the price history",
            "§7on §cskyblock.bz",
        )
    }

    @HandleEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!isEnabled()) return
        BazaarApi.currentlyOpenedProduct ?: return
        if (event.inventory is InventoryPlayer) return

        if (event.slot == 22) {
            event.replace(item)
        }
    }

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return
        val lastItem = BazaarApi.currentlyOpenedProduct ?: return

        if (event.slotId == 22) {
            event.cancel()
            if (lastClick.passedSince() > 0.3.seconds) {
                val name = getSkyBlockBzName(lastItem)
                OSUtils.openBrowser("https://www.skyblock.bz/product/$name")
                lastClick = SimpleTimeMark.now()
            }
        }
    }

    private fun getSkyBlockBzName(internalName: NeuInternalName): String {
        val name = internalName.asString()
        return if (name.contains(";")) {
            "ENCHANTMENT_" + name.replace(";", "_")
        } else name
    }

    fun isEnabled() = config.openPriceWebsite
}
