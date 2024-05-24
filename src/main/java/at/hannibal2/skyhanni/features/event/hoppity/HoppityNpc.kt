package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryAPI
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.SkyblockSeason
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object HoppityNpc {

    private val config get() = HoppityEggsManager.config

    private var lastReminderSent = SimpleTimeMark.farPast()
    private var hoppityYearOpened
        get() = ChocolateFactoryAPI.profileStorage?.hoppityShopYearOpened ?: -1
        set(value) {
            ChocolateFactoryAPI.profileStorage?.hoppityShopYearOpened = value
        }

    private var slotsToHighlight = mutableSetOf<Int>()
    private var inShop = false

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Hoppity") return
        hoppityYearOpened = SkyBlockTime.now().year
        inShop = true
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isReminderEnabled()) return
        if (ReminderUtils.isBusy()) return
        if (hoppityYearOpened == SkyBlockTime.now().year) return
        if (SkyblockSeason.getCurrentSeason() != SkyblockSeason.SPRING) return
        if (lastReminderSent.passedSince() <= 30.seconds) return

        ChatUtils.clickableChat(
            "New rabbits are available at §aHoppity's Shop§e! §c(Click to disable this reminder)",
            onClick = {
                disableReminder()
                ChatUtils.chat("§eHoppity's Shop reminder disabled.")
            },
            oneTimeClick = true
        )

        lastReminderSent = SimpleTimeMark.now()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        clear()
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        clear()
    }

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!inShop) return
        slotsToHighlight.clear()
        for ((slot, item) in event.inventoryItems) {
            if (item.getLore().contains("§eClick to trade!")) {
                slotsToHighlight.add(slot)
            }
        }
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isHighlightEnabled()) return
        if (!inShop) return
        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.slotIndex in slotsToHighlight) {
                slot highlight LorenzColor.GREEN.addOpacity(200)
            }
        }
    }

    private fun isHighlightEnabled() = LorenzUtils.inSkyBlock && config.highlightHoppityShop
    private fun isReminderEnabled() = LorenzUtils.inSkyBlock && config.hoppityShopReminder

    private fun clear() {
        inShop = false
        slotsToHighlight.clear()
    }

    private fun disableReminder() {
        config.hoppityShopReminder = false
    }
}
