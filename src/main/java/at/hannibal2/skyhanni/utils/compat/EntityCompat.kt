package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
//#if MC < 1.16
//$$ import net.minecraft.entity.monster.EntitySkeleton
//#else
import net.minecraft.world.entity.monster.WitherSkeleton
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.EntityType
//#endif

fun ArmorStand.getStandHelmet(): ItemStack? =
//#if MC < 1.16
//$$     this.getEquipmentInSlot(4)
//#else
this.getItemBySlot(EquipmentSlot.HEAD)
//#endif

fun Mob.getEntityHelmet(): ItemStack? =
//#if MC < 1.16
//$$     this.getEquipmentInSlot(4)
//#else
this.getItemBySlot(EquipmentSlot.HEAD)
//#endif

fun LivingEntity.getAllEquipment() =
//#if MC < 1.16
//$$     this.inventory
//#elseif MC < 1.21
//$$ this.armorItems
//#else
this.equipment.items.values.toTypedArray()
//#endif

fun Entity.getFirstPassenger(): Entity? =
//#if MC < 1.16
//$$     this.riddenByEntity
//#else
this.passengers.firstOrNull()
//#endif

fun ArmorStand.getHandItem(): ItemStack? =
//#if MC < 1.16
//$$     this.getEquipmentInSlot(0)
//#else
this.getItemBySlot(EquipmentSlot.MAINHAND)
//#endif

fun ArmorStand.getInventoryItems(): Array<ItemStack> =
    //#if MC < 1.16
    //$$ inventory
//#else
arrayOf(
getItemBySlot(EquipmentSlot.MAINHAND),
getItemBySlot(EquipmentSlot.FEET),
getItemBySlot(EquipmentSlot.LEGS),
getItemBySlot(EquipmentSlot.CHEST),
getItemBySlot(EquipmentSlot.HEAD),
getItemBySlot(EquipmentSlot.OFFHAND),
)
//#endif

fun Entity.getEntityLevel(): Level =
//#if MC < 1.16
//$$     this.entityWorld
//#else
this.level()
//#endif

fun createWitherSkeleton(world: Level?): LivingEntity =
//#if MC < 1.16
//$$     EntitySkeleton(world).also { it.skeletonType = 1 }
//#else
WitherSkeleton(EntityType.WITHER_SKELETON, world)
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
