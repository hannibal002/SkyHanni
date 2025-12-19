package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

fun ArmorStand.getStandHelmet(): ItemStack? =
    this.getItemBySlot(EquipmentSlot.HEAD)

fun Mob.getEntityHelmet(): ItemStack? =
    this.getItemBySlot(EquipmentSlot.HEAD)

fun LivingEntity.getAllEquipment() =
    this.equipment.items.values.toTypedArray()

fun ArmorStand.getHandItem(): ItemStack? =
    this.getItemBySlot(EquipmentSlot.MAINHAND)

fun ArmorStand.getInventoryItems(): Array<ItemStack> =
    arrayOf(
        getItemBySlot(EquipmentSlot.MAINHAND),
        getItemBySlot(EquipmentSlot.FEET),
        getItemBySlot(EquipmentSlot.LEGS),
        getItemBySlot(EquipmentSlot.CHEST),
        getItemBySlot(EquipmentSlot.HEAD),
        getItemBySlot(EquipmentSlot.OFFHAND),
    )

fun Entity.getEntityLevel(): Level =
    this.level()

val Entity.deceased: Boolean
    get() = this.isRemoved

fun LivingEntity.findHealthReal(): Float {
    val entityHealth = health
    if (entityHealth == 1024f && !PlatformUtils.IS_LEGACY) {
        return baseMaxHealth.toFloat()
    }
    return entityHealth
}
