package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.data.MobData.Result.Found
import at.hannibal2.skyhanni.data.MobData.Result.Illegal
import at.hannibal2.skyhanni.data.MobData.Result.NotYetFound
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.isNPC
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.LorenzUtils.derpy
import at.hannibal2.skyhanni.utils.LorenzUtils.takeWhileInclusive
import at.hannibal2.skyhanni.utils.MobUtils
import at.hannibal2.skyhanni.utils.MobUtils.getArmorStand
import at.hannibal2.skyhanni.utils.MobUtils.getClosedArmorStand
import at.hannibal2.skyhanni.utils.MobUtils.getNextEntity
import at.hannibal2.skyhanni.utils.MobUtils.isDefaultValue
import at.hannibal2.skyhanni.utils.MobUtils.makeMobResult
import at.hannibal2.skyhanni.utils.MobUtils.takeNonDefault
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityCaveSpider
import net.minecraft.entity.monster.EntityCreeper
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntityGiantZombie
import net.minecraft.entity.monster.EntityGuardian
import net.minecraft.entity.monster.EntityMagmaCube
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.monster.EntityWitch
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.passive.EntityChicken
import net.minecraft.entity.passive.EntityCow
import net.minecraft.entity.passive.EntityHorse
import net.minecraft.entity.passive.EntityMooshroom
import net.minecraft.entity.passive.EntityPig
import net.minecraft.entity.passive.EntityRabbit
import net.minecraft.entity.passive.EntitySheep
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.entity.player.EntityPlayer

object MobFilter {

    enum class DungeonAttribute {
        Flaming, Stormy, Speedy, Fortified, Healthy, Healing, Boomer, Golden, Stealth;

        companion object {
            val toRegexLine = DungeonAttribute.entries.joinToString("|") { it.name }
        }
    }

    val mobNameFilter = "(\\[\\w+([0-9]+)\\] )?(.Corrupted )?(.*) [\\d❤]+".toRegex()
    val slayerNameFilter = "^. (.*) ([IV]+) \\d+".toRegex()
    val bossMobNameFilter = "^. (\\[(.*)\\] )?(.*) ([\\d\\/Mk.,❤]+|█+) .$".toRegex()
    val dungeonNameFilter = "^(?:(✯)\\s)?(?:(${DungeonAttribute.toRegexLine})\\s)?(?:\\[[\\w\\d]+\\]\\s)?(.+)\\s[^\\s]+$".toRegex()

    val summonRegex = "^(\\w+)'s (.*) \\d+".toRegex()
    val summonOwnerRegex = "Spawned by: (.*)".toRegex()
    val dojoFilter = "^(?:(\\d+) pts|(\\w+))$".toRegex()

    fun Entity.isSkyBlockMob(): Boolean = when {
        this !is EntityLivingBase -> false
        this is EntityArmorStand -> false
        this is EntityOtherPlayerMP && this.isRealPlayer() -> false
        this.isDisplayNPC() -> false
        this is EntityWither && (this.entityId < 0 || this.invulTime == 800) -> false
        this is EntityPlayerSP -> false
        else -> true
    }

    fun EntityPlayer.isRealPlayer() = uniqueID != null && uniqueID.version() == 4

    fun EntityLivingBase.isDisplayNPC() = (this is EntityPlayer && isNPC() && when {
        this.name.any { it in '0'..'9' } -> true
        extraDisplayNPCByName.contains(this.name) -> true
        else -> false
    }) || (this is EntityVillager && this.maxHealth == 20.0f) // Villager NPCs in the Village
        || (this is EntityWitch && this.entityId == 253) // Alchemist NPC
        || (this is EntityCow && this.entityId == 175) // Shania NPC
        || (this is EntityCow && this.entityId == 275) // Shania NPC in Rift
        || (this is EntityPlayer && extraDisplayNPCByName.contains(this.name))

    private val extraDisplayNPCByName = setOf(
        "Guy ", // Guy NPC (but only as visitor)
        "§bSam ", // Sam NPC (in Private Island)
        "BarbarianGuard ", // BarbarianGuard NPCs
        "Branchstrutter ", // Those guys in the Trees in the first area in Rift
        "vswiblxdxg", // Mayor Cole
        "anrrtqytsl", // Weaponsmith
    )

    val illegalDisplayNPCArmorStandNames = listOf("§e§lCLICK", "§a§lSTAY SAFE!", "§6§lNEW UPDATE", "§6§lSkyBlock Wiki", "§6Thaumaturgist")

    fun EntityLivingBase.isFarmMob() =
        this is EntityAnimal && (this.maxHealth == 50.0f || this.maxHealth == 20.0f || this.maxHealth == 130.0f) && LorenzUtils.skyBlockIsland != IslandType.PRIVATE_ISLAND


