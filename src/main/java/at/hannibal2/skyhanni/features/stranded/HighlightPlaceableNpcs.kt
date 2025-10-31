package at.hannibal2.hanni.features.stranded

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.events.InventoryCloseEvent
import at.hannibal2.hanni.events.InventoryFullyOpenedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.RenderUtils.highlight
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object HighlightPlaceableNpcs {

    private val config get() = HanniMod.feature.misc.stranded

    private val patternGroup = RepoPattern.group("stranded.highlightplacement")

    // TODO Please add regex tests
    private val locationPattern by patternGroup.pattern(
        "location",
        "§7Location: §f\\[§e\\d+§f, §e\\d+§f, §e\\d+§f]",
    )
    private val clickToSetPattern by patternGroup.pattern(
        "clicktoset",
        "§7§eClick to set the location of this NPC!",
    )
    private val clickToSpawnPattern by patternGroup.pattern(
        "clicktospawn",
        "§elocation!",
    )

    private var inInventory = false
    private var highlightedItems = emptyList<Int>()

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        inInventory = false
        if (!isEnabled()) return

        if (event.inventoryName != "Island NPCs") return

        val highlightedItems = mutableListOf<Int>()
        for ((slot, stack) in event.inventoryItems) {
            if (isPlaceableNpc(stack.getLore())) {
                highlightedItems.add(slot)
            }
        }
        inInventory = true
        this.highlightedItems = highlightedItems
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
        highlightedItems = emptyList()
    }

    @HandleEvent(priority = HandleEvent.LOW)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled() || !inInventory) return
        for (slot in InventoryUtils.getItemsInOpenChest()) {
            if (slot.slotIndex in highlightedItems) {
                slot.highlight(LorenzColor.GREEN)
            }
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(31, "stranded", "misc.stranded")
    }

    private fun isPlaceableNpc(lore: List<String>): Boolean {
        // Checking if NPC & placeable
        if (lore.isEmpty() || !(clickToSetPattern.matches(lore.last()) || clickToSpawnPattern.matches(lore.last()))) {
            return false
        }

        // Checking if is already placed
        return lore.none { locationPattern.matches(it) }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && SkyBlockUtils.isStrandedProfile && config.highlightPlaceableNpcs
}
