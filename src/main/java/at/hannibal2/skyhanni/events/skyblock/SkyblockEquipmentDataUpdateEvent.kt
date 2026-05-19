package at.hannibal2.skyhanni.events.skyblock

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.inventory.EquipmentSlot
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.item.ItemStack

/**
 * This event fires when the Equipment Inventory is fully opened or when the equipment is changed via Chat message.
 * Fires for each slot independently.
 *
 * @param slot EquipmentSlot Enum Entry
 * @param newItemStack Item Stack (or Null when previously clicked is unknown) of the Equipment.
 *
 */
@PrimaryFunction("onSkyblockEquipmentDataUpdate")
class SkyblockEquipmentDataUpdateEvent(val slot: EquipmentSlot, val newItemStack: ItemStack?) : SkyHanniEvent() {

    val isNecklace get() = slot == EquipmentSlot.NECKLACE
    val isCloak get() = slot == EquipmentSlot.CLOAK
    val isBelt get() = slot == EquipmentSlot.BELT
    val isGloves get() = slot == EquipmentSlot.GLOVES

}
