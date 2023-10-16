package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.mobs.EntityKill
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.rayIntersects
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand

object SkyblockMobUtils {
    val mobNameFilter = "\\[.*\\] (.*) \\d+".toRegex()
    val dungeonAttribute = listOf("Flaming", "Stormy", "Speedy", "Fortified", "Healthy", "Healing")

    //TODO find exceptions to the Rule and Analyse it

    open class SkyblockMob(val baseEntity: Entity) {
        //Fun Fact the corresponding ArmorStand for a mob has always the ID + 1
        val armorStand = EntityUtils.getEntityById(baseEntity.entityId + 1)

        open val name: String = armorStand?.name?.let { mobNameFilter.find(it.removeColor())?.groupValues?.get(1) }
            ?: "Skyblock Name of Mob ${baseEntity.name} found"

        override fun toString(): String = name
    }

    class DungeonMob(baseEntity: Entity) : SkyblockMob(baseEntity) {

        val Attribute: String?
        val hasStar: Boolean
        override val name: String

        init { //TODO test with every dungeon boss
            var initStartIndex = 0
            val nameWithoutColor = armorStand?.name?.removeColor()
            if (nameWithoutColor != null) {
                val words = nameWithoutColor.split(
                    " ",
                    ignoreCase = true,
                    limit = 6
                ) // if new Dungeon Mobs get added that have a name longer than 3 words this must be changed (limit =
                // max words in name + 1x Attribute name + 1x Health + 1x Star
                hasStar = words[initStartIndex] == "✯"
                if (hasStar) initStartIndex++

                if (dungeonAttribute.contains(words[initStartIndex])) {
                    Attribute = words[initStartIndex]
                    initStartIndex++
                } else Attribute = null

                if (words[initStartIndex].startsWith("[")) {
                    initStartIndex++ // For a wierd reason the Undead Skeletons (or similar)
                    // can spawn with a level if they are summoned with the 3 skulls
                }

                name = words.subList(initStartIndex, words.lastIndex).joinToString(separator = " ")
            } else {
                Attribute = null
                hasStar = false
                name = "Skyblock Name of Mob ${baseEntity.name} found"
            }
        }
    }

    /** baseEntity must pass the testIfSkyBlockMob function */
    fun createSkyblockMob(baseEntity: Entity): SkyblockMob = if (DungeonAPI.inDungeon()) DungeonMob(baseEntity) else
        SkyblockMob(baseEntity)

    fun testIfSkyBlockMob(entity: Entity): Boolean {
        if (entity !is EntityLivingBase) return false
        if (entity is EntityArmorStand || entity is EntityPlayerSP) return false
        //Protection that no real Player gets added. Only difference to a custom mob is that every SkyblockItem has a nbtTag
        if (entity.inventory != null) { //TODO fix this
            if (entity.inventory.isNotEmpty() && entity.inventory.any { it != null && it.tagCompound == null }) {
                if (EntityKill.config.mobKilldetetctionLogPlayerCantBeAdded) {
                    LorenzDebug.log("Entity ${entity.name} is not allowed in HitList")
                }
                return false
            }
        }
        return true
    }

    fun Entity.isSkyBlockMob() = testIfSkyBlockMob(this)


    fun rayTraceForSkyblockMob(entity: Entity, distance: Double, partialTicks: Float): Entity? {
        val hit = rayTraceForSkyblockMob(entity, partialTicks) ?: return null
        return if (hit.distanceTo(entity.getLorenzVec()) > distance) null else hit
    }

    fun rayTraceForSkyblockMobs(entity: Entity, distance: Double, partialTicks: Float): List<Entity>? {
        val hits = rayTraceForSkyblockMobs(entity, partialTicks) ?: return null
        val inDistance = hits.filter { it.distanceTo(entity.getLorenzVec()) <= distance }
        if (inDistance.isEmpty()) return null
        return inDistance
    }

    fun rayTraceForSkyblockMob(entity: Entity, partialTicks: Float): Entity? {
        val hits = rayTraceForSkyblockMobs(entity, partialTicks) ?: return null
        return hits.first()
    }

    fun rayTraceForSkyblockMobs(entity: Entity, partialTicks: Float): List<Entity>? {
        val pos = entity.getPositionEyes(partialTicks).toLorenzVec()
        val look = entity.getLook(partialTicks).toLorenzVec().normalize()
        val possibleEntitys = EntityKill.currentEntityLiving.filter { it.entityBoundingBox.rayIntersects(pos, look) }
        if (possibleEntitys.isEmpty()) return null
        possibleEntitys.sortedBy { it.distanceTo(pos) }
        return possibleEntitys
    }
}