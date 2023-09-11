package at.hannibal2.skyhanni.features.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.entity.player.InventoryPlayer
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class BazaarOpenPriceWebsite {
    private val config get() = SkyHanniMod.feature.bazaar
    private var lastClick = SimpleTimeMark.farPast()

    private val item by lazy {
        val neuItem = NEUItems.getItemStack("PAPER", true)
        Utils.createItemStack(
            neuItem.item,
            "§bPrice History",
            "§7Click here to open",
            "§7the price history",
            "§7on §cskyblock.bz"
        )
    }

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!isEnabled()) return
        BazaarApi.currentlyOpenedProduct ?: return
        if (event.inventory is InventoryPlayer) return

        if (event.slotNumber == 22) {
            event.replaceWith(item)
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onStackClick(event: SlotClickEvent) {
        if (!isEnabled()) return
        val lastItem = BazaarApi.currentlyOpenedProduct ?: return

        if (event.slotId == 22) {
            event.isCanceled = true
            if (lastClick.passedSince() > 0.3.seconds) {
                val name = getSkyBlockBzName(lastItem)
                OSUtils.openBrowser("https://www.skyblock.bz/product/$name")
                lastClick = SimpleTimeMark.now()
            }
        }
    }

    private fun getSkyBlockBzName(internalName: NEUInternalName): String {
        val name = internalName.asString()
        return if (name.contains(";")) {
            "ENCHANTMENT_" + name.replace(";", "_")
        } else name
    }

    fun isEnabled() = config.openPriceWebsite
}