package at.hannibal2.skyhanni.data.mob

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.MobData.MobResult
import at.hannibal2.skyhanni.data.mob.MobData.MobResult.Companion.makeMobResult
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.utils.CollectionUtils.takeWhileInclusive
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.isNPC
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.LorenzUtils.derpy
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.MobUtils
import at.hannibal2.skyhanni.utils.MobUtils.isDefaultValue
import at.hannibal2.skyhanni.utils.MobUtils.takeNonDefault
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
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
import net.minecraft.entity.monster.EntitySnowman
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

@Suppress("RegExpRedundantEscape")
object MobFilter {

    private val repoGroup = RepoPattern.group("mob.detection")

    val mobNameFilter by repoGroup.pattern(
        "filter.basic",
        "(?:\\[\\w+(?<level>\\d+)\\] )?(?<corrupted>.Corrupted )?(?<name>.*) [\\d❤]+"
    )
    val slayerNameFilter by repoGroup.pattern("filter.slayer", "^. (?<name>.*) (?<tier>[IV]+) \\d+.*")
    val bossMobNameFilter by repoGroup.pattern(
        "filter.boss",
        "^. (?:\\[\\w+(?<level>\\d+)\\] )?(?<name>.*) (?:[\\d\\/Mk.,❤]+|█+) .$"
    )
    val dungeonNameFilter by repoGroup.pattern(
        "filter.dungeon",
        "^(?:(?<star>✯)\\s)?(?:(?<attribute>${DungeonAttribute.toRegexLine})\\s)?(?:\\[[\\w\\d]+\\]\\s)?(?<name>.+)\\s[^\\s]+$"
    )
    val summonFilter by repoGroup.pattern("filter.summon", "^(?<owner>\\w+)'s (?<name>.*) \\d+.*")
    val dojoFilter by repoGroup.pattern("filter.dojo", "^(?:(?<points>\\d+) pts|(?<empty>\\w+))$")
    val jerryPattern by repoGroup.pattern(
        "jerry",
        "(?:\\[\\w+(?<level>\\d+)\\] )?(?<owner>\\w+)'s (?<name>\\w+ Jerry) \\d+ Hits"
    )

    val petCareNamePattern by repoGroup.pattern("pattern.petcare", "^\\[\\w+ (?<level>\\d+)\\] (?<name>.*)")
    val wokeSleepingGolemPattern by repoGroup.pattern("pattern.dungeon.woke.golem", "(?:§c§lWoke|§5§lSleeping) Golem§r")
    val jerryMagmaCubePattern by repoGroup.pattern(
        "pattern.jerry.magma.cube",
        "§c(?:Cubie|Maggie|Cubert|Cübe|Cubette|Magmalene|Lucky 7|8ball|Mega Cube|Super Cube) §a\\d+§8\\/§a\\d+§c❤"
    )
    val summonOwnerPattern by repoGroup.pattern("pattern.summon.owner", ".*Spawned by: (?<name>.*).*")

