package at.hannibal2.skyhanni.features.inventory.searchablestorage

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorageGui.SearchMode
import at.hannibal2.skyhanni.features.inventory.searchablestorage.SearchableStorageGui.SortMode
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SearchableStorage {

    private val config get() = SkyHanniMod.feature.inventory

    /**
     * REGEX-TEST: Backpack 5
     * REGEX-TEST: Ender Chest 3
     * REGEX-TEST: Rift Storage 2
     */
    val storagePattern by RepoPattern.pattern("storage.storage", "(?<type>.*) (?<page>\\d+)")

    private var lastCloseTime = SimpleTimeMark.farPast()
    var searchMode = SearchMode.NAME
    var sortMode = SortMode.NAME_DESC
    var highlightSlots = listOf<Int>()
    var waypoints = listOf<LorenzVec>()
    var inventoryName = ""

    @HandleEvent(GuiContainerEvent.BackgroundDrawnEvent::class)
    fun onBackgroundDrawn() {
        InventoryUtils.getItemsInOpenChest().forEach { slot ->
            val slotNumber = when (inventoryName) {
                "Chest" -> slot.slotNumber
                else -> slot.slotNumber - 9
            }

            if (slotNumber in highlightSlots) {
                slot.highlight(LorenzColor.GREEN)
            }
        }
    }

    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        if (lastCloseTime.passedSince() <= 30.seconds) return
        highlightSlots = listOf()
        waypoints = listOf()
        lastCloseTime = SimpleTimeMark.farFuture()
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
        event.registerBrigadier("shsearchstorage") {
            description = "Opens a gui to search for items across storages"
            category = CommandCategory.USERS_ACTIVE
            simpleCallback { onCommand() }
            argCallback("search", BrigadierArguments.greedyString()) { search -> onCommand(search) }
        }
    }

    fun onCommand(search: String = "") {
        SkyHanniMod.screenToOpen = if (shouldRemind()) SearchableStorageReminderGui(search) else SearchableStorageGui(search)
    }

    fun minecraftButton(text: String) = Renderable.drawInsideRoundedRectWithOutline(
        StringRenderable(text), Color.decode("#8b8b8b"),
        radius = 0,
        topOutlineColor = Color.WHITE.rgb,
        bottomOutlineColor = Color.WHITE.rgb,
        borderOutlineThickness = 2,
        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
    )

    private fun shouldRemind() = config.searchableStorageReminder && !config.savePrivateIslandChests
}
