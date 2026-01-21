package at.hannibal2.skyhanni.features.combat.damageindicator

import at.hannibal2.skyhanni.data.ElectionApi.ignoreDerpy
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.dungeon.DungeonLividFinder
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.EntityUtils.hasBossHealth
import at.hannibal2.skyhanni.utils.EntityUtils.hasMaxHealth
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.compat.findHealthReal
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.animal.horse.Horse
import net.minecraft.world.entity.animal.wolf.Wolf
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.monster.AbstractSkeleton
import net.minecraft.world.entity.monster.Blaze
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.monster.Ghast
import net.minecraft.world.entity.monster.Giant
import net.minecraft.world.entity.monster.Guardian
import net.minecraft.world.entity.monster.MagmaCube
import net.minecraft.world.entity.monster.Silverfish
import net.minecraft.world.entity.monster.Spider
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.entity.monster.ZombifiedPiglin
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class MobFinder {

    // F1
    private var floor1bonzo1 = false
    private var floor1bonzo1SpawnTime = SimpleTimeMark.farPast()
    private var floor1bonzo2 = false
    private var floor1bonzo2SpawnTime = SimpleTimeMark.farPast()

    // F2
    private var floor2summons1 = false
    private var floor2summons1SpawnTime = SimpleTimeMark.farPast()
    private val floor2summonsDiedOnce = mutableListOf<RemotePlayer>()
    private var floor2secondPhase = false
    private var floor2secondPhaseSpawnTime = SimpleTimeMark.farPast()

    // F3
    private var floor3GuardianShield = false
    private var floor3GuardianShieldSpawnTime = SimpleTimeMark.farPast()
    private val guardians = mutableListOf<Guardian>()
    private var floor3Professor = false
    private var floor3ProfessorSpawnTime = SimpleTimeMark.farPast()
    private var floor3ProfessorGuardianPrepare = false
    private var floor3ProfessorGuardianPrepareSpawnTime = SimpleTimeMark.farPast()
    private var floor3ProfessorGuardian = false
    private var floor3ProfessorGuardianEntity: Guardian? = null

    // F5
    private var floor5lividEntity: RemotePlayer? = null
    private var floor5lividEntitySpawnTime = SimpleTimeMark.farPast()
    private val correctLividPattern = "§c\\[BOSS] (.*) Livid§r§f: Impossible! How did you figure out which one I was\\?!".toPattern()

    // F6
    private var floor6Giants = false
    private var floor6GiantsSpawnTime = SimpleTimeMark.farPast()
    private val floor6GiantsSeparateDelay = mutableMapOf<UUID, Pair<Duration, BossType>>()
    private var floor6Sadan = false
    private var floor6SadanSpawnTime = SimpleTimeMark.farPast()

    internal fun tryAdd(mob: Mob) = when {
        DungeonApi.inDungeon() -> tryAddDungeon(mob)
        RiftApi.inRift() -> tryAddRift(mob)
        GardenApi.inGarden() -> tryAddGarden(mob)
        else -> {
            if (mob.name == "Dummy") {
                EntityResult(bossType = BossType.DUMMY)
            } else {
                when (val entity = mob.baseEntity) {
                    /*
                     * Note that the order does matter here.
                     * For example, if you put EntityZombie before EntityPigZombie,
                     * EntityPigZombie will never be reached because EntityPigZombie extends EntityZombie.
                     * Please take this into consideration if you are to modify this.
                     */
                    is RemotePlayer -> tryAddEntityOtherPlayerMP(mob)
                    is IronGolem -> tryAddEntityIronGolem(mob)
                    is ZombifiedPiglin -> tryAddEntityPigZombie(mob)
                    is MagmaCube -> tryAddEntityMagmaCube(mob)
                    is EnderMan -> tryAddEntityEnderman(mob)
                    is AbstractSkeleton -> tryAddEntitySkeleton(mob)
                    is Guardian -> tryAddEntityGuardian(mob)
                    is Zombie -> tryAddEntityZombie(mob)
                    is WitherBoss -> tryAddEntityWither(mob)
                    is EnderDragon -> tryAddEntityDragon(mob)
                    is Spider -> tryAddEntitySpider(mob)
                    is Horse -> tryAddEntityHorse(mob)
                    is Blaze -> tryAddEntityBlaze(mob)
                    is Wolf -> tryAddEntityWolf(mob)
                    else -> null
                }
            }
        }
    }

    private fun tryAddGarden(mob: Mob): EntityResult? {
        val entity = mob.baseEntity
        if (entity is Silverfish || entity is Bat) {
            return tryAddGardenPest(mob)
        }

        return null
    }

    private fun tryAddGardenPest(mob: Mob): EntityResult? {
        return PestType.filterableEntries.firstOrNull { mob.name == it.displayName }
            ?.let { EntityResult(bossType = it.damageIndicatorBoss) }
    }

    private fun tryAddDungeon(mob: Mob) = when {
        DungeonApi.isOneOf("F1", "M1") -> tryAddDungeonF1(mob)
        DungeonApi.isOneOf("F2", "M2") -> tryAddDungeonF2(mob)
        DungeonApi.isOneOf("F3", "M3") -> tryAddDungeonF3(mob)
        DungeonApi.isOneOf("F4", "M4") -> tryAddDungeonF4(mob)
        DungeonApi.isOneOf("F5", "M5") -> tryAddDungeonF5(mob)
        DungeonApi.isOneOf("F6", "M6") -> tryAddDungeonF6(mob)
        else -> null
    }

    private fun tryAddDungeonF1(mob: Mob) = when {
        floor1bonzo1 && mob.name == "Bonzo" -> {
            EntityResult(floor1bonzo1SpawnTime, bossType = BossType.DUNGEON_F1_BONZO_FIRST)
        }

        // TODO Use other approach as the old entity does not despawn, so it does not get checked again
        floor1bonzo2 && mob.name == "Bonzo" -> {
            EntityResult(floor1bonzo2SpawnTime, bossType = BossType.DUNGEON_F1_BONZO_SECOND, finalDungeonBoss = true)
        }

        else -> null
    }

    private fun tryAddDungeonF2(mob: Mob): EntityResult? {
        val entity = mob.baseEntity
        if (entity.name.string == "Summon " && entity is RemotePlayer) {
            if (floor2summons1 && !floor2summonsDiedOnce.contains(entity)) {
                if (entity.findHealthReal().toInt() != 0) {
                    return EntityResult(floor2summons1SpawnTime, bossType = BossType.DUNGEON_F2_SUMMON)
                }
                floor2summonsDiedOnce.add(entity)
            }
            if (floor2secondPhase) {
                return EntityResult(floor2secondPhaseSpawnTime, bossType = BossType.DUNGEON_F2_SUMMON)
            }
        }

        if (floor2secondPhase && entity is RemotePlayer) {
            // TODO only show scarf after (all/at least x) summons are dead?
            if (entity.name.string == "Scarf ") {
                return EntityResult(
                    floor2secondPhaseSpawnTime,
                    finalDungeonBoss = true,
                    bossType = BossType.DUNGEON_F2_SCARF,
                )
            }
        }
        return null
    }

    private fun tryAddDungeonF3(mob: Mob): EntityResult? {
        val entity = mob.baseEntity
        if (entity is Guardian && floor3GuardianShield) {
            if (guardians.size == 4) {
                calcGuardiansTotalHealth()
            } else {
                findGuardians()
            }
            if (guardians.contains(entity)) {
                return EntityResult(floor3GuardianShieldSpawnTime, true, bossType = BossType.DUNGEON_F3_GUARDIAN)
            }
        }

        if (floor3Professor && mob.name == "The Professor") {
            return EntityResult(
                floor3ProfessorSpawnTime,
                floor3ProfessorSpawnTime.passedSince() > 1.seconds,
                bossType = BossType.DUNGEON_F3_PROFESSOR_1,
            )
        }
        if (floor3ProfessorGuardianPrepare && mob.name == "The Professor") {
            return EntityResult(
                floor3ProfessorGuardianPrepareSpawnTime,
                true,
                bossType = BossType.DUNGEON_F3_PROFESSOR_2,
            )
        }

        if (entity is Guardian && floor3ProfessorGuardian && entity == floor3ProfessorGuardianEntity) {
            return EntityResult(finalDungeonBoss = true, bossType = BossType.DUNGEON_F3_PROFESSOR_2)
        }
        return null
    }

    private fun tryAddDungeonF4(mob: Mob): EntityResult? {
        if (mob.baseEntity is Ghast) {
            return EntityResult(
                bossType = BossType.DUNGEON_F4_THORN,
                ignoreBlocks = true,
                finalDungeonBoss = true,
            )
        }
        return null
    }

    private fun tryAddDungeonF5(mob: Mob): EntityResult? {
        if (mob.baseEntity == DungeonLividFinder.livid) {
            return EntityResult(
                bossType = BossType.DUNGEON_F5,
                ignoreBlocks = true,
                finalDungeonBoss = true,
            )
        }
        return null
    }

    private fun tryAddDungeonF6(mob: Mob): EntityResult? {
        val entity = mob.baseEntity
        if (entity !is Giant || entity.isInvisible) return null
        if (floor6Giants && entity.position().y > 68) {
            val (extraDelay, bossType) = checkExtraF6GiantsDelay(entity)
            return EntityResult(
                floor6GiantsSpawnTime + extraDelay + 5.seconds,
                floor6GiantsSpawnTime.passedSince() > extraDelay,
                bossType = bossType,
            )
        }

        if (floor6Sadan) {
            return EntityResult(floor6SadanSpawnTime, finalDungeonBoss = true, bossType = BossType.DUNGEON_F6_SADAN, ignoreBlocks = true)
        }
        return null
    }

    private fun tryAddRift(mob: Mob): EntityResult? = when (mob.name) {
        "Leech Supreme" -> EntityResult(bossType = BossType.LEECH_SUPREME)
        "Bloodfiend " -> {
            val entity = mob.baseEntity
            // there is no derpy in rift
            val hp = entity.baseMaxHealth.ignoreDerpy()
            when {
                entity.hasMaxHealth(625, true, hp) -> EntityResult(bossType = BossType.SLAYER_BLOODFIEND_1)
                entity.hasMaxHealth(1_100, true, hp) -> EntityResult(bossType = BossType.SLAYER_BLOODFIEND_2)
                entity.hasMaxHealth(1_800, true, hp) -> EntityResult(bossType = BossType.SLAYER_BLOODFIEND_3)
                entity.hasMaxHealth(2_400, true, hp) -> EntityResult(bossType = BossType.SLAYER_BLOODFIEND_4)
                entity.hasMaxHealth(3_000, true, hp) -> EntityResult(bossType = BossType.SLAYER_BLOODFIEND_5)
                else -> null
            }
        }

        "Bacte" -> EntityResult(bossType = BossType.BACTE)
        "Sun Gecko" -> EntityResult(bossType = BossType.SUN_GECKO)
        else -> null
    }

    private fun tryAddEntityBlaze(mob: Mob) = when (mob.name) {
        "Ashfang" -> {
            EntityResult(bossType = BossType.NETHER_ASHFANG)
        }

        "Inferno Demonlord" -> {
            when (mob.levelOrTier) {
                1 -> EntityResult(bossType = BossType.SLAYER_BLAZE_1)
                2 -> EntityResult(bossType = BossType.SLAYER_BLAZE_2)
                3 -> EntityResult(bossType = BossType.SLAYER_BLAZE_3)
                4 -> EntityResult(bossType = BossType.SLAYER_BLAZE_4)
                else -> null
            }
        }

        else -> null
    }

    private fun tryAddEntitySkeleton(mob: Mob): EntityResult? {
        if (mob.name.contains("ⓆⓊⒶⓏⒾⒾ")) {
            val entity = mob.baseEntity
            return when {
                entity.hasBossHealth(10_000_000) -> EntityResult(bossType = BossType.SLAYER_BLAZE_QUAZII_4)
                entity.hasBossHealth(5_000_000) -> EntityResult(bossType = BossType.SLAYER_BLAZE_QUAZII_3)
                entity.hasBossHealth(1_750_000) -> EntityResult(bossType = BossType.SLAYER_BLAZE_QUAZII_2)
                entity.hasBossHealth(500_000) -> EntityResult(bossType = BossType.SLAYER_BLAZE_QUAZII_1)
                else -> null
            }
        }
        return when (mob.name) {
            "Bladesoul" -> {
                EntityResult(bossType = BossType.NETHER_BLADESOUL)
            }

            else -> null
        }
    }

    private fun tryAddEntityOtherPlayerMP(mob: Mob) = when (mob.name) {
        "Mage Outlaw" -> EntityResult(bossType = BossType.NETHER_MAGE_OUTLAW)
        "DukeBarb" -> EntityResult(bossType = BossType.NETHER_BARBARIAN_DUKE)
        "Minos Inquisitor" -> EntityResult(bossType = BossType.MINOS_INQUISITOR)
        "Minos Champion" -> EntityResult(bossType = BossType.MINOS_CHAMPION)
        "Minotaur " -> EntityResult(bossType = BossType.MINOTAUR)
        "Ragnarok" -> EntityResult(bossType = BossType.RAGNAROK)

        else -> null
    }

    private fun tryAddEntityWither(mob: Mob) = when (mob.name) {
        "Vanquisher" -> EntityResult(bossType = BossType.NETHER_VANQUISHER)
        else -> null
    }

    private fun tryAddEntityEnderman(mob: Mob): EntityResult? {
        if (mob.name != "Voidgloom Seraph") return null
        return when (mob.levelOrTier) {
            1 -> EntityResult(bossType = BossType.SLAYER_ENDERMAN_1)
            2 -> EntityResult(bossType = BossType.SLAYER_ENDERMAN_2)
            3 -> EntityResult(bossType = BossType.SLAYER_ENDERMAN_3)
            4 -> EntityResult(bossType = BossType.SLAYER_ENDERMAN_4)
            else -> null
        }
    }

    // TODO testing and use sidebar data
    @Suppress("UnusedParameter")
    private fun tryAddEntityDragon(mob: Mob) = when {
        IslandType.THE_END.isCurrent() -> EntityResult(bossType = BossType.END_ENDER_DRAGON)
        IslandType.WINTER.isCurrent() -> EntityResult(bossType = BossType.WINTER_REINDRAKE)

        else -> null
    }

    private fun tryAddEntityIronGolem(mob: Mob) = when {
        mob.name == "Endstone Protector" -> EntityResult(bossType = BossType.END_ENDSTONE_PROTECTOR)
        // TODO use Gaia Construct Name
        mob.baseEntity.hasMaxHealth(1_500_000) -> {
            EntityResult(bossType = BossType.GAIA_CONSTRUCT)
        }
        // TODO use Lord Jawbus Name
        mob.baseEntity.hasMaxHealth(100_000_000) -> {
            EntityResult(bossType = BossType.LORD_JAWBUS)
        }

        else -> null
    }

    private fun tryAddEntityZombie(mob: Mob) = when (mob.name) {
        "Revenant Horror" -> when (mob.levelOrTier) {
            1 -> EntityResult(bossType = BossType.SLAYER_ZOMBIE_1)
            2 -> EntityResult(bossType = BossType.SLAYER_ZOMBIE_2)
            3 -> EntityResult(bossType = BossType.SLAYER_ZOMBIE_3)
            4 -> EntityResult(bossType = BossType.SLAYER_ZOMBIE_4)
            5 -> EntityResult(bossType = BossType.SLAYER_ZOMBIE_5)
            else -> null
        }

        "Atoned Horror" -> EntityResult(bossType = BossType.SLAYER_ZOMBIE_5)

        else -> null
    }

    private fun tryAddEntityMagmaCube(mob: Mob) = when (mob.name) {
        "Magma Boss" -> EntityResult(bossType = BossType.NETHER_MAGMA_BOSS, ignoreBlocks = true)
        else -> null
    }

    private fun tryAddEntityHorse(mob: Mob) = when (mob.name) {
        "Headless Horseman" -> EntityResult(bossType = BossType.HUB_HEADLESS_HORSEMAN)
        else -> null
    }

    private fun tryAddEntityPigZombie(mob: Mob) = if (mob.name.contains("ⓉⓎⓅⒽⓄⒺⓊⓈ")) {
        val entity = mob.baseEntity
        when {
            entity.hasBossHealth(10_000_000) -> EntityResult(bossType = BossType.SLAYER_BLAZE_TYPHOEUS_4)
            entity.hasBossHealth(5_000_000) -> EntityResult(bossType = BossType.SLAYER_BLAZE_TYPHOEUS_3)
            entity.hasBossHealth(1_750_000) -> EntityResult(bossType = BossType.SLAYER_BLAZE_TYPHOEUS_2)
            entity.hasBossHealth(500_000) -> EntityResult(bossType = BossType.SLAYER_BLAZE_TYPHOEUS_1)
            else -> null
        }
    } else null

    private fun tryAddEntitySpider(mob: Mob): EntityResult? {
        if (mob.name == "Tarantula Broodfather") {
            return when (mob.levelOrTier) {
                1 -> EntityResult(bossType = BossType.SLAYER_SPIDER_1)
                2 -> EntityResult(bossType = BossType.SLAYER_SPIDER_2)
                3 -> EntityResult(bossType = BossType.SLAYER_SPIDER_3)
                4 -> EntityResult(bossType = BossType.SLAYER_SPIDER_4)
                5 -> EntityResult(bossType = BossType.SLAYER_SPIDER_5_1)
                else -> null
            }
        }

        if (mob.name == "Conjoined Brood") {
            return EntityResult(bossType = BossType.SLAYER_SPIDER_5_2)
        }
        if (IslandType.SPIDER_DEN.isCurrent()) {
            if (mob.name == "Broodmother") {
                return EntityResult(bossType = BossType.BROODMOTHER)
            }
            checkArachne(mob)?.let { return it }
        }
        return null
    }

    private fun checkArachne(mob: Mob): EntityResult? {
        if (mob.name != "Arachne") return null
        return when (mob.levelOrTier) {
            300 -> EntityResult(bossType = BossType.ARACHNE_SMALL)
            500 -> EntityResult(bossType = BossType.ARACHNE_BIG)
            else -> null
        }
    }

    private fun tryAddEntityWolf(mob: Mob) = if (mob.name == "Sven Packmaster") {
        when (mob.levelOrTier) {
            1 -> EntityResult(bossType = BossType.SLAYER_WOLF_1)
            2 -> EntityResult(bossType = BossType.SLAYER_WOLF_2)
            3 -> EntityResult(bossType = BossType.SLAYER_WOLF_3)
            4 -> EntityResult(bossType = BossType.SLAYER_WOLF_4)
            else -> null
        }
    } else null

    private fun tryAddEntityGuardian(mob: Mob) = if (mob.baseEntity.hasMaxHealth(35_000_000)) {
        EntityResult(bossType = BossType.THUNDER)
    } else null

    private fun checkExtraF6GiantsDelay(entity: Giant): Pair<Duration, BossType> {
        val uuid = entity.uuid

        floor6GiantsSeparateDelay[uuid]?.let {
            return it
        }

        val middle = LorenzVec(-8, 0, 56)

        val loc = entity.getLorenzVec()

        val pos: Int

        val type: BossType
        if (loc.x > middle.x && loc.z > middle.z) {
            // first
            pos = 2
            type = BossType.DUNGEON_F6_GIANT_3
        } else if (loc.x > middle.x && loc.z < middle.z) {
            // second
            pos = 3
            type = BossType.DUNGEON_F6_GIANT_4
        } else if (loc.x < middle.x && loc.z < middle.z) {
            // third
            pos = 0
            type = BossType.DUNGEON_F6_GIANT_1
        } else if (loc.x < middle.x && loc.z > middle.z) {
            // fourth
            pos = 1
            type = BossType.DUNGEON_F6_GIANT_2
        } else {
            pos = 0
            type = BossType.DUNGEON_F6_GIANT_1
        }

        val extraDelay = 900.milliseconds * pos
        val pair = extraDelay to type
        floor6GiantsSeparateDelay[uuid] = pair

        return pair
    }

    fun handleChat(message: String) {
        if (!DungeonApi.inDungeon()) return
        when (message) {
            // F1
            "§c[BOSS] Bonzo§r§f: Gratz for making it this far, but I'm basically unbeatable." -> {
                floor1bonzo1 = true
                floor1bonzo1SpawnTime = 11.25.seconds.fromNow()
            }

            "§c[BOSS] Bonzo§r§f: Oh noes, you got me.. what ever will I do?!" -> {
                floor1bonzo1 = false
            }

            "§c[BOSS] Bonzo§r§f: Oh I'm dead!" -> {
                floor1bonzo2 = true
                floor1bonzo2SpawnTime = 4.2.seconds.fromNow()
            }

            "§c[BOSS] Bonzo§r§f: Alright, maybe I'm just weak after all.." -> {
                floor1bonzo2 = false
            }

            // F2
            "§c[BOSS] Scarf§r§f: ARISE, MY CREATIONS!" -> {
                floor2summons1 = true
                floor2summons1SpawnTime = 3.5.seconds.fromNow()
            }

            "§c[BOSS] Scarf§r§f: Those toys are not strong enough I see." -> {
                floor2summons1 = false
            }

            "§c[BOSS] Scarf§r§f: Don't get too excited though." -> {
                floor2secondPhase = true
                floor2secondPhaseSpawnTime = 6.3.seconds.fromNow()
            }

            "§c[BOSS] Scarf§r§f: Whatever..." -> {
                floor2secondPhase = false
            }

            // F3
            "§c[BOSS] The Professor§r§f: I was burdened with terrible news recently..." -> {
                floor3GuardianShield = true
                floor3GuardianShieldSpawnTime = 15.4.seconds.fromNow()
            }

            "§c[BOSS] The Professor§r§f: Oh? You found my Guardians' one weakness?" -> {
                floor3GuardianShield = false
                DamageIndicatorManager.removeDamageIndicator(BossType.DUNGEON_F3_GUARDIAN)
                floor3Professor = true
                floor3ProfessorSpawnTime = 10.3.seconds.fromNow()
            }

            "§c[BOSS] The Professor§r§f: I see. You have forced me to use my ultimate technique." -> {
                floor3Professor = false

                floor3ProfessorGuardianPrepare = true
                floor3ProfessorGuardianPrepareSpawnTime = 10.5.seconds.fromNow()
            }

            "§c[BOSS] The Professor§r§f: The process is irreversible, but I'll be stronger than a Wither now!" -> {
                floor3ProfessorGuardian = true
            }

            "§c[BOSS] The Professor§r§f: What?! My Guardian power is unbeatable!" -> {
                floor3ProfessorGuardian = false
            }

            // F5
            "§c[BOSS] Livid§r§f: This Orb you see, is Thorn, or what is left of him." -> {
                floor5lividEntity = DungeonLividFinder.livid
                floor5lividEntitySpawnTime = 13.seconds.fromNow()
            }

            // F6
            "§c[BOSS] Sadan§r§f: ENOUGH!" -> {
                floor6Giants = true
                floor6GiantsSpawnTime = 2.8.seconds.fromNow()
            }

            "§c[BOSS] Sadan§r§f: You did it. I understand now, you have earned my respect." -> {
                floor6Giants = false
                floor6Sadan = true
                floor6SadanSpawnTime = 11.5.seconds.fromNow()
            }

            "§c[BOSS] Sadan§r§f: NOOOOOOOOO!!! THIS IS IMPOSSIBLE!!" -> {
                floor6Sadan = false
            }
        }

        correctLividPattern.matchMatcher(message) {
            floor5lividEntity = null
        }
    }

    fun handleNewEntity(entity: Entity) {
        if (DungeonApi.inDungeon() && floor3ProfessorGuardian && entity is Guardian && floor3ProfessorGuardianEntity == null) {
            floor3ProfessorGuardianEntity = entity
            floor3ProfessorGuardianPrepare = false
        }
    }

    // While this could be improved to not use getEntities, the performance impact is
    // minimal enough (only happens in f3/m3 bossfight), that it doesn't matter
    @OptIn(AllEntitiesGetter::class)
    private fun findGuardians() {
        guardians.clear()

        for (entity in EntityUtils.getEntities<Guardian>()) {
            // F3
            if (entity.hasMaxHealth(1_000_000, true) || entity.hasMaxHealth(1_200_000, true)) {
                guardians.add(entity)
            }

            // M3
            if (entity.hasMaxHealth(120_000_000, true) || entity.hasMaxHealth(240_000_000, true)) {
                guardians.add(entity)
            }
            // M3 Reinforced Guardian
            if (entity.hasMaxHealth(140_000_000, true) || entity.hasMaxHealth(280_000_000, true)) {
                guardians.add(entity)
            }
        }
    }

    private fun calcGuardiansTotalHealth() {
        var totalHealth = 0
        for (guardian in guardians) {
            totalHealth += guardian.findHealthReal().toInt()
        }
        if (totalHealth == 0) {
            floor3GuardianShield = false
            guardians.clear()
        }
    }
}
