package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class KloonHacking {
    private var wearingHelmet = false
    private var inTerminalInventory = false
    private var inColourInventory = false
    private val correctButtons = mutableListOf<String>()
    private var nearestColor: String? = null

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!RiftAPI.inRift()) return
        if (event.isMod(20)) {
            checkHelmet()
        }
    }

    private fun checkHelmet() {
        wearingHelmet = InventoryUtils.getArmor()[3]?.getInternalName() == "RETRO_ENCABULATING_VISOR"
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        inTerminalInventory = false
        inColourInventory = false
        if (!RiftAPI.inRift()) return
        if (!SkyHanniMod.feature.rift.hacking.solver) return
        if (event.inventoryName == "Hacking" || event.inventoryName == "Hacking (As seen on CSI)") {
            inTerminalInventory = true
            correctButtons.clear()
            for ((slot, stack) in event.inventoryItems) {
                if (slot in 2..6) {
                    correctButtons.add(stack.displayName.removeColor())
                }
            }
        }
        if (event.inventoryName == "Hacked Terminal Color Picker") {
            inColourInventory = true
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inTerminalInventory = false
        inColourInventory = false
        nearestColor = null
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!RiftAPI.inRift()) return
        if (inTerminalInventory) {
            if (!SkyHanniMod.feature.rift.hacking.solver) return
            var i = 0
            for (slot in InventoryUtils.getItemsInOpenChest()) {
                if (slot.slotIndex == 11 + 10 * i) {
                    val correctButton = slot.stack!!.displayName.removeColor() == correctButtons[i]
                    slot highlight if (correctButton) LorenzColor.GREEN else LorenzColor.RED
                    continue
                }
                if (slot.slotIndex > i * 9 + 8 && slot.slotIndex < i * 9 + 18) {
                    if (slot.stack!!.displayName.removeColor() == correctButtons[i]) {
                        slot highlight LorenzColor.YELLOW
                    }
                }
                if (slot.slotIndex == i * 9 + 17) {
                    i += 1
                }
            }
        }
        if (inColourInventory) {
            if (!SkyHanniMod.feature.rift.hacking.colour) return
            val targetColour = nearestColor ?: getNearestColour()
            for (slot in InventoryUtils.getItemsInOpenChest()) {
                if (slot.stack.getLore().any { it.contains(targetColour) }) {
                    slot highlight LorenzColor.GREEN
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onSlotClick(event: SlotClickEvent) {
        if (!inTerminalInventory || !RiftAPI.inRift()) return
        if (!inTerminalInventory) return
        event.usePickblockInstead()
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!RiftAPI.inRift()) return
        if (!SkyHanniMod.feature.rift.hacking.waypoints) return
        if (!wearingHelmet) return
        for (terminal in KloonTerminal.values()) {
            event.drawWaypointFilled(terminal.location, LorenzColor.DARK_RED.toColor(), true, true)
        }
    }

    private fun getNearestColour(): String {
        var closestTerminal = ""
        var closestDistance = 8.0

        for (terminal in KloonTerminal.values()) {
            val distance = terminal.location.distanceToPlayer()
            if (distance < closestDistance) {
                closestTerminal = terminal.name
                closestDistance = distance
            }
        }
        nearestColor = closestTerminal
        return closestTerminal
    }
}