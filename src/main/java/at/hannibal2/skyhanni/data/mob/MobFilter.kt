package at.hannibal2.skyhanni.data.mob

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.MobData.MobResult
import at.hannibal2.skyhanni.data.mob.MobData.MobResult.Companion.makeMobResult
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.isNPC
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.LorenzUtils.derpy
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.takeWhileInclusive
import at.hannibal2.skyhanni.utils.MobUtils
import at.hannibal2.skyhanni.utils.MobUtils.isDefaultValue
import at.hannibal2.skyhanni.utils.MobUtils.takeNonDefault
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.entity.EntityOtherPlayerMP
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
import net.minecraft.entity.monster.EntityIronGolem
import net.minecraft.entity.monster.EntityMagmaCube
import net.minecraft.entity.monster.EntityPigZombie
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.monster.EntityWitch
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.passive.EntityChicken
import net.minecraft.entity.passive.EntityCow
import net.minecraft.entity.passive.EntityHorse
import net.minecraft.entity.passive.EntityMooshroom
import net.minecraft.entity.passive.EntityOcelot
import net.minecraft.entity.passive.EntityPig
import net.minecraft.entity.passive.EntityRabbit
import net.minecraft.entity.passive.EntitySheep
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.entity.player.EntityPlayer

object MobFilter {

    val mobNameFilter = "(\\[\\w+([0-9]+)\\] )?(.Corrupted )?(.*) [\\d❤]+".toRegex()
    val slayerNameFilter = "^. (.*) ([IV]+) \\d+".toRegex()
    val bossMobNameFilter = "^. (\\[(.*)\\] )?(.*) ([\\d\\/Mk.,❤]+|█+) .$".toRegex()
    val dungeonNameFilter = "^(?:(✯)\\s)?(?:(${DungeonAttribute.toRegexLine})\\s)?(?:\\[[\\w\\d]+\\]\\s)?(.+)\\s[^\\s]+$".toRegex()
    val petCareNameRegex = "^\\[\\w+ (\\d+)\\] (.*)".toRegex()
    val wokeSleepingGolemRegex = "(?:Woke)|(?:Sleeping) Golem".toRegex()

    val summonRegex = "^(\\w+)'s (.*) \\d+".toRegex()
    val summonOwnerRegex = "Spawned by: (.*)".toRegex()
    val dojoFilter = "^(?:(\\d+) pts|(\\w+))$".toRegex()

    private val RatSkull = "ewogICJ0aW1lc3RhbXAiIDogMTYxODQxOTcwMTc1MywKICAicHJvZmlsZUlkIiA6ICI3MzgyZGRmYmU0ODU0NTVjODI1ZjkwMGY4OGZkMzJmOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJCdUlJZXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYThhYmI0NzFkYjBhYjc4NzAzMDExOTc5ZGM4YjQwNzk4YTk0MWYzYTRkZWMzZWM2MWNiZWVjMmFmOGNmZmU4IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="
    private val HellwispTentacleSkull = "ewogICJ0aW1lc3RhbXAiIDogMTY0OTM4MzAyMTQxNiwKICAicHJvZmlsZUlkIiA6ICIzYjgwOTg1YWU4ODY0ZWZlYjA3ODg2MmZkOTRhMTVkOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJLaWVyYW5fVmF4aWxpYW4iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDI3MDQ2Mzg0OTM2MzhiODVjMzhkZDYzZmZkYmUyMjJmZTUzY2ZkNmE1MDk3NzI4NzU2MTE5MzdhZTViNWUyMiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"
    private val RiftEyeSkull1 = "ewogICJ0aW1lc3RhbXAiIDogMTY0ODA5MTkzNTcyMiwKICAicHJvZmlsZUlkIiA6ICJhNzdkNmQ2YmFjOWE0NzY3YTFhNzU1NjYxOTllYmY5MiIsCiAgInByb2ZpbGVOYW1lIiA6ICIwOEJFRDUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjI2YmRlNDUwNDljN2I3ZDM0NjA1ZDgwNmEwNjgyOWI2Zjk1NWI4NTZhNTk5MWZkMzNlN2VhYmNlNDRjMDgzNCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"
    private val RiftEyeSkull2 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTdkYjE5MjNkMDNjNGVmNGU5ZjZlODcyYzVhNmFkMjU3OGIxYWZmMmIyODFmYmMzZmZhNzQ2NmM4MjVmYjkifX19"
    private val NPCTurdSkull = "ewogICJ0aW1lc3RhbXAiIDogMTYzOTUxMjYxNzc5MywKICAicHJvZmlsZUlkIiA6ICIwZjczMDA3NjEyNGU0NGM3YWYxMTE1NDY5YzQ5OTY3OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJPcmVfTWluZXIxMjMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjM2MzBkOWIwMjA4OGVhMTkyNGE4NzIyNDJhYmM3NWI2MjYyYzJhY2E5MmFlY2Y4NzE0YTU3YTQxZWVhMGI5ZCIKICAgIH0KICB9Cn0="

