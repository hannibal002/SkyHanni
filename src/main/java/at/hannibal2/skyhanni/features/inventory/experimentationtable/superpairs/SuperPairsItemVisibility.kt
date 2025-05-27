package at.hannibal2.skyhanni.features.inventory.experimentationtable.superpairs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.ExperimentationTableApi
import at.hannibal2.skyhanni.api.TaskType
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import net.minecraft.item.ItemStack

// Todo: Merge this with SuperpairDataDisplay
//  Store slots over there
//  Have the rendered text of superpairdatadisplay highlight the slots the items are in
@SkyHanniModule
object SuperPairsItemVisibility {

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable.superpairs.clickedItemsVisible
    private val superpairsSlotMap: MutableMap<Int, ItemStack> = mutableMapOf()
    private val superpairsSlotsToRead: MutableSet<Int> = mutableSetOf()

    /**
     * REGEX-TEST: §8?
     * REGEX-TEST: §eClick any button!
     * REGEX-TEST: §bClick a second button!
     * REGEX-TEST: §dNext button is instantly rewarded!
     */
    private val unknownSuperpairsClickPattern by ExperimentationTableApi.patternGroup.pattern(
        "superpairs.unknown-click",
        "(?:§.)+(?:\\?|(?:Click a(?: seco)?n[dy]|Next) button(?: is instantly rewarded)?!?)"
    )

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onReplaceItem(event: ReplaceItemEvent) {
        if (!config.enabled) return
        if (!ExperimentationTableApi.inTable || ExperimentationTableApi.currentExperimentType != TaskType.SUPERPAIRS) return
        if (superpairsSlotMap.isEmpty() || event.slot !in superpairsSlotMap.keys) return
        if (!unknownSuperpairsClickPattern.matches(event.originalItem.displayName)) return
        val replacementItem = superpairsSlotMap[event.slot] ?: return
        event.replace(replacementItem)
    }

    @HandleEvent
    fun onInventoryClose() {
        superpairsSlotMap.clear()
        superpairsSlotsToRead.clear()
    }

    @HandleEvent
    fun GuiContainerEvent.SlotClickEvent.tryReadUncoveredItem() {
        val slotNumber = slot?.slotNumber?.takeIf {
            it !in superpairsSlotMap.keys
        } ?: return
        val clickedItem = item ?: return
        if (unknownSuperpairsClickPattern.matches(clickedItem.displayName)) superpairsSlotsToRead.add(slotNumber)
        else superpairsSlotMap[slotNumber] = clickedItem
    }

    @HandleEvent
    fun InventoryOpenEvent.tryReadSuperpairsSlots() {
        if (!ExperimentationTableApi.inTable || ExperimentationTableApi.currentExperimentType != TaskType.SUPERPAIRS) return
        if (superpairsSlotsToRead.isEmpty()) return

        inventoryItems.filter {
            it.key in superpairsSlotsToRead && !unknownSuperpairsClickPattern.matches(it.value.displayName)
        }.forEach {
            superpairsSlotMap[it.key] = it.value
            superpairsSlotsToRead.remove(it.key)
        }
    }

}
