package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandGraphs.pathFind
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.LoreCostUtils.hasTradeLine
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object HoppityNpc {

    private val config get() = HoppityEggsManager.config

    private var lastReminderSent = SimpleTimeMark.farPast()
    private var hoppityYearOpened
        get() = CFApi.profileStorage?.hoppityShopYearOpened ?: -1
        set(value) {
            CFApi.profileStorage?.hoppityShopYearOpened = value
        }

    private val slotsToHighlight = mutableSetOf<Int>()
    private var inShop = false

    @HandleEvent
    private fun onInventoryFullyOpened() {
        if (!HoppityApi.hoppityDetector.isInside()) return
        // TODO maybe we could add an annoying chat message that tells you how many years you have skipped
        //  or the last year you have opened the shop before.
        //  that way we verbally punish non active users in a funny and non harmful way
        hoppityYearOpened = SkyBlockTime.now().year
        inShop = true
    }

    @HandleEvent
    private fun onSecondPassed() {
        if (!isReminderEnabled()) return
        if (ReminderUtils.isBusy()) return
        if (SkyBlockUtils.isStrandedProfile) return

        if (hoppityYearOpened == SkyBlockTime.now().year) return
        if (!HoppityApi.isHoppityEvent()) return
        if (lastReminderSent.passedSince() <= 2.minutes) return

        ChatUtils.clickToActionOrDisable(
            "New rabbits are available at §aHoppity's Shop§e!",
            config::hoppityShopReminder,
            actionName = "warp to hub",
            action = {
                HypixelCommands.warp("hub")
                EntityMovementData.onNextTeleport(IslandType.HUB) {
                    IslandGraphs.node("Hoppity", GraphNodeTag.NPC).pathFind(
                        label = "§aHoppity's Shop",
                        condition = { config.hoppityShopReminder },
                    )
                }
            },
        )

        lastReminderSent = SimpleTimeMark.now()
    }

    @HandleEvent
    private fun onInventoryClose() {
        clear()
    }

    @HandleEvent
    private fun onWorldChange() {
        clear()
    }

    @HandleEvent
    private fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!inShop) return
        slotsToHighlight.clear()
        for ((slot, item) in event.inventoryItems) {
            if (item.getCleanLore().hasTradeLine()) {
                slotsToHighlight.add(slot)
            }
        }
    }

    @HandleEvent
    private fun onBackgroundDrawn() {
        if (!isHighlightEnabled()) return
        if (!inShop) return
        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.containerSlot in slotsToHighlight) {
                slot.highlight(LorenzColor.GREEN.addOpacity(200))
            }
        }
    }

    private fun isHighlightEnabled() = SkyBlockUtils.inSkyBlock && config.highlightHoppityShop
    private fun isReminderEnabled() = SkyBlockUtils.inSkyBlock && config.hoppityShopReminder

    private fun clear() {
        inShop = false
        slotsToHighlight.clear()
    }
}
