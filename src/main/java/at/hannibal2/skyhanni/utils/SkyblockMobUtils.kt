package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.EntityData
import at.hannibal2.skyhanni.data.skyblockentities.DungeonMob
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockBasicMob
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockBossMob
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockEntity
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockMob
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockSlayerBoss
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockSpecialMob
import at.hannibal2.skyhanni.data.skyblockentities.SummoningMob
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.utils.EntityUtils.isDisplayNPC
import at.hannibal2.skyhanni.utils.EntityUtils.isRealPlayer
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.rayIntersects
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer

object SkyblockMobUtils {
    val mobNameFilter = "(\\[(.*)\\] )?(.Corrupted )?(.*) \\d+".toRegex()
    val slayerNameFilter = "^. (.*) ([IV]+) \\d+".toRegex()
    val bossMobNameFilter = "^. (\\[(.*)\\] )?(.*) \\d+".toRegex()
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
    fun getArmorStand(entity: Entity) = getArmorStand(entity, 1)
    fun getArmorStand(entity: Entity, offSet: Int) =
        EntityUtils.getEntityByID(entity.entityId + offSet) as? EntityArmorStand

    fun getArmorStandByRangeAll(entity: Entity, range: Double) =
        EntityUtils.getEntitiesNearby<EntityArmorStand>(entity.getLorenzVec(), range)

    fun getArmorStandByRange(entity: Entity, range: Double) =
        getArmorStandByRangeAll(entity, range).filter { entity.rotationYaw == it.rotationYaw }.firstOrNull()

    fun EntityArmorStand.isDefaultValue() = this.name == defaultArmorStandName

    private fun createSkyblockMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand): SkyblockMob {
        val name = armorStand.name.removeColor()
        slayerNameFilter.find(name)
            ?.also { return SkyblockSlayerBoss(baseEntity, armorStand, it.groupValues[1], it.groupValues[2]) }
        bossMobNameFilter.find(name)?.also { return SkyblockBossMob(baseEntity, armorStand, it.groupValues[3]) }

        return if (DungeonAPI.inDungeon()) DungeonMob(baseEntity, armorStand) else SkyblockBasicMob(baseEntity, armorStand)
    }

    /** baseEntity must have passed the .isSkyBlockMob() function */
    fun createSkyblockEntity(baseEntity: EntityLivingBase): SkyblockEntity? {
        val armorStand = getArmorStand(baseEntity) ?: getArmorStandByRange(baseEntity, 1.5) ?: return null
        LorenzDebug.log(armorStand.name.removeColor())
        if (armorStand.inventory?.get(4) != null) return armorStandOnlyMobs(baseEntity, armorStand)
        if (armorStand.isDefaultValue()) return null
        val sumReg =
            summoningRegex.find(armorStand.name.removeColor()) ?: return createSkyblockMob(baseEntity, armorStand)
        return SummoningMob(baseEntity, armorStand, sumReg)
    }

    private fun armorStandOnlyMobs(baseEntity: EntityLivingBase, armorStand: EntityArmorStand): SkyblockEntity? {
        // TODO move to repo
        if (armorStand.inventory[4].getSkullTexture() == "ewogICJ0aW1lc3RhbXAiIDogMTYxODQxOTcwMTc1MywKICAicHJvZmlsZUlkIiA6ICI3MzgyZGRmYmU0ODU0NTVjODI1ZjkwMGY4OGZkMzJmOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJCdUlJZXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYThhYmI0NzFkYjBhYjc4NzAzMDExOTc5ZGM4YjQwNzk4YTk0MWYzYTRkZWMzZWM2MWNiZWVjMmFmOGNmZmU4IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=") return SkyblockSpecialMob(baseEntity, armorStand, "Rat")
        return null
    }

    fun createSkyblockMobIfValid(baseEntity: EntityLivingBase): SkyblockMob? =
        if (baseEntity.isSkyBlockMob()) createSkyblockEntity(baseEntity) as? SkyblockMob else null


    class ownerShip(val ownerName: String) {
        val ownerPlayer = EntityData.currentRealPlayers.firstOrNull { it.name == ownerName }
        override fun equals(other: Any?): Boolean {
            if (other is EntityPlayer) return ownerPlayer == other || ownerName == other.name
            if (other is String) return ownerName == other
            return false
        }

        override fun hashCode(): Int {
            return ownerName.hashCode()
        }
    }


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