    private fun createSkyblockMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand, extraEntityList: List<EntityLivingBase>): Mob? =
        MobFactories.summon(baseEntity, armorStand, extraEntityList)
            ?: MobFactories.slayer(baseEntity, armorStand, extraEntityList)
            ?: MobFactories.boss(baseEntity, armorStand, extraEntityList)
            ?: if (DungeonAPI.inDungeon()) MobFactories.dungeon(baseEntity, armorStand, extraEntityList) else (MobFactories.basic(baseEntity, armorStand, extraEntityList)
                ?: MobFactories.dojo(baseEntity, armorStand))

    /** baseEntity must have passed the .isSkyBlockMob() function */
    fun createSkyblockEntity(baseEntity: EntityLivingBase): MobData.MobResult {
        noArmorStandMobs(baseEntity)?.also { return it }

        val nextEntity = MobUtils.getNextEntity(baseEntity, 1) as? EntityLivingBase

        exceptions(baseEntity, nextEntity)?.let { return it }

        // Check if Late Stack
        nextEntity?.let { nextEntity ->
            MobData.entityToMob[nextEntity]?.apply { internalAddEntity(baseEntity) }?.also { return MobData.MobResult(Illegal, null) }
        }

        // Stack up the mob
        var caughtSkyblockMob: Mob? = null
        val extraEntityList = generateSequence(nextEntity) { MobUtils.getNextEntity(it, 1) as? EntityLivingBase }.takeWhileInclusive { entity ->
            !(entity is EntityArmorStand && !entity.isDefaultValue()) && MobData.entityToMob[entity]?.also {
                caughtSkyblockMob = it
            }?.run { false } ?: true
        }.toList()
        stackedMobsException(baseEntity, extraEntityList)?.let { return it }

        // If Late Stack add all entities
        caughtSkyblockMob?.apply { internalAddEntity(extraEntityList.dropLast(1)) }?.also { return MobData.MobResult(Illegal, null) }


        val armorStand = extraEntityList.lastOrNull() as? EntityArmorStand
            ?: return MobData.MobResult(NotYetFound, null)

        if (armorStand.isDefaultValue()) return MobData.MobResult(NotYetFound, null)
        return createSkyblockMob(baseEntity, armorStand, extraEntityList.dropLast(1))?.let { MobData.MobResult(Found, it) }
            ?: MobData.MobResult(NotYetFound, null)
    }

    private fun noArmorStandMobs(baseEntity: EntityLivingBase): MobData.MobResult? {
        if (baseEntity is EntityBat) return createBat(baseEntity)?.let { MobData.MobResult(Found, it) }
            ?: MobData.MobResult(NotYetFound, null)
        if (baseEntity.isFarmMob()) return createFarmMobs(baseEntity)?.let { MobData.MobResult(Found, it) }
        if (baseEntity is EntityDragon) return MobData.MobResult(Found, MobFactories.basic(baseEntity, baseEntity.cleanName()))
        if (baseEntity is EntityGiantZombie && baseEntity.name == "Dinnerbone") return MobData.MobResult(Found, MobFactories.projectile(baseEntity, "Giant Sword"))  // Will false trigger if there is another Dinnerbone Giant
        if (baseEntity is EntityCaveSpider) getArmorStand(baseEntity, -1)?.takeIf { it.cleanName().matches(summonOwnerRegex) }
            ?.let { MobData.entityToMob[getNextEntity(baseEntity, -4)]?.internalAddEntity(baseEntity)?.also { return MobData.MobResult(Illegal, null) } }
        return null
    }

    private fun exceptions(baseEntity: EntityLivingBase, nextEntity: EntityLivingBase?): MobData.MobResult? {
        if (DungeonAPI.inDungeon()) {
            if (baseEntity is EntityZombie && nextEntity is EntityArmorStand && (nextEntity.name == "§e﴾ §c§lThe Watcher§r§r §e﴿" || nextEntity.name == "§3§lWatchful Eye§r")) return MobData.MobResult(Found, MobFactories.special(baseEntity, nextEntity.cleanName(), nextEntity))
            if (baseEntity is EntityCaveSpider) return getClosedArmorStand(baseEntity, 2.0).takeNonDefault().makeMobResult { MobFactories.dungeon(baseEntity, it) }
            if (baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && baseEntity.name == "Shadow Assassin") return MobUtils.getClosedArmorStandWithName(baseEntity, 3.0, "Shadow Assassin").makeMobResult { MobFactories.dungeon(baseEntity, it) }
            if (baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && baseEntity.name == "The Professor") return MobUtils.getArmorStand(baseEntity, 9).makeMobResult { MobFactories.boss(baseEntity, it) }
            if (baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && (nextEntity is EntityGiantZombie || nextEntity == null) && baseEntity.name.contains("Livid")) return MobUtils.getClosedArmorStandWithName(baseEntity, 6.0, "﴾ Livid").makeMobResult {
                MobFactories.boss(baseEntity, it, overriddenName = "Real Livid")
            }
        } else when (LorenzUtils.skyBlockIsland) {
            IslandType.PRIVATE_ISLAND -> if (nextEntity !is EntityArmorStand) return MobData.MobResult(Illegal, null) // TODO fix to always include Valid Mobs on Private Island
            IslandType.THE_RIFT -> {
                if (baseEntity is EntitySlime && nextEntity is EntitySlime) return MobData.MobResult(Illegal, null)// Bacte Tentacle
                if (baseEntity is EntitySlime && nextEntity is EntityArmorStand && nextEntity.cleanName().startsWith("﴾ [Lv10] B")) return MobData.MobResult(Found, MobFactories.boss(baseEntity, nextEntity, overriddenName = "Bacte"))
            }

            IslandType.CRIMSON_ISLE -> {
                if (baseEntity is EntitySlime && nextEntity?.name == "§f§lCOLLECT!") return MobData.MobResult(Found, MobFactories.special(baseEntity, "Heavy Pearl"))
                if (baseEntity is EntityPig && nextEntity is EntityPig) return MobData.MobResult(Illegal, null) // Matriarch Tongue
            }

            IslandType.HUB -> {
                if (baseEntity is EntityZombie) { // Rat
                    val from = 3
                    val to = 9
                    generateSequence(from) { it + 1 }.take(to - from + 1).map { i ->
                        MobUtils.getArmorStand(
                            baseEntity, i
                        )
                    }.firstOrNull {
                        it != null && it.distanceTo(baseEntity) < 4.0 && it.inventory?.get(4)?.getSkullTexture() == RatSkull
                    }?.also { return MobData.MobResult(Found, Mob(baseEntity = baseEntity, mobType = Mob.Type.Basic, armorStand = it, name = "Rat")) }
                    if (nextEntity is EntityZombie) return MobData.MobResult(NotYetFound, null)
                }
            }

            IslandType.DWARVEN_MINES -> {
                if (baseEntity is EntityCreeper && baseEntity.baseMaxHealth.derpy() == 1_000_000) return MobData.MobResult(Found, MobFactories.basic(baseEntity, "Ghost"))
            }

            IslandType.CRYSTAL_HOLLOWS -> {
                if (baseEntity is EntityMagmaCube && nextEntity is EntityArmorStand && nextEntity.cleanName() == "[Lv100] Bal ???❤") return MobData.MobResult(Found, Mob(baseEntity, Mob.Type.Boss, nextEntity, "Bal", levelOrTier = 100))
            }

            else -> {}
        }


        val armorStand = nextEntity as? EntityArmorStand ?: return null
        armorStandOnlyMobs(baseEntity, armorStand)?.also { return it }
        return when {
            baseEntity is EntityPig && armorStand.name.endsWith("'s Pig") -> MobData.MobResult(Illegal, null) // Pig Pet

            baseEntity is EntityHorse && armorStand.name.endsWith("'s Skeleton Horse") -> MobData.MobResult(Illegal, null)// Skeleton Horse Pet

            baseEntity is EntityHorse && armorStand.name.endsWith("'s Horse") -> MobData.MobResult(Illegal, null) // Horse Pet

            baseEntity is EntityGuardian && armorStand.cleanName().matches("^\\d+".toRegex()) -> MobData.MobResult(Illegal, null) // Wierd Sea Guardian Ability

            else -> null
        }
    }

    private fun stackedMobsException(baseEntity: EntityLivingBase, extraEntityList: List<EntityLivingBase>): MobData.MobResult? {
        if (DungeonAPI.inDungeon()) {
            if ((baseEntity is EntityEnderman || baseEntity is EntityGiantZombie) && extraEntityList.lastOrNull()?.name == "§e﴾ §c§lLivid§r§r §a7M§c❤ §e﴿") return MobData.MobResult(Illegal, null) // Livid Start Animation
        } else when (LorenzUtils.skyBlockIsland) {
            IslandType.CRIMSON_ISLE -> {}
            else -> {}
        }
        return null
    }

    private fun armorStandOnlyMobs(baseEntity: EntityLivingBase, armorStand: EntityArmorStand): MobData.MobResult? {
        if (baseEntity !is EntityZombie) return null
        when {
            armorStand.name.endsWith("'s Armadillo") -> return MobData.MobResult(Illegal, null) // Armadillo Pet
            armorStand.name.endsWith("'s Rat") -> return MobData.MobResult(Illegal, null) // Rat Pet
        }
        when (armorStand.inventory?.get(4)?.getSkullTexture()) {
            HellwispTentacleSkull -> return MobData.MobResult(Illegal, null) // Hellwisp Tentacle
            RiftEyeSkull1 -> return MobData.MobResult(Found, MobFactories.special(baseEntity, "Rift Teleport Eye", armorStand))
            RiftEyeSkull2 -> return MobData.MobResult(Found, MobFactories.special(baseEntity, "Rift Teleport Eye", armorStand))
        }
        return null
    }

    private val RatSkull = "ewogICJ0aW1lc3RhbXAiIDogMTYxODQxOTcwMTc1MywKICAicHJvZmlsZUlkIiA6ICI3MzgyZGRmYmU0ODU0NTVjODI1ZjkwMGY4OGZkMzJmOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJCdUlJZXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYThhYmI0NzFkYjBhYjc4NzAzMDExOTc5ZGM4YjQwNzk4YTk0MWYzYTRkZWMzZWM2MWNiZWVjMmFmOGNmZmU4IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="
    private val HellwispTentacleSkull = "ewogICJ0aW1lc3RhbXAiIDogMTY0OTM4MzAyMTQxNiwKICAicHJvZmlsZUlkIiA6ICIzYjgwOTg1YWU4ODY0ZWZlYjA3ODg2MmZkOTRhMTVkOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJLaWVyYW5fVmF4aWxpYW4iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDI3MDQ2Mzg0OTM2MzhiODVjMzhkZDYzZmZkYmUyMjJmZTUzY2ZkNmE1MDk3NzI4NzU2MTE5MzdhZTViNWUyMiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"
    private val RiftEyeSkull1 = "ewogICJ0aW1lc3RhbXAiIDogMTY0ODA5MTkzNTcyMiwKICAicHJvZmlsZUlkIiA6ICJhNzdkNmQ2YmFjOWE0NzY3YTFhNzU1NjYxOTllYmY5MiIsCiAgInByb2ZpbGVOYW1lIiA6ICIwOEJFRDUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjI2YmRlNDUwNDljN2I3ZDM0NjA1ZDgwNmEwNjgyOWI2Zjk1NWI4NTZhNTk5MWZkMzNlN2VhYmNlNDRjMDgzNCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"
    private val RiftEyeSkull2 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTdkYjE5MjNkMDNjNGVmNGU5ZjZlODcyYzVhNmFkMjU3OGIxYWZmMmIyODFmYmMzZmZhNzQ2NmM4MjVmYjkifX19"

    private fun createFarmMobs(baseEntity: EntityLivingBase): Mob? = when (baseEntity) {
        is EntityMooshroom -> MobFactories.basic(baseEntity, "Farm Mooshroom")
        is EntityCow -> MobFactories.basic(baseEntity, "Farm Cow")
        is EntityPig -> MobFactories.basic(baseEntity, "Farm Pig")
        is EntityChicken -> MobFactories.basic(baseEntity, "Farm Chicken")
        is EntityRabbit -> MobFactories.basic(baseEntity, "Farm Rabbit")
        is EntitySheep -> MobFactories.basic(baseEntity, "Farm Sheep")
        else -> null
    }

    private fun createBat(baseEntity: EntityLivingBase): Mob? = when (baseEntity.baseMaxHealth.derpy()) {
        // TODO Bat Pinata, Mega Bat
        5_000_000 -> MobFactories.basic(baseEntity, "Cinderbat")
        75_000 -> MobFactories.basic(baseEntity, "Thorn Bat")
        100 -> MobFactories.basic(
            baseEntity, if (DungeonAPI.inDungeon()) "Dungeon Secret Bat" else "Private Island Bat"
        )

        20 -> MobFactories.projectile(baseEntity, "Vampire Mask Bat")
        // 6 -> MobFactories.projectile(baseEntity, "Spirit Scepter Bat") // moved to Packet Event because 6 is default Health of Bats
        else -> null
    }

    fun createDisplayNPC(entity: EntityLivingBase): Boolean =
        MobUtils.getArmorStandByRangeAll(entity, 2.0).firstOrNull { armorStand ->
            !illegalDisplayNPCArmorStandNames.any { armorStand.name.startsWith(it) } && !armorStand.isDefaultValue()
        }?.let { armorStand ->
            MobEvent.Spawn.DisplayNPC(MobFactories.displayNPC(entity, armorStand)).postAndCatch().also { true }
        } ?: false
}
