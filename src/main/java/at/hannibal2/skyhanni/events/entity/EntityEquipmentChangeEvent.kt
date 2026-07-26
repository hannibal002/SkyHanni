package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.utils.SafeItemStack
import net.minecraft.world.entity.LivingEntity

/**
 * Event that is called when an entity's equipment changes.
 *
 * @param T The type of the entity.
 * @property entity The entity whose equipment changed.
 * @property equipmentSlot The slot of the equipment that changed.
 * @property newItemStack The new item stack that was equipped, or null if the slot was cleared.
 */
data class EntityEquipmentChangeEvent<T : LivingEntity>(
    val entity: T,
    val equipmentSlot: Int,
    val newItemStack: SafeItemStack?,
) : GenericSkyHanniEvent<T>(entity.javaClass) {

    val isHead get() = equipmentSlot == EQUIPMENT_SLOT_HEAD
    val isChest get() = equipmentSlot == EQUIPMENT_SLOT_CHEST
    val isLeggings get() = equipmentSlot == EQUIPMENT_SLOT_LEGGINGS
    val isFeet get() = equipmentSlot == EQUIPMENT_SLOT_FEET
    val isHand get() = equipmentSlot == EQUIPMENT_SLOT_HAND

    companion object {

        const val EQUIPMENT_SLOT_HEAD = 4
        const val EQUIPMENT_SLOT_CHEST = 3
        const val EQUIPMENT_SLOT_LEGGINGS = 2
        const val EQUIPMENT_SLOT_FEET = 1
        const val EQUIPMENT_SLOT_HAND = 0
    }
}
