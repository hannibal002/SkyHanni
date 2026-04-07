package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.Level

fun ArmorStand.getStandHelmet(): SafeItemStack? =
    this.getItemBySlot(EquipmentSlot.HEAD)

fun Mob.getEntityHelmet(): SafeItemStack? =
    this.getItemBySlot(EquipmentSlot.HEAD)

fun LivingEntity.getAllEquipment() =
    this.equipment.items.values.toTypedArray()

fun ArmorStand.getHandItem(): SafeItemStack? =
    this.getItemBySlot(EquipmentSlot.MAINHAND)

fun ArmorStand.getInventoryItems(): Array<SafeItemStack> =
    arrayOf(
        getItemBySlot(EquipmentSlot.MAINHAND),
        getItemBySlot(EquipmentSlot.FEET),
        getItemBySlot(EquipmentSlot.LEGS),
        getItemBySlot(EquipmentSlot.CHEST),
        getItemBySlot(EquipmentSlot.HEAD),
        getItemBySlot(EquipmentSlot.OFFHAND),
    )

fun ArmorStand.getEquipmentSlots(): Map<EquipmentSlot, SafeItemStack?> =
    EquipmentSlot.entries.associateWith { getItemBySlot(it).orNull() }

fun Entity.getEntityLevel(): Level =
    this.level()

val Entity.deceased: Boolean
    get() = this.isRemoved

fun LivingEntity.findHealthReal(): Float {
    val entityHealth = health
    if (entityHealth == 1024f) {
        return baseMaxHealth.toFloat()
    }
    return entityHealth
}