    private const val RAT_SKULL =
        "ewogICJ0aW1lc3RhbXAiIDogMTYxODQxOTcwMTc1MywKICAicHJvZmlsZUlkIiA6ICI3MzgyZGRmYmU0ODU0NTVjODI1ZjkwMGY4OGZkMzJmOCIsCiAgInByb2ZpbGVOYW1lIiA6ICJCdUlJZXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYThhYmI0NzFkYjBhYjc4NzAzMDExOTc5ZGM4YjQwNzk4YTk0MWYzYTRkZWMzZWM2MWNiZWVjMmFmOGNmZmU4IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0="
    private const val HELLWISP_TENTACLE_SKULL =
        "ewogICJ0aW1lc3RhbXAiIDogMTY0OTM4MzAyMTQxNiwKICAicHJvZmlsZUlkIiA6ICIzYjgwOTg1YWU4ODY0ZWZlYjA3ODg2MmZkOTRhMTVkOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJLaWVyYW5fVmF4aWxpYW4iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDI3MDQ2Mzg0OTM2MzhiODVjMzhkZDYzZmZkYmUyMjJmZTUzY2ZkNmE1MDk3NzI4NzU2MTE5MzdhZTViNWUyMiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"
    private const val RIFT_EYE_SKULL1 =
        "ewogICJ0aW1lc3RhbXAiIDogMTY0ODA5MTkzNTcyMiwKICAicHJvZmlsZUlkIiA6ICJhNzdkNmQ2YmFjOWE0NzY3YTFhNzU1NjYxOTllYmY5MiIsCiAgInByb2ZpbGVOYW1lIiA6ICIwOEJFRDUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjI2YmRlNDUwNDljN2I3ZDM0NjA1ZDgwNmEwNjgyOWI2Zjk1NWI4NTZhNTk5MWZkMzNlN2VhYmNlNDRjMDgzNCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"
    private const val RIFT_EYE_SKULL2 =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTdkYjE5MjNkMDNjNGVmNGU5ZjZlODcyYzVhNmFkMjU3OGIxYWZmMmIyODFmYmMzZmZhNzQ2NmM4MjVmYjkifX19"
    private const val NPC_TURD_SKULL =
        "ewogICJ0aW1lc3RhbXAiIDogMTYzOTUxMjYxNzc5MywKICAicHJvZmlsZUlkIiA6ICIwZjczMDA3NjEyNGU0NGM3YWYxMTE1NDY5YzQ5OTY3OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJPcmVfTWluZXIxMjMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjM2MzBkOWIwMjA4OGVhMTkyNGE4NzIyNDJhYmM3NWI2MjYyYzJhY2E5MmFlY2Y4NzE0YTU3YTQxZWVhMGI5ZCIKICAgIH0KICB9Cn0="

    const val MINION_MOB_PREFIX = "Minion Mob "

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

    private val displayNPCCompressedNamePattern by repoGroup.pattern("displaynpc.name", "[a-z0-9]{10}")

    private fun displayNPCNameCheck(name: String) = name.startsWith('§')
        || displayNPCCompressedNamePattern.matches(name)
        || extraDisplayNPCByName.contains(name)

