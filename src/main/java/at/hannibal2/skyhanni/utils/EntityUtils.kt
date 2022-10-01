package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityBlaze
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.AxisAlignedBB

object EntityUtils {

    fun EntityLiving.hasNameTagWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): Boolean {
        return getNameTagWith(y, contains, debugRightEntity, inaccuracy, debugWrongEntity) != null
    }

    fun EntityLiving.getAllNameTagsWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): List<EntityArmorStand> {
        val center = getLorenzVec().add(0, y, 0)
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

    fun EntityLiving.getNameTagWith(
        y: Int,
        contains: String,
        debugRightEntity: Boolean = false,
        inaccuracy: Double = 1.6,
        debugWrongEntity: Boolean = false,
    ): EntityArmorStand? {
        val center = getLorenzVec().add(0, y, 0)
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

    fun EntityLivingBase.hasMaxHealth(health: Int, boss: Boolean = false): Boolean {
        if (this.baseMaxHealth == health.toDouble()) return true

        //Derpy
        if (this.baseMaxHealth == health.toDouble() * 2) return true

        if (!boss) {
            //Corrupted
            if (this.baseMaxHealth == health.toDouble() * 3) return true

            //Derpy + Corrupted
            if (this.baseMaxHealth == health.toDouble() * 2 * 3) return true
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

    fun WorldClient.getEntitiesNearby(
        clazz: Class<EntityBlaze>,
        location: LorenzVec,
        radius: Double
    ): MutableList<EntityBlaze> = getEntities(clazz) { entity ->
        entity?.getLorenzVec()?.let { it.distance(location) < radius } ?: false
    }

    fun EntityArmorStand.hasSkullTexture(skin: String): Boolean {
        if (inventory == null) return false
        return inventory.any { it != null && it.getSkullTexture() == skin }
    }
}