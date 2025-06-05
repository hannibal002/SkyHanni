package at.hannibal2.skyhanni.utils.compat

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
