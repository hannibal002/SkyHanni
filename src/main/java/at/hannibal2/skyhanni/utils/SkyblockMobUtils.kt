package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.EntityData
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

object SkyblockMobUtils {
    val mobNameFilter = "\\[.*\\] (.*) \\d+".toRegex()
    val dungeonAttribute = listOf("Flaming", "Stormy", "Speedy", "Fortified", "Healthy", "Healing")

    private fun errorNameFinding(name: String): String {
        LorenzDebug.chatAndLog("Skyblock Name of Mob $name not found")
        return ""
    }

    fun Entity.isSkyBlockMob(): Boolean {
        if (this !is EntityLivingBase) return false
        if (this is EntityArmorStand) return false
        if (this is EntityOtherPlayerMP && this.isRealPlayer()) return false
        if (this.isDisplayNPC()) return false
        if (this is EntityWither && (this.entityId < 0 || this.name == "Wither")) return false
        if (this is EntityPlayerSP) return false
        return true
    }

    abstract class SkyblockEntity(val baseEntity: Entity) {
        // The corresponding ArmorStand for a mob has always the ID + 1
        val armorStand = EntityUtils.getEntityByID(baseEntity.entityId + 1)

        // If an entity has a hologram (second ArmorStand) it has the ID + 2 if not there will be another mob
        val hologram by lazy {
            EntityUtils.getEntityByID(baseEntity.entityId + 2)?.takeIf { it is EntityArmorStand } as? EntityArmorStand
        }

        abstract val name: String

        override fun toString() = name

        override fun hashCode(): Int {
            return baseEntity.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            if (other is SkyblockEntity) {
                return baseEntity == other.baseEntity
            }

            if (other is Entity) {
                return baseEntity == other
            }

            return false
        }
    }

    class DisplayNPC(baseEntity: Entity) : SkyblockEntity(baseEntity) {
        override val name = armorStand?.name ?: ""
    }

    // TODO Test Boss mobs
    open class SkyblockMob(baseEntity: Entity) : SkyblockEntity(baseEntity) {
        override val name = armorStand?.name?.let {
            mobNameFilter.find(it.removeColor())?.groupValues?.get(1) ?: it.removeColor()
        } ?: run { errorNameFinding(baseEntity.name) }
    }

    class DungeonMob(baseEntity: Entity) : SkyblockMob(baseEntity) {

        val Attribute: String?
        val hasStar: Boolean
        override val name: String

        init { // TODO test with every dungeon boss
            var initStartIndex = 0
            val nameWithoutColor = armorStand?.name?.removeColor()
            if (nameWithoutColor != null) {
                // If new Dungeon Mobs get added that have a name longer than 3 words this must be changed
                // limit = max words in name + 1x Attribute name + 1x Health + 1x Star
                val words = nameWithoutColor.split(" ", ignoreCase = true, limit = 6)

                hasStar = (words[initStartIndex] == "✯").also { if (it) initStartIndex++ }

                Attribute = dungeonAttribute.firstOrNull { it == words[initStartIndex] }?.also { initStartIndex++ }

                // For a wierd reason the Undead Skeletons (or similar)
                // can spawn with a level if they are summoned with the 3 skulls
                words[initStartIndex].startsWith("[").also { if (it) initStartIndex++ }

                name = words.subList(initStartIndex, words.lastIndex).joinToString(separator = " ")
            } else {
                Attribute = null
                hasStar = false
                name = errorNameFinding(baseEntity.name)
            }
        }
    }

    /** baseEntity must have passed the .isSkyBlockMob() function */
    fun createSkyblockMob(baseEntity: Entity): SkyblockMob =
        if (DungeonAPI.inDungeon()) DungeonMob(baseEntity) else SkyblockMob(baseEntity)

    fun createSkyblockMobIfValid(baseEntity: Entity): SkyblockMob? =
        if (baseEntity.isSkyBlockMob()) createSkyblockMob(baseEntity) else null

    fun rayTraceForSkyblockMob(entity: Entity, distance: Double, partialTicks: Float, offset: LorenzVec = LorenzVec()) =
        rayTraceForSkyblockMob(entity, partialTicks, offset)?.takeIf { it.distanceTo(entity.getLorenzVec()) <= distance }

    fun rayTraceForSkyblockMobs(entity: Entity, distance: Double, partialTicks: Float, offset: LorenzVec = LorenzVec()) =
        rayTraceForSkyblockMobs(entity, partialTicks, offset)?.filter {
            it.distanceTo(entity.getLorenzVec()) <= distance
        }.takeIf { it?.isNotEmpty() ?: false }

    fun rayTraceForSkyblockMob(entity: Entity, partialTicks: Float, offset: LorenzVec = LorenzVec()) =
        rayTraceForSkyblockMobs(entity, partialTicks, offset)?.first()

    fun rayTraceForSkyblockMobs(entity: Entity, partialTicks: Float, offset: LorenzVec = LorenzVec()): List<Entity>? {
        val pos = entity.getPositionEyes(partialTicks).toLorenzVec().add(offset)
        val look = entity.getLook(partialTicks).toLorenzVec().normalize()
        val possibleEntitys = EntityData.currentSkyblockMobs.filter { it.entityBoundingBox.rayIntersects(pos, look) }
        if (possibleEntitys.isEmpty()) return null
        possibleEntitys.sortedBy { it.distanceTo(pos) }
        return possibleEntitys
    }

}