    const val minionMobPrefix = "Minion Mob "

    enum class DungeonAttribute {
        Flaming, Stormy, Speedy, Fortified, Healthy, Healing, Boomer, Golden, Stealth;

        companion object {
            val toRegexLine = DungeonAttribute.entries.joinToString("|") { it.name }
        }
    }

    private val extraDisplayNPCByName = setOf(
        "Guy ", // Guy NPC (but only as visitor)
        "vswiblxdxg", // Mayor Cole
        "anrrtqytsl", // Weaponsmith
    )

    private val listOfClickArmorStand = setOf(
        "§e§lCLICK",
        "§6§lSEASONAL SKINS",
    )

    fun Entity.isSkyBlockMob(): Boolean = when {
        this !is EntityLivingBase -> false
        this is EntityArmorStand -> false
        this is EntityPlayer && this.isRealPlayer() -> false
        this.isDisplayNPC() -> false
        this is EntityWither && this.entityId < 0 -> false
        else -> true
    }

    fun EntityPlayer.isRealPlayer() = uniqueID != null && uniqueID.version() == 4

    fun EntityLivingBase.isDisplayNPC() = (this is EntityPlayer && isNPC() && when {
        this.name.startsWith('§') -> true
        this.name.any { it in '0'..'9' } -> true
        extraDisplayNPCByName.contains(this.name) -> true
        else -> false
    }) || (this is EntityVillager && this.maxHealth == 20.0f) // Villager NPCs in the Village
        || (this is EntityWitch && this.entityId <= 500) // Alchemist NPC
        || (this is EntityCow && this.entityId <= 500) // Shania NPC (in Rift and Outside)

    internal fun createDisplayNPC(entity: EntityLivingBase): Boolean =
        MobUtils.getArmorStandByRangeAll(entity, 1.5).firstOrNull { armorStand ->
            listOfClickArmorStand.contains(armorStand.name)
        }?.let { MobUtils.getArmorStand(it, -1) }?.let { armorStand ->
            MobEvent.Spawn.DisplayNPC(MobFactories.displayNPC(entity, armorStand)).postAndCatch()
            true
        } ?: false


