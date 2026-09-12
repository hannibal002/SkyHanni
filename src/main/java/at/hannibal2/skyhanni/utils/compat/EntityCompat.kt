package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.isNotEmpty
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity

/**
 * This is a compatibility layer that helps with multiple Minecraft versions and mixins.
 * This class should be used in utils/data/api classes and not in feature classes.
 */
object EntityCompat {
    fun LivingEntity.getEquipmentSlots(): Map<EquipmentSlot, SafeItemStack?> =
        EquipmentSlot.entries.associateWith { getItemBySlot(it).orNull() }

    fun LivingEntity.hasEquipment(): Boolean =
        getEquipmentSlots().values.any { it.isNotEmpty() }

    fun LivingEntity.getHelmet(): SafeItemStack? =
        getItemBySlot(EquipmentSlot.HEAD).orNull()

    fun LivingEntity.getHandItem(): SafeItemStack? =
        getItemBySlot(EquipmentSlot.MAINHAND).orNull()

    val LivingEntity.realHealth: Float
        get() = health.takeUnless { it == 1024f } ?: baseMaxHealth.toFloat()
}
