package at.hannibal2.skyhanni.features.rift.everywhere

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.jsonobjects.EnigmaSoulsJson
import io.github.moulberry.notenoughupdates.events.ReplaceItemEvent
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.player.inventory.ContainerLocalMenu
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object EnigmaSoulWaypoints {
    private val config get() = RiftAPI.config.enigmaSoulWaypoints
    private var inInventory = false
    private var soulLocations = mapOf<String, LorenzVec>()
    private val trackedSouls = mutableListOf<String>()
    private val inventoryUnfound = mutableListOf<String>()
    private var adding = true

    private val item by lazy {
        val neuItem = NEUItems.getItemStack("SKYBLOCK_ENIGMA_SOUL")
        Utils.createItemStack(
            neuItem.item,
            "§5Toggle Missing",
            "§7Click here to toggle",
            "§7the waypoints for each",
            "§7missing souls on this page"
        )
    }

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!isEnabled()) return

        if (inventoryUnfound.isEmpty()) return
        if (event.inventory is ContainerLocalMenu && inInventory && event.slotNumber == 31) {
            event.replaceWith(item)
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        inInventory = false
        if (!event.inventoryName.contains("Enigma Souls")) return
        inInventory = true

        for (stack in event.inventoryItems.values) {
            val split = stack.displayName.split("Enigma: ")
            if (split.size == 2 && stack.getLore().last() == "§8✖ Not completed yet!") {
                inventoryUnfound.add(split.last())
            }
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
        inventoryUnfound.clear()
        adding = true
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onSlotClick(event: SlotClickEvent) {
        if (!inInventory || !isEnabled()) return

        if (event.slotId == 31 && inventoryUnfound.isNotEmpty()) {
            event.usePickblockInstead()
            if (adding) {
                trackedSouls.addAll(inventoryUnfound)
                adding = false
            } else {
                trackedSouls.removeAll(inventoryUnfound)
                adding = true
            }
        }

        if (event.slot.stack == null) return
        val split = event.slot.stack.displayName.split("Enigma: ")
        if (split.size == 2) {
            event.usePickblockInstead()
            if (soulLocations.contains(split.last())) {
                if (!trackedSouls.contains(split.last())) {
                    LorenzUtils.chat("§5Tracking the ${split.last()} Enigma Soul!")
                    trackedSouls.add(split.last())
                } else {
                    trackedSouls.remove(split.last())
                    LorenzUtils.chat("§5No longer tracking the ${split.last()} Enigma Soul!")
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled() || !inInventory) return

        if (event.gui !is GuiChest) return
        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest

        for (slot in chest.inventorySlots) {
            if (slot == null) continue
            val stack = slot.stack ?: continue

            for (soul in trackedSouls) {
                if (stack.displayName.removeColor().contains(soul)) {
                    slot highlight LorenzColor.DARK_PURPLE
                }
            }
        }
        if (!adding) {
            chest.inventorySlots[31] highlight LorenzColor.DARK_PURPLE
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        for (soul in trackedSouls) {
            soulLocations[soul]?.let {
                event.drawWaypointFilled(it, LorenzColor.DARK_PURPLE.toColor(), seeThroughBlocks = true, beacon = true)
                event.drawDynamicText(it.add(0, 1, 0), "§5$soul Soul", 1.5)
            }
        }
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<EnigmaSoulsJson>("EnigmaSouls")
        val areas = data.areas ?: error("'areas' is null in EnigmaSouls!")
        soulLocations = buildMap {
            for ((area, locations) in areas) {
                for (location in locations) {
                    this[location.name] = location.position
                }
            }
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        val message = event.message.removeColor().trim()
        if (message == "You have already found that Enigma Soul!" || message == "SOUL! You unlocked an Enigma Soul!") {
            hideClosestSoul()
        }
    }

    private fun hideClosestSoul() {
        var closestSoul = ""
        var closestDistance = 8.0

        for ((soul, location) in soulLocations) {
            if (location.distanceToPlayer() < closestDistance) {
                closestSoul = soul
                closestDistance = location.distanceToPlayer()
            }
        }
        if (closestSoul in trackedSouls) {
            trackedSouls.remove(closestSoul)
            LorenzUtils.chat("§5Found the $closestSoul Enigma Soul!")
        }
    }

    fun isEnabled() = RiftAPI.inRift() && config.enabled
}
