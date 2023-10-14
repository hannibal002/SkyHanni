package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.features.mobs.EntityKill
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.rayIntersects
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand

object SkyblockMobUtils {
    val mobNameFilter = "\\[.*\\] (.*) \\d+".toRegex() //TODO change it so it works also with Dungeon Mobs
    class SkyblockMob(val baseEntity: Entity) {
        //Fun Fact the corresponding ArmorStand for a mob has always the mobId + 1
        val armorStand = EntityUtils.getEntityById(baseEntity.entityId+1)

        val name: String = armorStand?.name?.let { mobNameFilter.find(it.removeColor())?.groupValues?.get(1) }
            ?: "Skyblock Name of Mob ${baseEntity.name} found"

        override fun toString() : String = name
    }

    fun testIfSkyBlockMob(entity: Entity): Boolean {
        if(entity !is EntityLivingBase) return false
        if (entity is EntityArmorStand || entity is EntityPlayerSP) return false
        //Protection that no real Player gets added. Only difference to a custom mob is that every SkyblockItem has a nbtTag
        if (entity.inventory != null) { //TODO fix this
            if (entity.inventory.isNotEmpty() && entity.inventory.any { it != null && it.tagCompound == null })
            {
                if(EntityKill.config.mobKilldetetctionLogPlayerCantBeAdded) {
                    LorenzDebug.log("Entity ${entity.name} is not allowed in HitList")
                }
                return false
            }
        }
        return true
    }
    fun Entity.isSkyBlockMob() = testIfSkyBlockMob(this)


    fun rayTraceForSkyblockMob(entity: Entity, distance : Double,partialTicks: Float): Entity? {
        val hit = rayTraceForSkyblockMob(entity,partialTicks) ?: return null
        return if(hit.distanceTo(entity.getLorenzVec()) > distance) null else hit
    }

    fun rayTraceForSkyblockMob(entity: Entity,partialTicks: Float): Entity? {
        val pos = entity.getPositionEyes(partialTicks).toLorenzVec()
        val look = entity.getLook(partialTicks).toLorenzVec().normalize()
        val possibleEntitys = EntityKill.currentEntityLiving.filter { it.entityBoundingBox.rayIntersects(pos, look) }
        if (possibleEntitys.isEmpty()) return null
        possibleEntitys.sortedBy { it.distanceTo(pos) }
        return possibleEntitys.first()
    }
}