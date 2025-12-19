package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.item.ItemStack
import net.minecraft.world.World
//#if MC < 1.16
//$$ import net.minecraft.entity.monster.EntitySkeleton
//#else
import net.minecraft.entity.mob.WitherSkeletonEntity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.EntityType
//#endif

fun ArmorStandEntity.getStandHelmet(): ItemStack? =
//#if MC < 1.16
//$$     this.getEquipmentInSlot(4)
//#else
this.getEquippedStack(EquipmentSlot.HEAD)
//#endif

fun MobEntity.getEntityHelmet(): ItemStack? =
//#if MC < 1.16
//$$     this.getEquipmentInSlot(4)
//#else
this.getEquippedStack(EquipmentSlot.HEAD)
//#endif

fun LivingEntity.getAllEquipment() =
//#if MC < 1.16
//$$     this.inventory
//#elseif MC < 1.21
//$$ this.armorItems
//#else
this.equipment.map.values.toTypedArray()
//#endif

fun Entity.getFirstPassenger(): Entity? =
//#if MC < 1.16
//$$     this.riddenByEntity
//#else
this.passengerList.firstOrNull()
//#endif

fun ArmorStandEntity.getHandItem(): ItemStack? =
//#if MC < 1.16
//$$     this.getEquipmentInSlot(0)
//#else
this.getEquippedStack(EquipmentSlot.MAINHAND)
//#endif

fun ArmorStandEntity.getInventoryItems(): Array<ItemStack> =
    //#if MC < 1.16
    //$$ inventory
//#else
arrayOf(
getEquippedStack(EquipmentSlot.MAINHAND),
getEquippedStack(EquipmentSlot.FEET),
getEquippedStack(EquipmentSlot.LEGS),
getEquippedStack(EquipmentSlot.CHEST),
getEquippedStack(EquipmentSlot.HEAD),
getEquippedStack(EquipmentSlot.OFFHAND),
)
//#endif

fun Entity.getEntityLevel(): World =
//#if MC < 1.16
//$$     this.entityWorld
//#else
this.world
//#endif

fun createWitherSkeleton(world: World?): LivingEntity =
//#if MC < 1.16
//$$     EntitySkeleton(world).also { it.skeletonType = 1 }
//#else
WitherSkeletonEntity(EntityType.WITHER_SKELETON, world)
//#endif

//#if MC > 1.21
val Entity.deceased: Boolean
    get() = this.isRemoved
//#endif

fun LivingEntity.findHealthReal(): Float {
    //#if MC < 1.21
    //$$ val entityHealth = findHealthReal()
    //#else
    val entityHealth = health
    //#endif
    if (entityHealth == 1024f && !PlatformUtils.IS_LEGACY) {
        return baseMaxHealth.toFloat()
    }
    return entityHealth
}
