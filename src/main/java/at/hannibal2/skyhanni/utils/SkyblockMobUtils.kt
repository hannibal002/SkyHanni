package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.EntityData
import at.hannibal2.skyhanni.data.skyblockentities.DungeonMob
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockEntity
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockMob
import at.hannibal2.skyhanni.data.skyblockentities.SummoningMob
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
    private val summoningRegex = "^(\\w+)'s (.*) \\d+".toRegex()
    private const val defaultArmorStandName = "Armor Stand"

    fun errorNameFinding(name: String): String {
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
    fun getArmorStand(entity: Entity) = EntityUtils.getEntityByID(entity.entityId + 1) as? EntityArmorStand

    private fun createSkyblockMob(baseEntity: Entity, armorStand: EntityArmorStand): SkyblockMob =
        if (DungeonAPI.inDungeon()) DungeonMob(baseEntity, armorStand) else SkyblockMob(baseEntity, armorStand)

    /** baseEntity must have passed the .isSkyBlockMob() function */
    fun createSkyblockEntity(baseEntity: Entity): SkyblockEntity? {
        val armorStand = getArmorStand(baseEntity) ?: return null
        LorenzDebug.log(armorStand.name)
        if (armorStand.name == defaultArmorStandName) return null
        val sumReg =
            summoningRegex.find(armorStand.name.removeColor()) ?: return createSkyblockMob(baseEntity, armorStand)
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