    /** baseEntity must have passed the .isSkyBlockMob() function */
    internal fun createSkyblockEntity(baseEntity: EntityLivingBase): MobResult {
        val nextEntity = MobUtils.getNextEntity(baseEntity, 1) as? EntityLivingBase

        exceptions(baseEntity, nextEntity)?.let { return it }

        // Check if Late Stack
        nextEntity?.let { nextEntity ->
            MobData.entityToMob[nextEntity]?.apply { internalAddEntity(baseEntity) }?.also { return MobResult.illegal }
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
        caughtSkyblockMob?.apply { internalAddEntity(extraEntityList.dropLast(1)) }?.also { return MobResult.illegal }


        val armorStand = extraEntityList.lastOrNull() as? EntityArmorStand ?: return MobResult.notYetFound

        if (armorStand.isDefaultValue()) return MobResult.notYetFound
        return createSkyblockMob(baseEntity, armorStand, extraEntityList.dropLast(1))?.let { MobResult.found(it) }
            ?: MobResult.notYetFound
    }

    private fun createSkyblockMob(baseEntity: EntityLivingBase, armorStand: EntityArmorStand, extraEntityList: List<EntityLivingBase>): Mob? =
        MobFactories.summon(baseEntity, armorStand, extraEntityList)
            ?: MobFactories.slayer(baseEntity, armorStand, extraEntityList)
            ?: MobFactories.boss(baseEntity, armorStand, extraEntityList)
            ?: if (DungeonAPI.inDungeon()) MobFactories.dungeon(baseEntity, armorStand, extraEntityList) else (MobFactories.basic(baseEntity, armorStand, extraEntityList)
                ?: MobFactories.dojo(baseEntity, armorStand))

    private fun noArmorStandMobs(baseEntity: EntityLivingBase): MobResult? = when {
        baseEntity is EntityBat -> createBat(baseEntity)?.let { MobResult.found(it) } ?: MobResult.notYetFound

        baseEntity.isFarmMob() -> createFarmMobs(baseEntity)?.let { MobResult.found(it) }
        baseEntity is EntityDragon -> MobResult.found(MobFactories.basic(baseEntity, baseEntity.cleanName()))
        baseEntity is EntityGiantZombie && baseEntity.name == "Dinnerbone" -> MobResult.found(MobFactories.projectile(baseEntity, "Giant Sword")) // Will false trigger if there is another Dinnerbone Giant
        baseEntity is EntityCaveSpider -> MobUtils.getArmorStand(baseEntity, -1)?.takeIf { it.cleanName().matches(summonOwnerRegex) }?.let { MobData.entityToMob[MobUtils.getNextEntity(baseEntity, -4)]?.internalAddEntity(baseEntity)?.let { MobResult.illegal } }

        baseEntity is EntityWither && baseEntity.invulTime == 800 -> MobResult.found(MobFactories.special(baseEntity, "Mini Wither"))
        else -> null
    }


    private fun exceptions(baseEntity: EntityLivingBase, nextEntity: EntityLivingBase?): MobResult? {
        noArmorStandMobs(baseEntity)?.also { return it }
        val armorStand = nextEntity as? EntityArmorStand
        islandSpecificExceptions(baseEntity, armorStand, nextEntity)?.also { return it }

        if (armorStand == null) return null
        armorStandOnlyMobs(baseEntity, armorStand)?.also { return it }
        return when {
            baseEntity is EntityPig && armorStand.name.endsWith("'s Pig") -> MobResult.illegal // Pig Pet

            baseEntity is EntityHorse && armorStand.name.endsWith("'s Skeleton Horse") -> MobResult.illegal// Skeleton Horse Pet

            baseEntity is EntityHorse && armorStand.name.endsWith("'s Horse") -> MobResult.illegal // Horse Pet

            baseEntity is EntityGuardian && armorStand.cleanName().matches("^\\d+".toRegex()) -> MobResult.illegal // Wierd Sea Guardian Ability

            else -> null
        }
    }

    private fun islandSpecificExceptions(baseEntity: EntityLivingBase, armorStand: EntityArmorStand?, nextEntity: EntityLivingBase?): MobResult? {
        return if (DungeonAPI.inDungeon()) {
            when {
                baseEntity is EntityZombie && armorStand != null && (armorStand.name == "§e﴾ §c§lThe Watcher§r§r §e﴿" || armorStand.name == "§3§lWatchful Eye§r") -> MobResult.found(MobFactories.special(baseEntity, armorStand.cleanName(), armorStand))
                baseEntity is EntityCaveSpider -> MobUtils.getClosedArmorStand(baseEntity, 2.0).takeNonDefault().makeMobResult { MobFactories.dungeon(baseEntity, it) }
                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && baseEntity.name == "Shadow Assassin" -> MobUtils.getClosedArmorStandWithName(baseEntity, 3.0, "Shadow Assassin").makeMobResult { MobFactories.dungeon(baseEntity, it) }
                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && baseEntity.name == "The Professor" -> MobUtils.getArmorStand(baseEntity, 9).makeMobResult { MobFactories.boss(baseEntity, it) }
                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && (nextEntity is EntityGiantZombie || nextEntity == null) && baseEntity.name.contains("Livid") -> MobUtils.getClosedArmorStandWithName(baseEntity, 6.0, "﴾ Livid").makeMobResult { MobFactories.boss(baseEntity, it, overriddenName = "Real Livid") }
                baseEntity is EntityIronGolem && wokeSleepingGolemRegex.matches(
                    armorStand?.name ?: ""
                ) -> MobResult.found(Mob(baseEntity, Mob.Type.Dungeon, armorStand, "Woke Golem")) // Consistency fix
                else -> null
            }
        } else when (LorenzUtils.skyBlockIsland) {
            IslandType.PRIVATE_ISLAND -> when {
                armorStand?.isDefaultValue() != false -> if (baseEntity.getLorenzVec().distanceChebyshevIgnoreY(LocationUtils.playerLocation()) < 15.0) MobResult.found(MobFactories.minionMob(baseEntity)) else MobResult.notYetFound // TODO fix to always include Valid Mobs on Private Island
                else -> null
            }

            IslandType.THE_RIFT -> when {
                baseEntity is EntitySlime && nextEntity is EntitySlime -> MobResult.illegal// Bacte Tentacle
                baseEntity is EntitySlime && armorStand != null && armorStand.cleanName().startsWith("﴾ [Lv10] B") -> MobResult.found(Mob(baseEntity, Mob.Type.Boss, armorStand, name = "Bacte"))
                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && baseEntity.name == "Branchstrutter " -> MobResult.found(Mob(baseEntity, Mob.Type.DisplayNPC, name = "Branchstrutter"))
                else -> null
            }

            IslandType.CRIMSON_ISLE -> when {
                baseEntity is EntitySlime && armorStand?.name == "§f§lCOLLECT!" -> MobResult.found(MobFactories.special(baseEntity, "Heavy Pearl"))
                baseEntity is EntityPig && nextEntity is EntityPig -> MobResult.illegal // Matriarch Tongue
                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && baseEntity.name == "BarbarianGuard " -> MobResult.found(Mob(baseEntity, Mob.Type.DisplayNPC, name = "Barbarian Guard"))
                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && baseEntity.name == "MageGuard " -> MobResult.found(Mob(baseEntity, Mob.Type.DisplayNPC, name = "Mage Guard"))
                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && baseEntity.name == "Mage Outlaw" -> MobResult.found(Mob(baseEntity, Mob.Type.Boss, armorStand, name = "Mage Outlaw")) // fix for wierd name
                baseEntity is EntityPigZombie && baseEntity.inventory?.get(4)?.getSkullTexture() == NPCTurdSkull -> MobResult.found(Mob(baseEntity, Mob.Type.DisplayNPC, name = "Turd"))
                baseEntity is EntityOcelot -> if (createDisplayNPC(baseEntity)) MobResult.illegal else MobResult.notYetFound // Maybe a problem in the future
                else -> null
            }

            IslandType.DEEP_CAVERNS -> when {
                baseEntity is EntityCreeper && baseEntity.baseMaxHealth.derpy() == 120 -> MobResult.found(Mob(baseEntity, Mob.Type.Basic, name = "Sneaky Creeper", levelOrTier = 3))
                else -> null
            }

            IslandType.DWARVEN_MINES -> when {
                baseEntity is EntityCreeper && baseEntity.baseMaxHealth.derpy() == 1_000_000 -> MobResult.found(MobFactories.basic(baseEntity, "Ghost"))
                else -> null
            }

            IslandType.CRYSTAL_HOLLOWS -> when {
                baseEntity is EntityMagmaCube && armorStand != null && armorStand.cleanName() == "[Lv100] Bal ???❤" -> MobResult.found(Mob(baseEntity, Mob.Type.Boss, armorStand, "Bal", levelOrTier = 100))
                else -> null
            }

            IslandType.HUB -> when {
                baseEntity is EntityOcelot && armorStand?.isDefaultValue() == false && armorStand.name.startsWith("§8[§7Lv155§8] §cAzrael§r") -> MobUtils.getArmorStand(baseEntity, 2).makeMobResult { MobFactories.basic(baseEntity, it) }
                baseEntity is EntityOcelot && nextEntity is EntityOcelot -> MobUtils.getArmorStand(baseEntity, 2).makeMobResult { MobFactories.basic(baseEntity, it) }
                baseEntity is EntityOtherPlayerMP && (baseEntity.name == "Minos Champion" || baseEntity.name == "Minos Inquisitor") && armorStand != null -> MobUtils.getArmorStand(baseEntity, 2).makeMobResult { MobFactories.basic(baseEntity, it, listOf(armorStand)) }
                baseEntity is EntityZombie && armorStand?.isDefaultValue() == true && MobUtils.getNextEntity(baseEntity, 4)?.name?.startsWith("§e") == true -> petCareHandler(baseEntity)
                baseEntity is EntityZombie && armorStand != null && !armorStand.isDefaultValue() -> null // Impossible Rat
                baseEntity is EntityZombie -> ratHandler(baseEntity, nextEntity) // Possible Rat
                else -> null
            }

            IslandType.GARDEN -> when {
                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() -> MobResult.found(Mob(baseEntity, Mob.Type.DisplayNPC, name = baseEntity.cleanName()))
                else -> null
            }

            else -> null
        }
    }

    private fun petCareHandler(baseEntity: EntityLivingBase): MobResult {
        val extraEntityList = listOf(1, 2, 3, 4).mapNotNull { MobUtils.getArmorStand(baseEntity, it) }
        if (extraEntityList.size != 4) return MobResult.notYetFound
        return petCareNameRegex.find(extraEntityList[1].cleanName())?.groupValues?.let {
            MobResult.found(
                Mob(
                    baseEntity, Mob.Type.Special, armorStand = extraEntityList[1], name = it[2], additionalEntities = extraEntityList, levelOrTier = it[1].toInt()
                ),
            )
        } ?: MobResult.somethingWentWrong
    }


    private fun stackedMobsException(baseEntity: EntityLivingBase, extraEntityList: List<EntityLivingBase>): MobResult? =
        if (DungeonAPI.inDungeon()) {
            when {
                (baseEntity is EntityEnderman || baseEntity is EntityGiantZombie) && extraEntityList.lastOrNull()?.name == "§e﴾ §c§lLivid§r§r §a7M§c❤ §e﴿" -> MobResult.illegal // Livid Start Animation
                else -> null
            }
        } else when (LorenzUtils.skyBlockIsland) {
            IslandType.CRIMSON_ISLE -> when {
                else -> null
            }

            else -> null
        }


    private fun armorStandOnlyMobs(baseEntity: EntityLivingBase, armorStand: EntityArmorStand): MobResult? {
        if (baseEntity !is EntityZombie) return null
        when {
            armorStand.name.endsWith("'s Armadillo") -> return MobResult.illegal // Armadillo Pet
            armorStand.name.endsWith("'s Rat") -> return MobResult.illegal // Rat Pet
        }
        when (armorStand.inventory?.get(4)?.getSkullTexture()) {
            HellwispTentacleSkull -> return MobResult.illegal // Hellwisp Tentacle
            RiftEyeSkull1 -> return MobResult.found(MobFactories.special(baseEntity, "Rift Teleport Eye", armorStand))
            RiftEyeSkull2 -> return MobResult.found(MobFactories.special(baseEntity, "Rift Teleport Eye", armorStand))
        }
        return null
    }

    fun EntityLivingBase.isFarmMob() =
        this is EntityAnimal && this.baseMaxHealth.derpy().let { it == 50 || it == 20 || it == 130 } && LorenzUtils.skyBlockIsland != IslandType.PRIVATE_ISLAND

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
        5_000_000 -> MobFactories.basic(baseEntity, "Cinderbat")
        75_000 -> MobFactories.basic(baseEntity, "Thorn Bat")
        100 -> MobFactories.basic(
            baseEntity, if (DungeonAPI.inDungeon()) "Dungeon Secret Bat" else if (IslandType.PRIVATE_ISLAND.isInIsland()) "Private Island Bat" else "Mega Bat"
        )

        20 -> MobFactories.projectile(baseEntity, "Vampire Mask Bat")
        // 6 -> MobFactories.projectile(baseEntity, "Spirit Scepter Bat") // moved to Packet Event because 6 is default Health of Bats
        5 -> MobFactories.special(baseEntity, "Bat Pinata")
        else -> null
    }

    private fun ratHandler(baseEntity: EntityZombie, nextEntity: EntityLivingBase?): MobResult? =
        generateSequence(ratSearchStart) { it + 1 }.take(ratSearchUpTo - ratSearchStart + 1).map { i ->
            MobUtils.getArmorStand(
                baseEntity, i
            )
        }.firstOrNull {
            it != null && it.distanceTo(baseEntity) < 4.0 && it.inventory?.get(4)?.getSkullTexture() == RatSkull
        }?.let { MobResult.found(Mob(baseEntity = baseEntity, mobType = Mob.Type.Basic, armorStand = it, name = "Rat")) }
            ?: if (nextEntity is EntityZombie) MobResult.notYetFound else null

    private const val ratSearchStart = 3
    private const val ratSearchUpTo = 11
}
