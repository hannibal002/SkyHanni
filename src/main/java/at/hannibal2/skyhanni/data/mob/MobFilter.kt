package at.hannibal2.skyhanni.data.mob

import at.hannibal2.skyhanni.data.ElectionApi.derpy
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.MobData.MobResult
import at.hannibal2.skyhanni.data.mob.MobData.MobResult.Companion.makeMobResult
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.isNpc
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.MobUtils
import at.hannibal2.skyhanni.utils.MobUtils.isDefaultValue
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeWhileInclusive
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.compat.getStandHelmet
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.animal.AbstractCow
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.Chicken
import net.minecraft.world.entity.animal.MushroomCow
import net.minecraft.world.entity.animal.Pig
import net.minecraft.world.entity.animal.Rabbit
import net.minecraft.world.entity.animal.SnowGolem
import net.minecraft.world.entity.animal.horse.Horse
import net.minecraft.world.entity.animal.sheep.Sheep
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.CaveSpider
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.monster.Giant
import net.minecraft.world.entity.monster.Guardian
import net.minecraft.world.entity.monster.Witch
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.entity.player.Player
import org.intellij.lang.annotations.Language

@Suppress("RegExpRedundantEscape")
@SkyHanniModule
object MobFilter {

    private val patternGroup = RepoPattern.group("mob.detection")

    @Language("RegExp")
    private val mobType = "(?<mobType>[^\\w\\s✯\\-]+ )?"

    @Language("RegExp")
    private val level = "(?:\\[Lv(?<level>\\d+)\\] )?"

    /**
     * REGEX-TEST: Wither Husk 500M❤
     * REGEX-TEST: [Lv10] ⚓♃ Sea Guardian 4,250/5,000❤
     */
    val mobNameFilter by patternGroup.pattern(
        "filter.basic",
        "$level$mobType(?<corrupted>.Corrupted )?(?<name>[^ᛤ]*)(?: ᛤ)? [\\dBMk.,❤]+",
    )

    /**
     * REGEX-TEST: ☠ Revenant Horror IV 1.5M❤
     * REGEX-TEST: ☠ Atoned Horror 2M❤
     * REGEX-TEST: ☠ Conjoined Brood 19.9M❤
     * REGEX-FAIL: ☠ Atoned Ho 2M❤
     */
    val slayerNameFilter by patternGroup.pattern(
        "filter.slayer",
        "^$mobType. (?<name>.*)(?: (?<tier>[IV]+)|(?<=Atoned Horror|Conjoined Brood)) \\d+.*",
    )

    /**
     * REGEX-TEST: ﴾ Storm ﴿
     * REGEX-TEST: ﴾ [Lv200] aMage Outlawa 70M/70M❤ ﴿
     * REGEX-TEST: ﴾ [Lv500] Magma Boss █████████████████████████ ﴿
     * REGEX-TEST: ﴾ [Lv200] Bladesoul 50M/50M❤ ﴿
     * REGEX-TEST: ﴾ [Lv300] Arachne 20,000/20,000❤ ﴿
     * REGEX-TEST: ﴾ [Lv500] Arachne 100k/100k❤ ﴿
     * REGEX-TEST: ﴾ [Lv200] Barbarian Duke X 70M/70M❤ ﴿
     * REGEX-TEST: ﴾ [Lv100] Endstone Protector 4.6M/5M❤ ﴿
     * REGEX-TEST: ﴾ [Lv400] Thunder 29M/35M❤ ﴿
     */
    val bossMobNameFilter by patternGroup.pattern(
        "filter.boss",
        "^. $level$mobType(?<name>[^ᛤ\n]*?)(?: ᛤ)?(?: [\\d\\/BMk.,❤]+| █+)? .$",
    )

    @Suppress("MaxLineLength")
    val dungeonNameFilter by patternGroup.pattern(
        "filter.dungeon",
        "^$level$mobType(?:(?<star>✯)\\s)?(?:(?<attribute>${DungeonAttribute.toRegexLine})\\s)?(?:\\[[\\w\\d]+\\]\\s)?(?<name>[^ᛤ]+)(?: ᛤ)?\\s[^\\s]+$",
    )
    val summonFilter by patternGroup.pattern(
        "filter.summon",
        "^(?<owner>\\w+)'s (?<name>.*) \\d+.*",
    )
    val dojoFilter by patternGroup.pattern(
        "filter.dojo",
        "^(?:(?<points>\\d+) pts|(?<empty>\\w+))$",
    )