    private val listOfClickArmorStand = setOf(
        "§e§lCLICK",
        "§6§lSEASONAL SKINS",
        "§e§lGATE KEEPER",
        "§e§lBLACKSMITH",
        "§e§lSHOP",
        "§e§lTREASURES"
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

    fun EntityLivingBase.isDisplayNPC() = (this is EntityPlayer && isNPC() && displayNPCNameCheck(this.name))
        || (this is EntityVillager && this.maxHealth == 20.0f) // Villager NPCs in the Village
        || (this is EntityWitch && this.entityId <= 500) // Alchemist NPC
        || (this is EntityCow && this.entityId <= 500) // Shania NPC (in Rift and Outside)
        || (this is EntitySnowman && this.entityId <= 500) // Sherry NPC (in Jerry Island)

    internal fun createDisplayNPC(entity: EntityLivingBase): Boolean {
        val clickArmorStand = MobUtils.getArmorStandByRangeAll(entity, 1.5).firstOrNull { armorStand ->
            listOfClickArmorStand.contains(armorStand.name)
        } ?: return false
        val armorStand = MobUtils.getArmorStand(clickArmorStand, -1) ?: return false
        MobEvent.Spawn.DisplayNPC(MobFactories.displayNPC(entity, armorStand, clickArmorStand)).postAndCatch()
        return true
    }

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
        val extraEntityList = generateSequence(nextEntity) {
            MobUtils.getNextEntity(
                it,
                1
            ) as? EntityLivingBase
        }.takeWhileInclusive { entity ->
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

    private fun createSkyblockMob(
        baseEntity: EntityLivingBase,
        armorStand: EntityArmorStand,
        extraEntityList: List<EntityLivingBase>
    ): Mob? =
        MobFactories.summon(baseEntity, armorStand, extraEntityList)
            ?: MobFactories.slayer(baseEntity, armorStand, extraEntityList)
            ?: MobFactories.boss(baseEntity, armorStand, extraEntityList)
            ?: if (DungeonAPI.inDungeon()) MobFactories.dungeon(
                baseEntity,
                armorStand,
                extraEntityList
            ) else (MobFactories.basic(baseEntity, armorStand, extraEntityList)
                ?: MobFactories.dojo(baseEntity, armorStand))

    private fun noArmorStandMobs(baseEntity: EntityLivingBase): MobResult? = when {
        baseEntity is EntityBat -> createBat(baseEntity)

        baseEntity.isFarmMob() -> createFarmMobs(baseEntity)?.let { MobResult.found(it) }
        baseEntity is EntityDragon -> MobResult.found(MobFactories.basic(baseEntity, baseEntity.cleanName()))
        baseEntity is EntityGiantZombie && baseEntity.name == "Dinnerbone" -> MobResult.found(
            MobFactories.projectile(
                baseEntity,
                "Giant Sword"
            )
        ) // Will false trigger if there is another Dinnerbone Giant
        baseEntity is EntityCaveSpider -> MobUtils.getArmorStand(baseEntity, -1)
            ?.takeIf { summonOwnerPattern.matches(it.cleanName()) }?.let {
                MobData.entityToMob[MobUtils.getNextEntity(baseEntity, -4)]?.internalAddEntity(baseEntity)
                    ?.let { MobResult.illegal }
            }

        baseEntity is EntityWither && baseEntity.invulTime == 800 -> MobResult.found(
            MobFactories.special(
                baseEntity,
                "Mini Wither"
            )
        )

        else -> null
    }

    private fun exceptions(baseEntity: EntityLivingBase, nextEntity: EntityLivingBase?): MobResult? {
        noArmorStandMobs(baseEntity)?.also { return it }
        val armorStand = nextEntity as? EntityArmorStand
        islandSpecificExceptions(baseEntity, armorStand, nextEntity)?.also { return it }

        if (armorStand == null) return null
        armorStandOnlyMobs(baseEntity, armorStand)?.also { return it }
        jerryPattern.matchMatcher(armorStand.cleanName()) {
            val level = this.group("level")?.toInt() ?: -1
            val owner = this.group("owner") ?: return@matchMatcher
            val name = this.group("name") ?: return@matchMatcher
            return MobResult.found(
                Mob(
                    baseEntity,
                    Mob.Type.BASIC,
                    armorStand,
                    name = name,
                    ownerName = owner,
                    levelOrTier = level
                )
            )
        }
        return when {
            baseEntity is EntityPig && armorStand.name.endsWith("'s Pig") -> MobResult.illegal // Pig Pet

            baseEntity is EntityHorse && armorStand.name.endsWith("'s Skeleton Horse") -> MobResult.illegal// Skeleton Horse Pet

            baseEntity is EntityHorse && armorStand.name.endsWith("'s Horse") -> MobResult.illegal // Horse Pet

            baseEntity is EntityGuardian && armorStand.cleanName()
                .matches("^\\d+".toRegex()) -> MobResult.illegal // Wierd Sea Guardian Ability

            else -> null
        }
    }

    private fun islandSpecificExceptions(
        baseEntity: EntityLivingBase,
        armorStand: EntityArmorStand?,
        nextEntity: EntityLivingBase?
    ): MobResult? {
        return if (DungeonAPI.inDungeon()) {
            when {
                baseEntity is EntityZombie && armorStand != null && (armorStand.name == "§e﴾ §c§lThe Watcher§r§r §e﴿" || armorStand.name == "§3§lWatchful Eye§r") -> MobResult.found(
                    MobFactories.special(baseEntity, armorStand.cleanName(), armorStand)
                )

                baseEntity is EntityCaveSpider -> MobUtils.getClosedArmorStand(baseEntity, 2.0).takeNonDefault()
                    .makeMobResult { MobFactories.dungeon(baseEntity, it) }

                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && baseEntity.name == "Shadow Assassin" -> MobUtils.getClosedArmorStandWithName(
                    baseEntity,
                    3.0,
                    "Shadow Assassin"
                ).makeMobResult { MobFactories.dungeon(baseEntity, it) }

                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && baseEntity.name == "The Professor" -> MobUtils.getArmorStand(
                    baseEntity,
                    9
                ).makeMobResult { MobFactories.boss(baseEntity, it) }

                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && (nextEntity is EntityGiantZombie || nextEntity == null) && baseEntity.name.contains(
                    "Livid"
                ) -> MobUtils.getClosedArmorStandWithName(baseEntity, 6.0, "﴾ Livid")
                    .makeMobResult { MobFactories.boss(baseEntity, it, overriddenName = "Real Livid") }

                baseEntity is EntityIronGolem && wokeSleepingGolemPattern.matches(
                    armorStand?.name ?: ""
                ) -> MobResult.found(Mob(baseEntity, Mob.Type.DUNGEON, armorStand, "Sleeping Golem")) // Consistency fix
                else -> null
            }
        } else when (LorenzUtils.skyBlockIsland) {
            IslandType.PRIVATE_ISLAND -> when {
                armorStand?.isDefaultValue() != false -> if (baseEntity.getLorenzVec()
                        .distanceChebyshevIgnoreY(LocationUtils.playerLocation()) < 15.0
                ) MobResult.found(MobFactories.minionMob(baseEntity)) else MobResult.notYetFound // TODO fix to always include Valid Mobs on Private Island
                else -> null
            }

            IslandType.THE_RIFT -> when {
                baseEntity is EntitySlime && nextEntity is EntitySlime -> MobResult.illegal// Bacte Tentacle
                baseEntity is EntitySlime && armorStand != null && armorStand.cleanName()
                    .startsWith("﴾ [Lv10] B") -> MobResult.found(
                    Mob(
                        baseEntity,
                        Mob.Type.BOSS,
                        armorStand,
                        name = "Bacte"
                    )
                )

                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && baseEntity.name == "Branchstrutter " -> MobResult.found(
                    Mob(baseEntity, Mob.Type.DISPLAY_NPC, name = "Branchstrutter")
                )

                else -> null
            }

            IslandType.CRIMSON_ISLE -> when {
                baseEntity is EntitySlime && armorStand?.name == "§f§lCOLLECT!" -> MobResult.found(
                    MobFactories.special(
                        baseEntity,
                        "Heavy Pearl"
                    )
                )

                baseEntity is EntityPig && nextEntity is EntityPig -> MobResult.illegal // Matriarch Tongue
                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && baseEntity.name == "BarbarianGuard " -> MobResult.found(
                    Mob(baseEntity, Mob.Type.DISPLAY_NPC, name = "Barbarian Guard")
                )

                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && baseEntity.name == "MageGuard " -> MobResult.found(
                    Mob(baseEntity, Mob.Type.DISPLAY_NPC, name = "Mage Guard")
                )

                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && baseEntity.name == "Mage Outlaw" -> MobResult.found(
                    Mob(baseEntity, Mob.Type.BOSS, armorStand, name = "Mage Outlaw")
                ) // fix for wierd name
                baseEntity is EntityPigZombie && baseEntity.inventory?.get(4)
                    ?.getSkullTexture() == NPC_TURD_SKULL -> MobResult.found(
                    Mob(
                        baseEntity,
                        Mob.Type.DISPLAY_NPC,
                        name = "Turd"
                    )
                )

                baseEntity is EntityOcelot -> if (createDisplayNPC(baseEntity)) MobResult.illegal else MobResult.notYetFound // Maybe a problem in the future
                else -> null
            }

            IslandType.DEEP_CAVERNS -> when {
                baseEntity is EntityCreeper && baseEntity.baseMaxHealth.derpy() == 120 -> MobResult.found(
                    Mob(
                        baseEntity,
                        Mob.Type.BASIC,
                        name = "Sneaky Creeper",
                        levelOrTier = 3
                    )
                )

                else -> null
            }

            IslandType.DWARVEN_MINES -> when {
                baseEntity is EntityCreeper && baseEntity.baseMaxHealth.derpy() == 1_000_000 -> MobResult.found(
                    MobFactories.basic(baseEntity, "Ghost")
                )

                else -> null
            }

            IslandType.CRYSTAL_HOLLOWS -> when {
                baseEntity is EntityMagmaCube && armorStand != null && armorStand.cleanName() == "[Lv100] Bal ???❤" -> MobResult.found(
                    Mob(baseEntity, Mob.Type.BOSS, armorStand, "Bal", levelOrTier = 100)
                )

                else -> null
            }

            IslandType.HUB -> when {
                baseEntity is EntityOcelot && armorStand?.isDefaultValue() == false && armorStand.name.startsWith("§8[§7Lv155§8] §cAzrael§r") -> MobUtils.getArmorStand(
                    baseEntity,
                    1
                ).makeMobResult { MobFactories.basic(baseEntity, it) }

                baseEntity is EntityOcelot && (nextEntity is EntityOcelot || nextEntity == null) -> MobUtils.getArmorStand(
                    baseEntity,
                    3
                ).makeMobResult { MobFactories.basic(baseEntity, it) }

                baseEntity is EntityOtherPlayerMP && (baseEntity.name == "Minos Champion" || baseEntity.name == "Minos Inquisitor" || baseEntity.name == "Minotaur ") && armorStand != null -> MobUtils.getArmorStand(
                    baseEntity,
                    2
                ).makeMobResult { MobFactories.basic(baseEntity, it, listOf(armorStand)) }

                baseEntity is EntityZombie && armorStand?.isDefaultValue() == true && MobUtils.getNextEntity(
                    baseEntity,
                    4
                )?.name?.startsWith("§e") == true -> petCareHandler(baseEntity)

                baseEntity is EntityZombie && armorStand != null && !armorStand.isDefaultValue() -> null // Impossible Rat
                baseEntity is EntityZombie -> ratHandler(baseEntity, nextEntity) // Possible Rat
                else -> null
            }

            IslandType.GARDEN -> when {
                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() -> MobResult.found(
                    Mob(
                        baseEntity,
                        Mob.Type.DISPLAY_NPC,
                        name = baseEntity.cleanName()
                    )
                )

                else -> null
            }

            IslandType.KUUDRA_ARENA -> when {
                baseEntity is EntityMagmaCube && nextEntity is EntityMagmaCube -> MobResult.illegal
                baseEntity is EntityZombie && nextEntity is EntityZombie -> MobResult.illegal
                baseEntity is EntityZombie && nextEntity is EntityGiantZombie -> MobResult.illegal
                else -> null
            }

            IslandType.WINTER -> when {
                baseEntity is EntityMagmaCube && jerryMagmaCubePattern.matches(
                    MobUtils.getArmorStand(
                        baseEntity,
                        2
                    )?.name
                ) ->
                    MobResult.found(
                        Mob(
                            baseEntity,
                            Mob.Type.BOSS,
                            MobUtils.getArmorStand(baseEntity, 2),
                            "Jerry Magma Cube"
                        )
                    )

                else -> null
            }

            else -> null
        }
    }

    private fun petCareHandler(baseEntity: EntityLivingBase): MobResult {
        val extraEntityList = listOf(1, 2, 3, 4).mapNotNull { MobUtils.getArmorStand(baseEntity, it) }
        if (extraEntityList.size != 4) return MobResult.notYetFound
        return petCareNamePattern.matchMatcher(extraEntityList[1].cleanName()) {
            MobResult.found(
                Mob(
                    baseEntity,
                    Mob.Type.SPECIAL,
                    armorStand = extraEntityList[1],
                    name = this.group("name"),
                    additionalEntities = extraEntityList,
                    levelOrTier = this.group("level").toInt()
                ),
            )
        } ?: MobResult.somethingWentWrong
    }

    private fun stackedMobsException(
        baseEntity: EntityLivingBase,
        extraEntityList: List<EntityLivingBase>
    ): MobResult? =
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
            baseEntity.riddenByEntity is EntityPlayer && MobUtils.getArmorStand(baseEntity, 2)?.inventory?.get(4)
                ?.getSkullTexture() == RAT_SKULL -> return MobResult.illegal // Rat Morph
        }
        when (armorStand.inventory?.get(4)?.getSkullTexture()) {
            HELLWISP_TENTACLE_SKULL -> return MobResult.illegal // Hellwisp Tentacle
            RIFT_EYE_SKULL1 -> return MobResult.found(MobFactories.special(baseEntity, "Rift Teleport Eye", armorStand))
            RIFT_EYE_SKULL2 -> return MobResult.found(MobFactories.special(baseEntity, "Rift Teleport Eye", armorStand))
        }
        return null
    }

