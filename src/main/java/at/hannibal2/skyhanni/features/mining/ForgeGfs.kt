package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackAPI
import at.hannibal2.skyhanni.data.SackAPI
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object ForgeGfs {

    private val patternGroup = RepoPattern.group("mining.forge")

    private val confirmScreenPattern by patternGroup.pattern(
        "recipe.confirm",
        "Confirm Process\$"
    )

    private val config get() = SkyHanniMod.feature.mining.forgeGfs

    private val gfsFakeItem by lazy {
        ItemUtils.createSkull(
            displayName = "§aGet items from sack",
            uuid = "75ea8094-5152-4457-8c23-1ad9b3c176c0",
            value = "ewogICJ0aW1lc3RhbXAiIDogMTU5MTMxMDU4NTYwOSwKICAicHJvZmlsZUlkIiA6ICI0MWQzYWJjMmQ3NDk0MDBjOTA5MGQ1NDM0ZDAzODMxYiIsC"
                + "iAgInByb2ZpbGVOYW1lIiA6ICJNZWdha2xvb24iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogI"
                + "CJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3"
                + "RleHR1cmUvODBhMDc3ZTI0OGQxNDI3NzJlYTgwMDg2NGY4YzU3OGI5ZDM2ODg1YjI5ZGFmODM2YjY0YTcwNjg4MmI2ZWMxMCIKICAgIH0KICB9Cn0=",
            "§8(from SkyHanni)",
            "§7Click here to try to get all of this",
            "§7recipe's ingredients from sack."
        )
    }

    private var showFakeItem = false

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryUpdatedEvent) {
        if (!config) return
        if (!confirmScreenPattern.matches(event.inventoryName)) return

        // Passing 2 filters means that both are true
        // showFakeItem = config && confirmScreenPattern.matches(event.inventoryName)
        showFakeItem = true
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        showFakeItem = false
    }

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (event.inventory is ContainerLocalMenu && showFakeItem && event.slot == 53) {
            event.replace(gfsFakeItem)
        }
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!config) return
        if (!showFakeItem || event.slotId != 53) return

        event.cancel()

        val itemMap: MutableMap<NEUInternalName, Int> = LinkedHashMap()
        // Search for the first 4 columns only
        // Normally would be 3, but the gemstone mixture is the only one that overflows to 4
        val thisContainer = event.container
        for (i in 0..53) {
            if (i % 9 <= 3) {
                val currentItem = thisContainer.getSlot(i).stack
                val amount = currentItem.stackSize
                val currItemInternalName = currentItem.getInternalNameOrNull() ?: continue
                if (SackAPI.sackListInternalNames.contains(currItemInternalName.asString())) {
                    itemMap.addAndFold(currItemInternalName, amount)
                }
            }
        }

        for ((internalName, amount) in itemMap) {
            val getItYet = GetFromSackAPI.getFromSack(internalName, amount)
            if (!getItYet) {
                ChatUtils.chat("§cFailed to get $amount ${internalName.itemNameWithoutColor} from sack.")
            }
        }
    }

    private fun MutableMap<NEUInternalName, Int>.addAndFold(key: NEUInternalName, value: Int) {
        this[key] = this.getOrDefault(key, 0) + value
    }
}
