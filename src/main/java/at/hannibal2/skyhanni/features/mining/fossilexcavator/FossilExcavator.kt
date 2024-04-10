package at.hannibal2.skyhanni.features.mining.fossilexcavator

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class FossilExcavator {

    private val config get() = SkyHanniMod.feature.mining.fossilExcavator

    private val patternGroup = RepoPattern.group("mining.fossilexcavator")
    private val chargesRemainingPattern by patternGroup.pattern(
        "chargesremaining",
        "Chisel Charges Remaining: (?<charges>\\d+)"
    )
    private val fossilProgressPattern by patternGroup.pattern(
        "fossilprogress",
        "Fossil Excavation Progress: (?<progress>[\\d.]+%)"
    )

    private var inInventory = false
    private var foundPercentage = false
    private var chargesRemaining = 0
    private val possibleFossils = mutableListOf<FossilType>()

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (event.inventoryName != "Fossil Excavator") return
        inInventory = true
        possibleFossils.addAll(FossilType.entries)

        //todo remove when merging
        ChatUtils.chat(
            "Correct inventory opened. If this happens multiple times while doing one excavation, " +
                "please report it to me."
        )
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        clearData()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        clearData()
    }

    private fun clearData() {
        possibleFossils.clear()
        inInventory = false
        foundPercentage = false
        chargesRemaining = 0
    }

    @SubscribeEvent
    fun onInventoryUpdate(event: InventoryUpdatedEvent) {
        if (!isEnabled()) return
        if (!inInventory) return

        //todo remove when merging
        printInventoryInfo(event.inventoryItems)
        ChatUtils.chat("Inventory update detected. Whole inventory data logged.")

        val foundChargesRemaining = false
        for ((_, stack) in event.inventoryItems) {

        }
    }

    fun isEnabled() = IslandType.DWARVEN_MINES.isInIsland() && config.enabled

    //todo remove when merging
    private fun printInventoryInfo(inventory: Map<Int, ItemStack>) {
        val resultList = mutableListOf<String>()

        for ((slotIndex, stack) in inventory) {
            resultList.add("Item in slot: $slotIndex")
            resultList.add("  Name: ${stack.displayName}")
            resultList.add("  Lore: ${stack.getLore()}")
            resultList.add("")
        }

        println("")
        println(resultList.joinToString("\n"))
    }
}