    fun EntityLivingBase.isFarmMob() =
        this is EntityAnimal && this.baseMaxHealth.derpy()
            .let { it == 50 || it == 20 || it == 130 } && LorenzUtils.skyBlockIsland != IslandType.PRIVATE_ISLAND

    private fun createFarmMobs(baseEntity: EntityLivingBase): Mob? = when (baseEntity) {
        is EntityMooshroom -> MobFactories.basic(baseEntity, "Farm Mooshroom")
        is EntityCow -> MobFactories.basic(baseEntity, "Farm Cow")
        is EntityPig -> MobFactories.basic(baseEntity, "Farm Pig")
        is EntityChicken -> MobFactories.basic(baseEntity, "Farm Chicken")
        is EntityRabbit -> MobFactories.basic(baseEntity, "Farm Rabbit")
        is EntitySheep -> MobFactories.basic(baseEntity, "Farm Sheep")
        else -> null
    }

    private fun createBat(baseEntity: EntityLivingBase): MobResult? = when (baseEntity.baseMaxHealth.derpy()) {
        5_000_000 -> MobResult.found(MobFactories.basic(baseEntity, "Cinderbat"))
        75_000 -> MobResult.found(MobFactories.basic(baseEntity, "Thorn Bat"))
        600 -> if (IslandType.GARDEN.isInIsland()) null else MobResult.notYetFound
        100 -> MobResult.found(
            MobFactories.basic(
                baseEntity,
                if (DungeonAPI.inDungeon()) "Dungeon Secret Bat" else if (IslandType.PRIVATE_ISLAND.isInIsland()) "Private Island Bat" else "Mega Bat"
            )
        )

        20 -> MobResult.found(MobFactories.projectile(baseEntity, "Vampire Mask Bat"))
        // 6 -> MobFactories.projectile(baseEntity, "Spirit Scepter Bat") // moved to Packet Event because 6 is default Health of Bats
        5 -> MobResult.found(MobFactories.special(baseEntity, "Bat Pinata"))
        else -> MobResult.notYetFound
    }

    private fun ratHandler(baseEntity: EntityZombie, nextEntity: EntityLivingBase?): MobResult? =
        generateSequence(ratSearchStart) { it + 1 }.take(ratSearchUpTo - ratSearchStart + 1).map { i ->
            MobUtils.getArmorStand(
                baseEntity, i
            )
        }.firstOrNull {
            it != null && it.distanceTo(baseEntity) < 4.0 && it.inventory?.get(4)?.getSkullTexture() == RAT_SKULL
        }?.let {
            MobResult.found(
                Mob(
                    baseEntity = baseEntity,
                    mobType = Mob.Type.BASIC,
                    armorStand = it,
                    name = "Rat"
                )
            )
        }
            ?: if (nextEntity is EntityZombie) MobResult.notYetFound else null

    private const val ratSearchStart = 1
    private const val ratSearchUpTo = 11
}
