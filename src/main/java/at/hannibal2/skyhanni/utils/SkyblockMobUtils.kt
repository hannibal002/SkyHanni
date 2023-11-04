package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.EntityData
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.utils.EntityUtils.isDisplayNPC
import at.hannibal2.skyhanni.utils.EntityUtils.isRealPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
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
    private val summoningRegex = "^(\\w+)'s (.*) \\d+".toRegex()

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

    // The corresponding ArmorStand for a mob has always the ID + 1
    private fun getArmorStand(entity: Entity) = EntityUtils.getEntityByID(entity.entityId + 1) as? EntityArmorStand

    abstract class SkyblockEntity(val baseEntity: Entity, val armorStand: EntityArmorStand? = getArmorStand(baseEntity)) {

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

        fun isInRender() = baseEntity.distanceToPlayer() < EntityData.ENTITY_RENDER_RANGE_IN_BLOCKS
    }

    class DisplayNPC(baseEntity: Entity) : SkyblockEntity(baseEntity) {
        override val name = armorStand?.name ?: ""
    }

    class SummoningMob(baseEntity: Entity, armorStand: EntityArmorStand? = getArmorStand(baseEntity), result: MatchResult) : SkyblockEntity(baseEntity, armorStand) {
        override val name = result.groupValues[1]

        private val ownerName = result.groupValues[0]
        val owner = EntityData.currentRealPlayers.firstOrNull { it.name == ownerName }
    }

    // TODO Test Boss mobs
    open class SkyblockMob(baseEntity: Entity, armorStand: EntityArmorStand? = getArmorStand(baseEntity)) : SkyblockEntity(baseEntity, armorStand) {
        override val name = armorStand?.name?.let {
            mobNameFilter.find(it.removeColor())?.groupValues?.get(1) ?: it.removeColor()
        } ?: run { errorNameFinding(baseEntity.name) }
    }

    class DungeonMob(baseEntity: Entity, armorStand: EntityArmorStand? = getArmorStand(baseEntity)) : SkyblockMob(baseEntity, armorStand) {

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

    private fun createSkyblockMob(baseEntity: Entity, armorStand: EntityArmorStand): SkyblockMob =
        if (DungeonAPI.inDungeon()) DungeonMob(baseEntity, armorStand) else SkyblockMob(baseEntity, armorStand)

    /** baseEntity must have passed the .isSkyBlockMob() function */
    fun createSkyblockEntity(baseEntity: Entity): SkyblockEntity? {
        val armorStand = getArmorStand(baseEntity) ?: return null
        val sumReg = summoningRegex.find(armorStand.name) ?: return createSkyblockMob(baseEntity, armorStand)
        return SummoningMob(baseEntity, armorStand, sumReg)
    }

    fun createSkyblockMobIfValid(baseEntity: Entity): SkyblockMob? =
        if (baseEntity.isSkyBlockMob()) createSkyblockEntity(baseEntity) as? SkyblockMob else null

    fun rayTraceForSkyblockMob(entity: Entity, distance: Double, partialTicks: Float, offset: LorenzVec = LorenzVec()) =
        rayTraceForSkyblockMob(entity, partialTicks, offset)?.takeIf {
            it.baseEntity.distanceTo(entity.getLorenzVec()) <= distance
        }

    fun rayTraceForSkyblockMobs(entity: Entity, distance: Double, partialTicks: Float, offset: LorenzVec = LorenzVec()) =
        rayTraceForSkyblockMobs(entity, partialTicks, offset)?.filter {
            it.baseEntity.distanceTo(entity.getLorenzVec()) <= distance
        }.takeIf { it?.isNotEmpty() ?: false }

    fun rayTraceForSkyblockMob(entity: Entity, partialTicks: Float, offset: LorenzVec = LorenzVec()) =
        rayTraceForSkyblockMobs(entity, partialTicks, offset)?.first()

    fun rayTraceForSkyblockMobs(entity: Entity, partialTicks: Float, offset: LorenzVec = LorenzVec()): List<SkyblockMob>? {
        val pos = entity.getPositionEyes(partialTicks).toLorenzVec().add(offset)
        val look = entity.getLook(partialTicks).toLorenzVec().normalize()
        val possibleEntitys = EntityData.currentSkyblockMobs.filter {
            it.baseEntity.entityBoundingBox.rayIntersects(
                pos, look
            )
        }
        if (possibleEntitys.isEmpty()) return null
        possibleEntitys.sortedBy { it.baseEntity.distanceTo(pos) }
        return possibleEntitys
    }

}
