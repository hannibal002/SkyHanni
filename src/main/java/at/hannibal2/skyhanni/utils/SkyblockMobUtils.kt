package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.EntityData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.skyblockentities.DungeonMob
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockBasicMob
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockBossMob
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockEntity
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockInvalidEntity
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockMob
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockProjectileEntity
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockSlayerBoss
import at.hannibal2.skyhanni.data.skyblockentities.SkyblockSpecialMob
import at.hannibal2.skyhanni.data.skyblockentities.SummingOrSkyblockMob
import at.hannibal2.skyhanni.data.skyblockentities.SummoningMob
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.utils.EntityUtils.isDisplayNPC
import at.hannibal2.skyhanni.utils.EntityUtils.isFarmMob
import at.hannibal2.skyhanni.utils.EntityUtils.isRealPlayer
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.rayIntersects
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityGiantZombie
import net.minecraft.entity.monster.EntityGuardian
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.passive.EntityChicken
import net.minecraft.entity.passive.EntityCow
import net.minecraft.entity.passive.EntityHorse
import net.minecraft.entity.passive.EntityMooshroom
import net.minecraft.entity.passive.EntityPig
import net.minecraft.entity.passive.EntityRabbit
import net.minecraft.entity.passive.EntitySheep
import net.minecraft.entity.player.EntityPlayer

object SkyblockMobUtils {
    val mobNameFilter = "(\\[(.*)\\] )?(.Corrupted )?(.*) [\\d❤]+".toRegex()
    val slayerNameFilter = "^. (.*) ([IV]+) \\d+".toRegex()
    val bossMobNameFilter = "^. (\\[(.*)\\] )?(.*) ([\\d\\/Mk.,❤\\?]+|█+) .$".toRegex()
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
        // if (this is EntityHorse && this.maxHealth != 35_000f) return false

