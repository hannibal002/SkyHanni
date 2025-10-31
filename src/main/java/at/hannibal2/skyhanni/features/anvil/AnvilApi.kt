package at.hannibal2.hanni.features.anvil

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.OwnInventoryData
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.events.InventoryCloseEvent
import at.hannibal2.hanni.events.inventory.AnvilUpdateEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.InventoryDetector
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.compat.InventoryCompat.orNull
import net.minecraft.item.ItemStack
import kotlin.time.Duration.Companion.seconds

@HanniModule
object AnvilApi {
    val inventory = InventoryDetector { name -> name == "Anvil" }

    var left: ItemStack? = null
    var right: ItemStack? = null

    // InventoryUpdatedEvent only reacts on packets from the server, but the anvil interactions are client side only
    @HandleEvent(priority = HandleEvent.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!inventory.isInside()) return

        DelayedRun.runNextTick {
            if (!inventory.isInside()) return@runNextTick
            for (slot in InventoryUtils.getItemsInOpenChestWithNull()) {
                if (slot.slotNumber == 29) {
                    val left = slot.stack.orNull()
                    if (this.left?.getInternalName() != left?.getInternalName()) {
                        this.left = left
                        postEvent()
                    }
                }
                if (slot.slotNumber == 33) {
                    val right = slot.stack.orNull()
                    if (this.right?.getInternalName() != right?.getInternalName()) {
                        this.right = right
                        postEvent()
                    }
                }
            }
        }
    }

    private fun postEvent() {
        AnvilUpdateEvent(left, right).post()
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        // hypixel delays the pickup

        var hadItems = false
        left?.let {
            OwnInventoryData.ignoreItem(3.seconds, it.getInternalName())
            hadItems = true
        }
        right?.let {
            OwnInventoryData.ignoreItem(3.seconds, it.getInternalName())
            hadItems = true
        }

        left = null
        right = null
        if (hadItems) {
            postEvent()
        }
    }
}
