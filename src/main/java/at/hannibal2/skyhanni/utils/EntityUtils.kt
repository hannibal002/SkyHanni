package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.potion.Potion
import net.minecraft.util.AxisAlignedBB

object EntityUtils {

    fun EntityLivingBase.hasNameTagWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): Boolean {
        return getNameTagWith(y, contains, debugRightEntity, inaccuracy, debugWrongEntity) != null
    }

    fun EntityLivingBase.getAllNameTagsWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): List<EntityArmorStand> {
        val center = getLorenzVec().add(y = y)
        val a = center.add(-inaccuracy, -inaccuracy - 3, -inaccuracy).toBlocPos()
        val b = center.add(inaccuracy, inaccuracy + 3, inaccuracy).toBlocPos()
        val alignedBB = AxisAlignedBB(a, b)
        val clazz = EntityArmorStand::class.java
        val found = worldObj.getEntitiesWithinAABB(clazz, alignedBB)
        return found.filter {
            val result = it.name.contains(contains)
            if (debugWrongEntity && !result) {
                LorenzUtils.consoleLog("wrong entity in aabb: '" + it.name + "'")
            }
            if (debugRightEntity && result) {
                LorenzUtils.consoleLog("mob: " + center.printWithAccuracy(2))
                LorenzUtils.consoleLog("nametag: " + it.getLorenzVec().printWithAccuracy(2))
                LorenzUtils.consoleLog("accuracy: " + it.getLorenzVec().subtract(center).printWithAccuracy(3))
            }
            result
        }
    }

    fun EntityLivingBase.getAllNameTagsInRadiusWith(
        contains: String,
        radius: Double = 3.0,
    ): List<EntityArmorStand> {
        val center = getLorenzVec().add(y = 3)
        val a = center.add(-radius, -radius - 3, -radius).toBlocPos()
        val b = center.add(radius, radius + 3, radius).toBlocPos()
        val alignedBB = AxisAlignedBB(a, b)
        val clazz = EntityArmorStand::class.java
        val found = worldObj.getEntitiesWithinAABB(clazz, alignedBB)
        return found.filter {
            val result = it.name.contains(contains)
            result
        }
    }

    fun EntityLivingBase.getNameTagWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): EntityArmorStand? {
        val center = getLorenzVec().add(y = y)
        val a = center.add(-inaccuracy, -inaccuracy - 3, -inaccuracy).toBlocPos()
        val b = center.add(inaccuracy, inaccuracy + 3, inaccuracy).toBlocPos()
        val alignedBB = AxisAlignedBB(a, b)
        val clazz = EntityArmorStand::class.java
        val found = worldObj.getEntitiesWithinAABB(clazz, alignedBB)
        return found.find {
            val result = it.name.contains(contains)
            if (debugWrongEntity && !result) {
                LorenzUtils.consoleLog("wrong entity in aabb: '" + it.name + "'")
            }
            if (debugRightEntity && result) {
                LorenzUtils.consoleLog("mob: " + center.printWithAccuracy(2))
                LorenzUtils.consoleLog("nametag: " + it.getLorenzVec().printWithAccuracy(2))
                LorenzUtils.consoleLog("accuracy: " + it.getLorenzVec().subtract(center).printWithAccuracy(3))
            }
            result
        }
    }

    fun EntityLivingBase.hasBossHealth(health: Int): Boolean = this.hasMaxHealth(health, true)

    //TODO remove baseMaxHealth
    fun EntityLivingBase.hasMaxHealth(health: Int, boss: Boolean = false, maxHealth: Int = baseMaxHealth): Boolean {
        val derpyMultiplier = if (LorenzUtils.isDerpy) 2 else 1
        if (maxHealth == health * derpyMultiplier) return true

        if (!boss && !LorenzUtils.inDungeons) {
            //Corrupted
            if (maxHealth == health * 3 * derpyMultiplier) return true
            // Runic
            if (maxHealth == health * 4 * derpyMultiplier) return true
            // Corrupted+Runic
            if (maxHealth == health * 12 * derpyMultiplier) return true
        }

        return false
    }

    fun EntityPlayer.getSkinTexture(): String? {
        val gameProfile = gameProfile ?: return null

        return gameProfile.properties.entries()
            .filter { it.key == "textures" }
            .map { it.value }
            .firstOrNull { it.name == "textures" }
            ?.value
    }

    inline fun <reified T : Entity> getEntitiesNextToPlayer(radius: Double): Sequence<T> =
        getEntitiesNearby<T>(LocationUtils.playerLocation(), radius)

    inline fun <reified T : Entity> getEntitiesNearby(location: LorenzVec, radius: Double): Sequence<T> =
        getEntities<T>().filter { it.distanceTo(location) < radius }

    fun EntityLivingBase.isAtFullHealth() = baseMaxHealth == health.toInt()

    fun EntityArmorStand.hasSkullTexture(skin: String): Boolean {
        if (inventory == null) return false
        return inventory.any { it != null && it.getSkullTexture() == skin }
    }

    fun EntityPlayer.isNPC() = uniqueID == null || uniqueID.version() != 4

    fun EntityLivingBase.hasPotionEffect(potion: Potion) = getActivePotionEffect(potion) != null

    fun EntityLivingBase.getArmorInventory(): Array<ItemStack?>? =
        if (this is EntityPlayer) inventory.armorInventory else null

    fun EntityEnderman.getBlockInHand(): IBlockState? = heldBlockState

    inline fun <reified R : Entity> getEntities(): Sequence<R> = getAllEntities().filterIsInstance<R>()

    fun getAllEntities(): Sequence<Entity> = Minecraft.getMinecraft()?.theWorld?.loadedEntityList?.let {
        if (Minecraft.getMinecraft().isCallingFromMinecraftThread) it else it.toMutableList()
    }?.asSequence()?.filterNotNull() ?: emptySequence()

    fun Entity.canBeSeen(radius: Double = 150.0) = getLorenzVec().add(y = 0.5).canBeSeen(radius)

    fun getEntityByID(entityId: Int) = Minecraft.getMinecraft()?.thePlayer?.entityWorld?.getEntityByID(entityId)
}
