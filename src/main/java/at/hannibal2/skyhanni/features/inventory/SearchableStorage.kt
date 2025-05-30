package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SearchableStorage {

    private var lastCloseTime = SimpleTimeMark.farPast()

    /**
     * REGEX-TEST: Backpack 5
     * REGEX-TEST: Ender Chest 3
     * REGEX-TEST: Rift Storage 2
     */
    val storagePattern by RepoPattern.pattern("storage.storage", "(?<type>.*) (?<page>\\d+)")

    var highlightSlots = listOf<Int>()
    var waypoints = listOf<LorenzVec>()
    var inventoryName = ""

    @HandleEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        InventoryUtils.getItemsInOpenChest().forEach { slot ->
            val slotNumber = when (inventoryName) {
                "Island Chest" -> slot.slotNumber
                else -> slot.slotNumber - 9
            }

            if (slotNumber in highlightSlots) {
                slot.highlight(LorenzColor.GREEN)
            }
        }
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (lastCloseTime.passedSince() <= 30.seconds) return
        highlightSlots = listOf()
        waypoints = listOf()
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (Minecraft.getMinecraft().currentScreen is SearchableStorageGui) lastCloseTime = SimpleTimeMark.now()
        if (inventoryName.isNotBlank() && event.inventoryTitle.contains(inventoryName)) {
            highlightSlots = listOf()
            waypoints = listOf()
            inventoryName = ""
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        for (waypoint in waypoints) {
            event.drawWaypointFilled(waypoint, Color.GREEN, true)
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shsearchstorage") {
            description = "Search your storage for items with their names or lore."
            category = CommandCategory.USERS_ACTIVE
            callback { SkyHanniMod.screenToOpen = SearchableStorageGui() }
        }
    }
}
