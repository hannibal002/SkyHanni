package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.Storage.ProfileSpecific.DungeonStorage.DungeonRunInfo
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.SackAPI
import at.hannibal2.skyhanni.events.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI.DungeonChest
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CroesusChestTracker {

    private val config get() = SkyHanniMod.feature.dungeon.chest

    private val croesusPattern by RepoPattern.pattern("dungeon.croesus.inventory", "Croesus")
    private val croesusEmptyPattern by RepoPattern.pattern("dungeon.croesus.empty", "§cNo treasures!")
    private val kismetPattern by RepoPattern.pattern("dungeon.kismet.reroll", "§aReroll Chest")
    private val kismetUsedPattern by RepoPattern.pattern("dungeon.kismet.used", "§aYou already rerolled a chest!")

    private val floorPattern by RepoPattern.pattern("dungeon.croesus.chest.floor", "§7Tier: §eFloor (?<floor>[IV]+)")
    private val masterPattern by RepoPattern.pattern("dungeon.croesus.chest.master", ".*Master.*")

    private val kismetSlotId = 50
    private val emptySlotId = 22
    private val frontArrowSlotId = 53
    private val backArrowSlotId = 45

    private val kismetInternalName = "KISMET_FEATHER".asInternalName()

    private var inCroesusInventory = false
    private var croesusEmpty = false
    private var croesusPageNumber = 0
    private var pageSwitchable = false

    private var chestInventory: DungeonChest? = null

    private var currentRunIndex = 0

    private var kismetAmountCache = 0

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.dungeon.croesusUnopenedChestTracker) return

        if (inCroesusInventory && !croesusEmpty) {
            for ((run, slot) in InventoryUtils.getItemsInOpenChest()
                .mapNotNull { slot -> runSlots(slot.slotIndex, slot) }) {

                val state = run.openState

                if (state != OpenedState.KEY_USED) {
                    slot highlight if (state == OpenedState.OPENED) LorenzColor.DARK_AQUA else LorenzColor.DARK_PURPLE
                }
            }
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (SkyHanniMod.feature.dungeon.croesusUnopenedChestTracker || config.kismet) croesusPattern.matchMatcher(event.inventoryName) {
            inCroesusInventory = true
            pageSwitchable = true
            croesusEmpty = croesusEmptyPattern.matches(event.inventoryItems[emptySlotId]?.name)
            if (event.inventoryItems[backArrowSlotId]?.item != Items.arrow) {
                croesusPageNumber = 0
            }

            if (croesusEmpty) {
                croesusChests?.forEach { it.floor = null }
                return
            }

            for ((run, item) in event.inventoryItemsWithNull.mapNotNull { (key, value) -> runSlots(key, value) }) {
                if (item == null) {
                    run.floor = null
                    continue
                }

                val lore = item.getLore()

                if (run.floor == null) run.floor =
                    (if (masterPattern.matches(item.name)) "M" else "F") + (lore.firstNotNullOfOrNull {
                        floorPattern.matchMatcher(it) { group("floor").romanToDecimal() }
                    } ?: "0")
                run.openState = when {
                    "§aNo more Chests to open!" in lore -> OpenedState.KEY_USED
                    lore.any { it.contains("Opened Chest:") } -> OpenedState.OPENED
                    lore.any { it.contains("§8No Chests Opened!") } -> OpenedState.UNOPENED
                    else -> OpenedState.OPENED
                }
            }

            return
        }
        if (config.kismet || config.kismetStackSize) {
            chestInventory = DungeonChest.getByInventoryName(event.inventoryName) ?: return
            kismetAmountCache = getKismetAmount().toInt()
            val kismetItem = event.inventoryItems[kismetSlotId] ?: return
            if (config.kismet && kismetUsedPattern.matches(kismetItem.getLore().lastOrNull())) {
                setKismetUsed()
            }
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inCroesusInventory = false
        chestInventory = null
    }

    @SubscribeEvent
    fun onSlotClicked(event: GuiContainerEvent.SlotClickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.kismet) return
        if (chestInventory != null && event.slotId == kismetSlotId) {
            setKismetUsed()
            return
        }
        if (inCroesusInventory && !croesusEmpty) {
            if (event.slot == null) return
            when (event.slotId) {
                frontArrowSlotId -> if (pageSwitchable && event.slot.stack.isArrow()) {
                    pageSwitchable = false
                    croesusPageNumber++
                }

                backArrowSlotId -> if (pageSwitchable && event.slot.stack.isArrow()) {
                    pageSwitchable = false
                    croesusPageNumber--
                }

                else -> croesusSlotMapToRun(event.slotId)?.let { currentRunIndex = it }
            }
        }
    }

    @SubscribeEvent
    fun onRenderItemTipAmount(event: RenderItemTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.kismetStackSize) return
        if (chestInventory == null) return
        if (!kismetPattern.matches(event.stack.name)) return
        if (kismetUsedPattern.matches(event.stack.getLore().lastOrNull())) return
        event.stackTip = "§a$kismetAmountCache"
    }

    @SubscribeEvent
    fun onRenderItemTipIsKismetable(event: RenderInventoryItemTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.kismet) return
        if (!inCroesusInventory) return
        if (event.slot.slotIndex != event.slot.slotNumber) return
        val run = croesusSlotMapToRun(event.slot.slotIndex) ?: return
        if (!getKismetUsed(run)) return
        event.offsetY = -1
        event.offsetX = -9
        event.stackTip = "§a✔"
    }

    @SubscribeEvent
    fun onDungeonComplete(event: DungeonCompleteEvent) {
        if (event.floor == "E") return
        croesusChests?.add(0, DungeonRunInfo(event.floor))
        currentRunIndex = 0
        if ((croesusChests?.size ?: 0) > maxChests) {
            croesusChests?.dropLast(1)
        }
    }

    private fun Int.getRun() = getRun0(this)

    private fun getRun0(run: Int = currentRunIndex) = croesusChests?.takeIf { run < it.size }?.get(run)

    private fun setKismetUsed() {
        getRun0()?.kismetUsed = true
    }

    private fun getKismetUsed(runIndex: Int) = getRun0(runIndex)?.kismetUsed ?: false

    private fun getKismetAmount() =
        (SackAPI.fetchSackItem(kismetInternalName).takeIf { it.statusIsCorrectOrAlright() }?.amount
            ?: 0) + InventoryUtils.getAmountOfItemInInventory(kismetInternalName)

    private fun croesusSlotMapToRun(slotId: Int) = when (slotId) {
        in 10..16 -> slotId - 10 // 0 - 6
        in 19..25 -> slotId - 12 // 7 - 13
        in 28..34 -> slotId - 14 // 14 - 20
        in 37..43 -> slotId - 16 // 21 - 27
        else -> null
    }?.let { it + croesusPageNumber * 28 }

    private fun ItemStack.isArrow() = this.item == Items.arrow

    private inline fun <reified T> runSlots(slotId: Int, any: T) =
        croesusSlotMapToRun(slotId)?.getRun()?.let { it to any }

    companion object {
        val maxChests = 60

        private val croesusChests get() = ProfileStorageData.profileSpecific?.dungeons?.runs

        fun getLastActiveChest(includeDungeonKey: Boolean = false) =
            (croesusChests?.indexOfLast { it.floor != null && (it.openState == OpenedState.UNOPENED || !(includeDungeonKey && it.openState == OpenedState.OPENED)) }
                ?: -1) + 1

        enum class OpenedState {
            UNOPENED,
            OPENED,
            KEY_USED,
        }
    }
}
