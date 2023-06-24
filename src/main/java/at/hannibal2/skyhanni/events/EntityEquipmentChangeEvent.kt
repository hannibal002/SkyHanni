package at.hannibal2.skyhanni.events

import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack

data class EntityEquipmentChangeEvent(
    val entity: Entity,
    val equipmentSlot: Int,
    val newItemStack: ItemStack?
) : LorenzEvent() {
    companion object {
        val EQUIPMENT_SLOT_HEAD = 4
        val EQUIPMENT_SLOT_CHEST = 3
        val EQUIPMENT_SLOT_LEGGINGS = 2
        val EQUIPMENT_SLOT_FEET = 1
        val EQUIPMENT_SLOT_HAND = 0
    }
}