    /**
     * REGEX-TEST: [Lv1] ✰⛨ Throwpo's Green Jerry 3 Hits
     * REGEX-TEST: [Lv1] ✰⛨ RecluseFang's Green Jerry 3 Hits
     * REGEX-TEST: [Lv1] ✰⛨ aThunderblade73's Green Jerrya 7 Hits
     */
    val jerryPattern by patternGroup.pattern(
        "jerry",
        "(?:\\[\\w+(?<level>\\d+)] )?✰⛨ (?:(?:a(?=a ))?(?<owner>\\w+)'s (?<name>\\w+ Jerrya?)) \\d+ Hits",
    )
    val petCareNamePattern by patternGroup.pattern(
        "pattern.petcare",
        "^\\[\\w+ (?<level>\\d+)\\] (?<name>.*)",
    )

    // TODO fix pattern
    val wokeSleepingGolemPattern by patternGroup.pattern(
        "pattern.dungeon.woke.golem",
        "(?:§c§lWoke|§5§lSleeping) Golem§r",
    )
    val jerryMagmaCubePattern by patternGroup.pattern(
        "pattern.jerry.magma.cube",
        "§c(?:Cubie|Maggie|Cubert|Cübe|Cubette|Magmalene|Lucky 7|8ball|Mega Cube|Super Cube)(?: ᛤ)? §a\\d+§8\\/§a\\d+§c❤",
    )
    val summonOwnerPattern by patternGroup.pattern(
        "pattern.summon.owner",
        ".*Spawned by: (?<name>.*).*",
    )
    val heavyPearlPattern by patternGroup.pattern(
        "pattern.heavypearl.collect",
        "§.§lCOLLECT!",
    )

    /**
     * REGEX-TEST: SHINY PIG
     */
    val shinyPig by patternGroup.pattern(
        "pattern.shiny",
        "SHINY PIG",
    )

    /**
     * REGEX-TEST: §8[§7Lv1§8] §5Horse
     * REGEX-TEST: §8[§7Lv52§8] §eArmadillo
     * REGEX-TEST: §8[§7Lv12§8] §eSkeleton Horse
     * REGEX-TEST: §8[§7Lv49§8] §ePig
     * REGEX-TEST: §8[§7Lv64§8] §eRat
     */
    val illegalEntitiesPattern by patternGroup.pattern(
        "pattern.pet.entities",
        "^§8\\[§7Lv\\d+§8] §.(?<name>Horse|Armadillo|Skeleton Horse|Pig|Rat)$",
    )

    internal val RAT_SKULL_TEXTURE by lazy { SkullTextureHolder.getTexture("MOB_RAT") }
    private val HELLWISP_TENTACLE_SKULL_TEXTURE by lazy { SkullTextureHolder.getTexture("HELLWISP_TENTACLE") }
    private val RIFT_EYE_SKULL1_TEXTURE by lazy { SkullTextureHolder.getTexture("RIFT_EYE_1") }
    private val RIFT_EYE_SKULL2_TEXTURE by lazy { SkullTextureHolder.getTexture("RIFT_EYE_2") }
    internal val NPC_TURD_SKULL by lazy { SkullTextureHolder.getTexture("NPC_TURD") }

    const val MINION_MOB_PREFIX = "Minion Mob "

    enum class DungeonAttribute {
        Flaming,
        Stormy,
        Speedy,
        Fortified,
        Healthy,
        Healing,
        Boomer,
        Golden,
        Stealth,
        ;

        companion object {

            val toRegexLine = DungeonAttribute.entries.joinToString("|") { it.name }
        }
    }

    private val extraDisplayNpcByName = setOf(
        "Guy ", // Guy NPC (but only as visitor)
        "vswiblxdxg", // Mayor Cole
        "anrrtqytsl", // Weaponsmith
    )

    private val displayNpcCompressedNamePattern by patternGroup.pattern("displaynpc.name", "[a-z0-9]{10}")

    private fun displayNpcNameCheck(name: String) = name.startsWith('§') ||
        displayNpcCompressedNamePattern.matches(name) ||
        extraDisplayNpcByName.contains(name)

    private val listOfClickArmorStand = setOf(
        "§e§lCLICK",
        "§6§lSEASONAL SKINS",
        "§e§lGATE KEEPER",
        "§e§lBLACKSMITH",
        "§e§lSHOP",
        "§e§lTREASURES",
        "§c§lQUEST",
        "§e§lQUEST",
    )

    fun Entity.isSkyBlockMob(): Boolean = when {
        this !is LivingEntity -> false
        this is ArmorStand -> false
        this is Player && this.isRealPlayer() -> false
        this.isDisplayNpc() -> false
        this is WitherBoss && this.id < 0 -> false
        else -> true
    }

    fun Player.isRealPlayer() = uuid?.let { it.version() == 4 } ?: false

    fun LivingEntity.isDisplayNpc() =
        (this is Player && isNpc() && displayNpcNameCheck(this.name.formattedTextCompatLessResets())) ||
            (this is Villager && this.maxHealth == 20f) || // Villager NPCs in the Village
            (this is Witch && this.id <= 500) || // Alchemist NPC
            (this is AbstractCow && this.id <= 500) || // Shania NPC (in Rift and Outside)
            (this is Pig && this.id <= 600) || // Pig Shop
            (this is SnowGolem && this.id <= 500) // Sherry NPC (in Jerry Island)