        // Maybe a Problem with Summons TODO find other solution (broke stuff)
        // if (this is EntitySlime && LorenzUtils.skyBlockIsland == IslandType.CRIMSON_ISLE) return false
        if (this is EntityOtherPlayerMP && this.isRealPlayer()) return false
        if (this.isDisplayNPC()) return false
        if (this is EntityWither && (this.entityId < 0 || this.name == "Wither")) return false
        if (this is EntityPlayerSP) return false
        return true
    }

    // The corresponding ArmorStand for a mob has always the ID + 1 (with some exceptions)
    fun getArmorStand(entity: Entity, offset: Int = 1) = getNextEntity(entity, offset) as? EntityArmorStand

    fun getNextEntity(entity: Entity, offset: Int) = EntityUtils.getEntityByID(entity.entityId + offset)

    fun getArmorStandByRangeAll(entity: Entity, range: Double) =
        EntityUtils.getEntitiesNearby<EntityArmorStand>(entity.getLorenzVec(), range)

    fun getArmorStandByRange(entity: Entity, range: Double) =
        getArmorStandByRangeAll(entity, range).filter { entity.rotationYaw == it.rotationYaw }.firstOrNull()

    fun EntityArmorStand.isDefaultValue() = this.name == defaultArmorStandName

    private fun createSkyblockMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand, extraEntityList: List<EntityLivingBase>): SkyblockMob {
        val name = armorStand.name.removeColor()
        slayerNameFilter.find(name)
            ?.also { return SkyblockSlayerBoss(baseEntity, armorStand, it.groupValues[1], it.groupValues[2], extraEntityList) }
        bossMobNameFilter.find(name)
            ?.also { return SkyblockBossMob(baseEntity, armorStand, it.groupValues[3], extraEntityList) }

        return if (DungeonAPI.inDungeon()) DungeonMob(baseEntity, armorStand) else SkyblockBasicMob(baseEntity, armorStand, extraEntityList)
    }

    /** baseEntity must have passed the .isSkyBlockMob() function */
    fun createSkyblockEntity(baseEntity: EntityLivingBase): SkyblockEntity? {
        noArmorStandMobs(baseEntity)?.also { return it }

        val nextEntity = getNextEntity(baseEntity, 1) as? EntityLivingBase

        exceptions(baseEntity, nextEntity)?.also { return if (it is exceptionSkipDetection) null else it }

        // Check if Late Stack
        nextEntity?.let { nextEntity ->
            EntityData.getSummonOrSkyblockMob(nextEntity)?.apply { addEntityInFront(baseEntity) }
                ?.also { return SkyblockInvalidEntity(baseEntity, "Added to $it") }
        }

        // Stack up the mob
        var caughtSkyblockMob: SummingOrSkyblockMob? = null
        val extraEntityList =
            generateSequence(nextEntity) { getNextEntity(it, 1) as? EntityLivingBase }.takeWhileInclusive { entity ->
                !(entity is EntityArmorStand && !entity.isDefaultValue()) && EntityData.getSummonOrSkyblockMob(entity)
                    ?.also {
                        caughtSkyblockMob = it
                    }?.run { false } ?: true
            }.toList()
        // If Late Stack add all entities
        caughtSkyblockMob?.apply { addEntityInFront(extraEntityList.dropLast(1)) }
            ?.also { return SkyblockInvalidEntity(baseEntity, "Added to $it") }

        val armorStand = extraEntityList.lastOrNull() as? EntityArmorStand ?: return null

        if (armorStand.isDefaultValue()) return null
        val sumReg = summoningRegex.find(armorStand.name.removeColor())
            ?: return createSkyblockMob(baseEntity, armorStand, extraEntityList.dropLast(1))
        return SummoningMob(baseEntity, armorStand, extraEntityList.dropLast(1).toMutableList(), sumReg)
    }

    private fun noArmorStandMobs(baseEntity: EntityLivingBase): SkyblockEntity? {
        if (baseEntity is EntityBat) return createBat(baseEntity)
        if (baseEntity.isFarmMob()) return createFarmMobs(baseEntity)
        if (baseEntity is EntityDragon) return SkyblockBossMob(baseEntity, null, baseEntity.name.removeColor(), null)
        if (baseEntity is EntityGiantZombie && baseEntity.name == "Dinnerbone") return SkyblockProjectileEntity(baseEntity, "Giant Sword") // Will false trigger if there is another Dinnerbone Giant
        return null
    }

    fun <T> Sequence<T>.takeWhileInclusive(predicate: (T) -> Boolean) = sequence {
        with(iterator()) {
            while (hasNext()) {
                val next = next()
                yield(next)
                if (!predicate(next)) break
            }
        }
    }

    private class exceptionSkipDetection(baseEntity: EntityLivingBase) : SkyblockEntity(baseEntity, null) {
        override val name: String = ""
    }

    private fun exceptions(baseEntity: EntityLivingBase, nextEntity: EntityLivingBase?): SkyblockEntity? {
        when (LorenzUtils.skyBlockIsland) {
            IslandType.PRIVATE_ISLAND -> if (nextEntity !is EntityArmorStand) return SkyblockInvalidEntity(baseEntity, "Minion Mob " + baseEntity.name) // TODO fix to always include Valid Mobs on Private Island
            IslandType.THE_RIFT -> if (baseEntity is EntitySlime && nextEntity is EntitySlime) return SkyblockInvalidEntity(baseEntity, "Bacte Tentacle")
            IslandType.CRIMSON_ISLE -> {
                if (baseEntity is EntitySlime && nextEntity?.name == "§f§lCOLLECT!") return SkyblockInvalidEntity(baseEntity, "Heavy Pearl")
                if (baseEntity is EntityPig && nextEntity is EntityPig) return SkyblockInvalidEntity(baseEntity, "Matriarch Tongue")
            }

            IslandType.HUB -> {
                if (baseEntity is EntityZombie) { // Rat
                    val from = 5
                    val to = 9
                    generateSequence(from) { it + 1 }.take(to - from).map { i ->
                        getArmorStand(
                            baseEntity, i
                        )
                    }.firstOrNull {
                        it != null && it.distanceTo(baseEntity) < 4.0 && it.inventory?.get(4)
                            ?.getSkullTexture() == RatSkull
                    }?.also { return SkyblockSpecialMob(baseEntity, it, "Rat") }
                    if (nextEntity is EntityZombie) return exceptionSkipDetection(baseEntity)
                }
            }

            else -> {}
        }

        val armorStand = nextEntity as? EntityArmorStand ?: return null
        armorStandOnlyMobs(baseEntity, armorStand)?.also { return it }
        return when {
            baseEntity is EntityPig && armorStand.name.endsWith("'s Pig") -> SkyblockInvalidEntity(
                baseEntity, "Pig Pet"
            )

            baseEntity is EntityHorse && armorStand.name.endsWith("'s Skeleton Horse") -> SkyblockInvalidEntity(
                baseEntity, "Skeleton Horse Pet"
            )

            baseEntity is EntityHorse && armorStand.name.endsWith("'s Horse") -> SkyblockInvalidEntity(
                baseEntity, "Horse Pet"
            )

            baseEntity is EntityGuardian && armorStand.name.removeColor()
                .matches("^\\d+".toRegex()) -> SkyblockInvalidEntity(
                baseEntity, "Wierd Sea Guardian Ability"
            )

            baseEntity is EntitySlime && armorStand.name == "§f§lCOLLECT!" && LorenzUtils.skyBlockIsland == IslandType.CRIMSON_ISLE -> SkyblockInvalidEntity(baseEntity, "Heavy Pearl") // Change to make Heavy Pearl Highlighter

            else -> null
        }
    }

    private fun armorStandOnlyMobs(baseEntity: EntityLivingBase, armorStand: EntityArmorStand): SkyblockEntity? {
        if (baseEntity !is EntityZombie) return null
        when {
            armorStand.name.endsWith("'s Armadillo") -> return SkyblockInvalidEntity(baseEntity, "Armadillo Pet")
            armorStand.name.endsWith("'s Rat") -> return SkyblockInvalidEntity(baseEntity, "Rat Pet")
        }
        when (armorStand.inventory?.get(4)?.getSkullTexture()) {
            RatSkull -> return SkyblockSpecialMob(baseEntity, armorStand, "Rat")
            HellwispTentacleSkull -> return SkyblockInvalidEntity(baseEntity, "Hellwisp Tentacle")
            RiftEyeSkull1 -> return SkyblockInvalidEntity(baseEntity, "Rift Teleport Eye")
            RiftEyeSkull2 -> return SkyblockInvalidEntity(baseEntity, "Rift Teleport Eye")
        }
        return null
    }

    private val RatSkull =
        "ewogICJ0aW1lc3RhbXAiIDogMTYxODQxOTcwMTc1MywKICAicHJvZmlsZUlkIiA6ICI3MzgyZGRmYmU0ODU0NTVjODI1ZjkwMGY4OGZkMzJmOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJCdUlJZXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYThhYmI0NzFkYjBhYjc4NzAzMDExOTc5ZGM4YjQwNzk4YTk0MWYzYTRkZWMzZWM2MWNiZWVjMmFmOGNmZmU4IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="
    private val HellwispTentacleSkull =
        "ewogICJ0aW1lc3RhbXAiIDogMTY0OTM4MzAyMTQxNiwKICAicHJvZmlsZUlkIiA6ICIzYjgwOTg1YWU4ODY0ZWZlYjA3ODg2MmZkOTRhMTVkOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJLaWVyYW5fVmF4aWxpYW4iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDI3MDQ2Mzg0OTM2MzhiODVjMzhkZDYzZmZkYmUyMjJmZTUzY2ZkNmE1MDk3NzI4NzU2MTE5MzdhZTViNWUyMiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"
    private val RiftEyeSkull1 =
        "ewogICJ0aW1lc3RhbXAiIDogMTY0ODA5MTkzNTcyMiwKICAicHJvZmlsZUlkIiA6ICJhNzdkNmQ2YmFjOWE0NzY3YTFhNzU1NjYxOTllYmY5MiIsCiAgInByb2ZpbGVOYW1lIiA6ICIwOEJFRDUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjI2YmRlNDUwNDljN2I3ZDM0NjA1ZDgwNmEwNjgyOWI2Zjk1NWI4NTZhNTk5MWZkMzNlN2VhYmNlNDRjMDgzNCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"
    private val RiftEyeSkull2 =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTdkYjE5MjNkMDNjNGVmNGU5ZjZlODcyYzVhNmFkMjU3OGIxYWZmMmIyODFmYmMzZmZhNzQ2NmM4MjVmYjkifX19"

    private fun createFarmMobs(baseEntity: EntityLivingBase): SkyblockEntity? = when (baseEntity) {
        is EntityMooshroom -> SkyblockSpecialMob(baseEntity, null, "Farm Mooshroom")
        is EntityCow -> SkyblockSpecialMob(baseEntity, null, "Farm Cow")
        is EntityPig -> SkyblockSpecialMob(baseEntity, null, "Farm Pig")
        is EntityChicken -> SkyblockSpecialMob(baseEntity, null, "Farm Chicken")
        is EntityRabbit -> SkyblockSpecialMob(baseEntity, null, "Farm Rabbit")
        is EntitySheep -> SkyblockSpecialMob(baseEntity, null, "Farm Sheep")
        else -> null
    }

    private fun createBat(baseEntity: EntityLivingBase): SkyblockEntity? = when (baseEntity.maxHealth) {
        // TODO Bat Pinata, Mega Bat
        5_000_000f -> SkyblockSpecialMob(baseEntity, null, "Cinderbat")
        100f -> SkyblockSpecialMob(
            baseEntity, null, if (DungeonAPI.inDungeon()) "Dungeon Secret Bat" else "Private Island Bat"
        )

        20f -> SkyblockProjectileEntity(baseEntity, "Vampire Mask Bat")
        6f -> SkyblockProjectileEntity(baseEntity, "Spirit Scepter Bat") // TODO fix false triggers
        else -> null
    }


    class OwnerShip(val ownerName: String) {
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
        val possibleEntitys = EntityData.currentSkyblockMobsMap.filterKeys {
            it !is EntityArmorStand && it.entityBoundingBox.rayIntersects(
                pos, look
            )
        }
        if (possibleEntitys.isEmpty()) return null
        return possibleEntitys.toList().sortedBy { it.first.distanceTo(pos) }.map { it.second }
    }

}
