package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class ChocolateFactoryShortcut {

    private val config get() = ChocolateFactoryAPI.config
    private var showItem = false
    private var lastClick = SimpleTimeMark.farPast()

    private val item by lazy {
        ItemUtils.createSkull(
            displayName = "§6Open Chocolate Factory",
            uuid = "d7ac85e6-bd40-359e-a2c5-86082959309e",
            value = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvO" +
                "WE4MTUzOThlN2RhODliMWJjMDhmNjQ2Y2FmYzhlN2I4MTNkYTBiZTBlZWMwY2NlNmQzZWZmNTIwNzgwMTAyNiJ9fX0=",
            "§8(From SkyHanni)",
            "",
            "§7Click here to run",
            "§e/chocolatefactory"
        )
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.inAnyIsland(
                IslandType.THE_RIFT,
                IslandType.KUUDRA_ARENA,
                IslandType.CATACOMBS,
                IslandType.MINESHAFT,
            )
        ) return
        showItem = config.hoppityMenuShortcut && event.inventoryName == "SkyBlock Menu"
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        showItem = false
    }

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (event.inventory is ContainerLocalMenu && showItem && event.slotNumber == 15) {
            event.replaceWith(item)
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!showItem || event.slotId != 15) return
        event.cancel()
        if (lastClick.passedSince() > 2.seconds) {
            HypixelCommands.chocolateFactory()
            lastClick = SimpleTimeMark.now()
        }
    }
}