    fun createDisplayNpc(entity: LivingEntity): Boolean {
        val clickArmorStand = MobUtils.getArmorStandByRangeAll(entity, 1.5).firstOrNull { armorStand ->
            listOfClickArmorStand.contains(armorStand.name.formattedTextCompatLessResets())
        } ?: return false
        val armorStand = MobUtils.getArmorStand(clickArmorStand, -1) ?: return false
        MobEvent.Spawn.DisplayNpc(MobFactories.displayNpc(entity, armorStand, clickArmorStand)).post()
        return true
    }

    /** baseEntity must have passed the .isSkyBlockMob() function */
    internal fun createSkyblockEntity(baseEntity: LivingEntity): MobResult {
        val nextEntity = MobUtils.getNextEntity(baseEntity, 1) as? LivingEntity

        exceptions(baseEntity, nextEntity)?.let { return it }

        // Check if Late Stack
        nextEntity?.let {
            MobData.entityToMob[it]?.apply { internalAddEntity(baseEntity) }?.also { return MobResult.illegal }
        }

        // Stack up the mob
        var caughtSkyblockMob: Mob? = null
        val extraEntityList = generateSequence(nextEntity) {
            MobUtils.getNextEntity(it, 1) as? LivingEntity
        }.takeWhileInclusive { entity ->
            !(entity is ArmorStand && !entity.isDefaultValue()) && MobData.entityToMob[entity]?.also {
                caughtSkyblockMob = it
            }?.run { false } ?: true
        }.toList()
        stackedMobsException(baseEntity, extraEntityList)?.let { return it }

        // If Late Stack add all entities
        caughtSkyblockMob?.apply { internalAddEntity(extraEntityList.dropLast(1)) }?.also { return MobResult.illegal }

        val armorStand = extraEntityList.lastOrNull() as? ArmorStand ?: return MobResult.notYetFound

        if (armorStand.isDefaultValue()) return MobResult.notYetFound
        return createSkyblockMob(baseEntity, armorStand, extraEntityList.dropLast(1))?.let { MobResult.found(it) }
            ?: MobResult.notYetFound
    }

    private fun createSkyblockMob(
        baseEntity: LivingEntity,
        armorStand: ArmorStand,
        extraEntityList: List<LivingEntity>,
    ): Mob? =
        MobFactories.summon(baseEntity, armorStand, extraEntityList)
            ?: MobFactories.slayer(baseEntity, armorStand, extraEntityList)
            ?: MobFactories.boss(baseEntity, armorStand, extraEntityList)
            ?: if (DungeonApi.inDungeon()) MobFactories.dungeon(
                baseEntity,
                armorStand,
                extraEntityList,
            ) else (
                MobFactories.basic(baseEntity, armorStand, extraEntityList)
                    ?: MobFactories.dojo(baseEntity, armorStand)
                )

    private fun noArmorStandMobs(baseEntity: LivingEntity): MobResult? = when {
        baseEntity is Bat -> createBat(baseEntity)

        baseEntity.isFarmMob() -> createFarmMobs(baseEntity)?.let { MobResult.found(it) }
        baseEntity is EnderDragon -> when (SkyBlockUtils.currentIsland) {
            IslandType.CATACOMBS -> (8..16).map { MobUtils.getArmorStand(baseEntity, it) }
                .makeMobResult {
                    MobFactories.boss(baseEntity, it.first(), it.drop(1))
                }

            else -> MobResult.found(MobFactories.basic(baseEntity, baseEntity.cleanName()))
        }

        baseEntity is Giant && baseEntity.name.formattedTextCompatLessResets() == "Dinnerbone" -> MobResult.found(
            MobFactories.projectile(
                baseEntity,
                "Giant Sword",
            ),
        ) // Will false trigger if there is another Dinnerbone Giant
        baseEntity is CaveSpider -> MobUtils.getArmorStand(baseEntity, -1)
            ?.takeIf { summonOwnerPattern.matches(it.cleanName()) }?.let {
                MobData.entityToMob[MobUtils.getNextEntity(baseEntity, -4)]?.internalAddEntity(baseEntity)
                    ?.let { MobResult.illegal }
            }

        baseEntity is WitherBoss && baseEntity.invulnerableTicks == 800 -> MobResult.found(
            MobFactories.special(
                baseEntity,
                "Mini Wither",
            ),
        )

        baseEntity is RemotePlayer && baseEntity.name.formattedTextCompatLessResets() == "Decoy " -> MobResult.found(
            MobFactories.special(
                baseEntity,
                "Decoy",
            ),
        )

        else -> null
    }

