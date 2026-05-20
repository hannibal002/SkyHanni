package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

// TODO replace all function calls outside the clas with the equivalents inside the class, then remove the function.
/**
 * This is a compatibility layer that helps with multiple minecraft versions and mixins.
 * This class should be used in utils/data/api classes and not in feature classes.
 */
object EntityCompat {

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

    fun ArmorStand.getEquipmentSlots(): Map<EquipmentSlot, ItemStack?> =
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

}

@Deprecated("use EntityCompat directly")
fun ArmorStand.getStandHelmet(): ItemStack? =
    this.getItemBySlot(EquipmentSlot.HEAD)

@Deprecated("use EntityCompat directly")
fun Mob.getEntityHelmet(): ItemStack? =
    this.getItemBySlot(EquipmentSlot.HEAD)

@Deprecated("use EntityCompat directly")
fun LivingEntity.getAllEquipment() =
    this.equipment.items.values.toTypedArray()

@Deprecated("use EntityCompat directly")
fun ArmorStand.getHandItem(): ItemStack? =
    this.getItemBySlot(EquipmentSlot.MAINHAND)

@Deprecated("use EntityCompat directly")
fun ArmorStand.getInventoryItems(): Array<ItemStack> =
    arrayOf(
        getItemBySlot(EquipmentSlot.MAINHAND),
        getItemBySlot(EquipmentSlot.FEET),
        getItemBySlot(EquipmentSlot.LEGS),
        getItemBySlot(EquipmentSlot.CHEST),
        getItemBySlot(EquipmentSlot.HEAD),
        getItemBySlot(EquipmentSlot.OFFHAND),
    )

@Deprecated("use EntityCompat directly")
fun ArmorStand.getEquipmentSlots(): Map<EquipmentSlot, ItemStack?> =
    EquipmentSlot.entries.associateWith { getItemBySlot(it).orNull() }

@Deprecated("use EntityCompat directly")
fun Entity.getEntityLevel(): Level =
    this.level()

@Deprecated("use EntityCompat directly")
val Entity.deceased: Boolean
    get() = this.isRemoved

@Deprecated("use EntityCompat directly")
fun LivingEntity.findHealthReal(): Float {
    val entityHealth = health
    if (entityHealth == 1024f) {
        return baseMaxHealth.toFloat()
    }
    return entityHealth
}
