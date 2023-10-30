package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.features.combat.killDetection.EntityKill
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.utils.EntityUtils.isDisplayNPC
import at.hannibal2.skyhanni.utils.EntityUtils.isRealPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.rayIntersects
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.passive.EntityVillager

object SkyblockMobUtils {
    val mobNameFilter = "\\[.*\\] (.*) \\d+".toRegex()
    val dungeonAttribute = listOf("Flaming", "Stormy", "Speedy", "Fortified", "Healthy", "Healing")

    //TODO find exceptions to the Rule and Analyse it

    open class SkyblockMob(val baseEntity: Entity) {
        //Fun Fact the corresponding ArmorStand for a mob has always the ID + 1
        val armorStand = EntityUtils.getEntityByID(baseEntity.entityId + 1)

        open val name: String = armorStand?.name?.let { mobNameFilter.find(it.removeColor())?.groupValues?.get(1) }
            ?: "Skyblock Name of Mob ${baseEntity.name} found"

        override fun toString(): String = name

        override fun hashCode(): Int {
            return baseEntity.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            // If the other object is an instance of SkyblockMob, compare their baseEntity properties.
            if (other is SkyblockMob) {
                return baseEntity == other.baseEntity
            }

            // If the other object is an instance of Entity, compare it with this SkyblockMob's baseEntity.
            if (other is Entity) {
                return baseEntity == other
            }

            return false
        }
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

    fun createSkyblockMobIfValid(baseEntity: Entity): SkyblockMob? = if (testIfSkyBlockMob(baseEntity))
        createSkyblockMob(baseEntity) else null

    fun testIfSkyBlockMob(entity: Entity): Boolean {
        if (entity !is EntityLivingBase) return false
        if (entity is EntityArmorStand) return false
        if (entity is EntityOtherPlayerMP && (entity.isRealPlayer() || entity.isDisplayNPC())) return false
        if (entity is EntityPlayerSP) return false
        if (entity is EntityWither && (entity.entityId < 0 || entity.name == "Wither")) return false
        if (entity is EntityVillager && entity.maxHealth > 7) return false
        return true
    }

    fun Entity.isSkyBlockMob() = testIfSkyBlockMob(this)


    fun rayTraceForSkyblockMob(
        entity: Entity,
        distance: Double,
        partialTicks: Float,
        offset: LorenzVec = LorenzVec(0, 0, 0)
    ): Entity? {
        val hit = rayTraceForSkyblockMob(entity, partialTicks, offset) ?: return null
        return if (hit.distanceTo(entity.getLorenzVec()) > distance) null else hit
    }

    fun rayTraceForSkyblockMobs(
        entity: Entity,
        distance: Double,
        partialTicks: Float,
        offset: LorenzVec = LorenzVec(0, 0, 0)
    ): List<Entity>? {
        val hits = rayTraceForSkyblockMobs(entity, partialTicks, offset) ?: return null
        val inDistance = hits.filter { it.distanceTo(entity.getLorenzVec()) <= distance }
        if (inDistance.isEmpty()) return null
        return inDistance
    }

    fun rayTraceForSkyblockMob(entity: Entity, partialTicks: Float, offset: LorenzVec = LorenzVec(0, 0, 0)) =
        rayTraceForSkyblockMobs(entity, partialTicks, offset)
            ?.first()

    fun rayTraceForSkyblockMobs(entity: Entity, partialTicks: Float, offset: LorenzVec = LorenzVec(0, 0, 0)):
        List<Entity>? {
        val pos = entity.getPositionEyes(partialTicks).toLorenzVec().add(offset)
        val look = entity.getLook(partialTicks).toLorenzVec().normalize()
        val possibleEntitys = EntityKill.currentEntityLiving.filter { it.entityBoundingBox.rayIntersects(pos, look) }
        if (possibleEntitys.isEmpty()) return null
        possibleEntitys.sortedBy { it.distanceTo(pos) }
        return possibleEntitys
    }

}