    private fun exceptions(baseEntity: LivingEntity, nextEntity: LivingEntity?): MobResult? {
        noArmorStandMobs(baseEntity)?.also { return it }
        val armorStand = nextEntity as? ArmorStand
        IslandExceptions.islandSpecificExceptions(baseEntity, armorStand, nextEntity)?.also { return it }

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
                    levelOrTier = level,
                ),
            )
        }
        return when {
            (baseEntity is Pig || baseEntity is Horse) && illegalEntitiesPattern.matches(armorStand.name.formattedTextCompatLessResets()) -> MobResult.illegal
            baseEntity is Guardian && armorStand.cleanName()
                .matches("^\\d+".toRegex()) -> MobResult.illegal // Wierd Sea Guardian Ability
            else -> null
        }
    }

    private fun stackedMobsException(
        baseEntity: LivingEntity,
        extraEntityList: List<LivingEntity>,
    ): MobResult? =
        if (DungeonApi.inDungeon()) {
            when {
                (baseEntity is EnderMan || baseEntity is Giant) &&
                    extraEntityList.lastOrNull()?.name.formattedTextCompatLessResets() == "§e﴾ §c§lLivid§r§r §a7M§c❤ §e﴿" -> MobResult.illegal // Livid Start Animation
                else -> null
            }
        } else when (SkyBlockUtils.currentIsland) {
            IslandType.CRIMSON_ISLE -> when {
                else -> null
            }

            else -> null
        }

    private fun armorStandOnlyMobs(baseEntity: LivingEntity, armorStand: ArmorStand): MobResult? {
        if (baseEntity !is Zombie) return null
        when {
            illegalEntitiesPattern.matches(armorStand.name.formattedTextCompatLessResets()) -> return MobResult.illegal
            baseEntity.firstPassenger is Player && MobUtils.getArmorStand(baseEntity, 2)
                ?.wearingSkullTexture(RAT_SKULL_TEXTURE) ?: false -> return MobResult.illegal // Rat Morph
        }
        when (armorStand.getStandHelmet()?.getSkullTexture()) {
            HELLWISP_TENTACLE_SKULL_TEXTURE -> return MobResult.illegal // Hellwisp Tentacle
            RIFT_EYE_SKULL1_TEXTURE -> return MobResult.found(MobFactories.special(baseEntity, "Rift Teleport Eye", armorStand))
            RIFT_EYE_SKULL2_TEXTURE -> return MobResult.found(MobFactories.special(baseEntity, "Rift Teleport Eye", armorStand))
        }
        return null
    }

    fun LivingEntity.isFarmMob() =
        this is Animal && this.baseMaxHealth.derpy()
            .let { it == 50 || it == 20 || it == 130 } && SkyBlockUtils.currentIsland != IslandType.PRIVATE_ISLAND

    private fun createFarmMobs(baseEntity: LivingEntity): Mob? = when (baseEntity) {
        is MushroomCow -> MobFactories.basic(baseEntity, "Farm Mooshroom")
        is AbstractCow -> MobFactories.basic(baseEntity, "Farm Cow")
        is Pig -> MobFactories.basic(baseEntity, "Farm Pig")
        is Chicken -> MobFactories.basic(baseEntity, "Farm Chicken")
        is Rabbit -> MobFactories.basic(baseEntity, "Farm Rabbit")
        is Sheep -> MobFactories.basic(baseEntity, "Farm Sheep")
        else -> null
    }

    private fun createBat(baseEntity: LivingEntity): MobResult? = when (baseEntity.baseMaxHealth.derpy()) {
        5_000_000 -> MobResult.found(MobFactories.basic(baseEntity, "Cinderbat"))
        75_000 -> MobResult.found(MobFactories.basic(baseEntity, "Thorn Bat"))
        600 -> if (IslandType.GARDEN.isCurrent()) null else MobResult.notYetFound
        100 -> MobResult.found(
            MobFactories.basic(
                baseEntity,
                when {
                    DungeonApi.inDungeon() -> "Dungeon Secret Bat"
                    IslandType.PRIVATE_ISLAND.isCurrent() -> "Private Island Bat"
                    else -> "Mega Bat"
                },
            ),
        )

        20 -> MobResult.found(MobFactories.projectile(baseEntity, "Vampire Mask Bat"))
        // 6 -> MobFactories.projectile(baseEntity, "Spirit Scepter Bat") // moved to Packet Event because 6 is default Health of Bats
        5 -> MobResult.found(MobFactories.special(baseEntity, "Bat Pinata"))
        else -> MobResult.notYetFound
    }

    internal fun ArmorStand?.makeMobResult(mob: (ArmorStand) -> Mob?) =
        this?.let { armor ->
            mob.invoke(armor)?.let { MobResult.found(it) } ?: MobResult.somethingWentWrong
        } ?: MobResult.notYetFound
}
