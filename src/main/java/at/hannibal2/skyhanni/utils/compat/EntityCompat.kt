package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.item.ItemStack
import net.minecraft.world.World
//#if MC < 1.16
import net.minecraft.entity.monster.EntitySkeleton
//#else
//$$ import net.minecraft.world.entity.monster.WitherSkeleton
//$$ import net.minecraft.world.entity.EquipmentSlot
//$$ import net.minecraft.world.entity.EntityType
//#endif

fun EntityArmorStand.getStandHelmet(): ItemStack? =
//#if MC < 1.16
    this.getEquipmentInSlot(4)
//#else
//$$ this.getItemBySlot(EquipmentSlot.HEAD)
//#endif

fun EntityLiving.getEntityHelmet(): ItemStack? =
//#if MC < 1.16
    this.getEquipmentInSlot(4)
//#else
//$$ this.getItemBySlot(EquipmentSlot.HEAD)
//#endif

fun EntityLivingBase.getAllEquipment() =
//#if MC < 1.16
    this.inventory
//#elseif MC < 1.21
//$$ this.armorSlots
//#else
//$$ this.equipment.map.values.toTypedArray()
//#endif

fun Entity.getFirstPassenger(): Entity? =
//#if MC < 1.16
    this.riddenByEntity
//#else
//$$ this.passengers.firstOrNull()
//#endif

fun EntityArmorStand.getHandItem(): ItemStack? =
//#if MC < 1.16
    this.getEquipmentInSlot(0)
//#else
//$$ this.getItemBySlot(EquipmentSlot.MAINHAND)
//#endif

fun EntityArmorStand.getInventoryItems(): Array<ItemStack> =
    //#if MC < 1.16
    inventory
//#else
//$$ arrayOf(
//$$ getEquippedStack(EquipmentSlot.MAINHAND),
//$$ getEquippedStack(EquipmentSlot.FEET),
//$$ getEquippedStack(EquipmentSlot.LEGS),
//$$ getEquippedStack(EquipmentSlot.CHEST),
//$$ getEquippedStack(EquipmentSlot.HEAD),
//$$ getEquippedStack(EquipmentSlot.OFFHAND),
//$$ )
//#endif

fun Entity.getEntityLevel(): World =
//#if MC < 1.16
    this.entityWorld
//#else
//$$ this.level
//#endif

fun createWitherSkeleton(world: World?): EntityLivingBase =
//#if MC < 1.16
    EntitySkeleton(world).also { it.skeletonType = 1 }
//#else
//$$ WitherSkeleton(EntityType.WITHER_SKELETON, world)
//#endif

//#if MC > 1.21
//$$ val Entity.deceased: Boolean
//$$     get() = this.isRemoved
//#endif

fun EntityLivingBase.findHealthReal(): Float {
    //#if MC < 1.21
    val entityHealth = health
    //#else
    //$$ val entityHealth = health
    //#endif
    if (entityHealth == 1024f && !PlatformUtils.IS_LEGACY) {
        return baseMaxHealth.toFloat()
    }
    return entityHealth
}
