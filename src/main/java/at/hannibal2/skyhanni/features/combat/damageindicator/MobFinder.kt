package at.hannibal2.skyhanni.features.combat.damageindicator

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.dungeon.DungeonLividFinder
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.hasBossHealth
import at.hannibal2.skyhanni.utils.EntityUtils.hasMaxHealth
import at.hannibal2.skyhanni.utils.EntityUtils.hasNameTagWith
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.LorenzUtils.derpy
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.monster.EntityBlaze
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntityGhast
import net.minecraft.entity.monster.EntityGiantZombie
import net.minecraft.entity.monster.EntityGuardian
import net.minecraft.entity.monster.EntityIronGolem
import net.minecraft.entity.monster.EntityMagmaCube
import net.minecraft.entity.monster.EntityPigZombie
import net.minecraft.entity.monster.EntitySkeleton
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.monster.EntitySpider
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.entity.passive.EntityHorse
import net.minecraft.entity.passive.EntityWolf
import java.util.UUID

class MobFinder {

    //F1
    private var floor1bonzo1 = false
    private var floor1bonzo1SpawnTime = 0L
    private var floor1bonzo2 = false
    private var floor1bonzo2SpawnTime = 0L

    //F2
    private var floor2summons1 = false
    private var floor2summons1SpawnTime = 0L
    private var floor2summonsDiedOnce = mutableListOf<EntityOtherPlayerMP>()
    private var floor2secondPhase = false
    private var floor2secondPhaseSpawnTime = 0L

    //F3
    private var floor3GuardianShield = false
    private var floor3GuardianShieldSpawnTime = 0L
    private var guardians = mutableListOf<EntityGuardian>()
    private var floor3Professor = false
    private var floor3ProfessorSpawnTime = 0L
    private var floor3ProfessorGuardianPrepare = false
    private var floor3ProfessorGuardianPrepareSpawnTime = 0L
    private var floor3ProfessorGuardian = false
    private var floor3ProfessorGuardianEntity: EntityGuardian? = null

    //F5
    private var floor5lividEntity: EntityOtherPlayerMP? = null
    private var floor5lividEntitySpawnTime = 0L

    //F6
    private var floor6Giants = false
    private var floor6GiantsSpawnTime = 0L
    private var floor6GiantsSeparateDelay = mutableMapOf<UUID, Long>()
    private var floor6Sadan = false
    private var floor6SadanSpawnTime = 0L

    internal fun tryAdd(entity: EntityLivingBase): EntityResult? {
        if (LorenzUtils.inDungeons) {
            if (DungeonAPI.isOneOf("F1", "M1")) {
                if (floor1bonzo1 && entity is EntityOtherPlayerMP && entity.name == "Bonzo ") {
                    return EntityResult(floor1bonzo1SpawnTime)
                }
                if (floor1bonzo2 && entity is EntityOtherPlayerMP && entity.name == "Bonzo ") {
                    return EntityResult(floor1bonzo2SpawnTime, finalDungeonBoss = true)
                }
            }

            if (DungeonAPI.isOneOf("F2", "M2")) {
                if (entity.name == "Summon " && entity is EntityOtherPlayerMP) {
                    if (floor2summons1 && !floor2summonsDiedOnce.contains(entity)) {
                        if (entity.health.toInt() != 0) {
                            return EntityResult(floor2summons1SpawnTime)
                        } else {
                            floor2summonsDiedOnce.add(entity)
                        }
                    }
                    if (floor2secondPhase) {
                        return EntityResult(floor2secondPhaseSpawnTime)
                    }
                }

                if (floor2secondPhase && entity is EntityOtherPlayerMP) {
                    //TODO only show scarf after (all/at least x) summons are dead?
                    val result = entity.name == "Scarf "
                    if (result) {
                        return EntityResult(floor2secondPhaseSpawnTime, finalDungeonBoss = true)
                    }
                }
            }

            if (DungeonAPI.isOneOf("F3", "M3")) {
                if (entity is EntityGuardian && floor3GuardianShield) {
                    if (guardians.size == 4) {
                        var totalHealth = 0
                        for (guardian in guardians) {
                            totalHealth += guardian.health.toInt()
                        }
                        if (totalHealth == 0) {
                            floor3GuardianShield = false
                            guardians.clear()
                        }
                    } else {
                        findGuardians()
                    }
                    if (guardians.contains(entity)) {
                        return EntityResult(floor3GuardianShieldSpawnTime, true)
                    }
                }

                if (floor3Professor && entity is EntityOtherPlayerMP && entity.name == "The Professor") {
                    return EntityResult(
                        floor3ProfessorSpawnTime,
                        floor3ProfessorSpawnTime + 1_000 > System.currentTimeMillis()
                    )
                }
                if (floor3ProfessorGuardianPrepare && entity is EntityOtherPlayerMP && entity.name == "The Professor") {
                    return EntityResult(floor3ProfessorGuardianPrepareSpawnTime, true)
                }

                if (entity is EntityGuardian && floor3ProfessorGuardian && entity == floor3ProfessorGuardianEntity) {
                    return EntityResult(finalDungeonBoss = true)
                }
            }

            if (DungeonAPI.isOneOf("F4", "M4") && entity is EntityGhast) {
                return EntityResult(
                    bossType = BossType.DUNGEON_F4_THORN,
                    ignoreBlocks = true,
                    finalDungeonBoss = true
                )
            }

            if (DungeonAPI.isOneOf(
                    "F5",
                    "M5"
                ) && entity is EntityOtherPlayerMP && entity == DungeonLividFinder.lividEntity
            ) {
                return EntityResult(
                    bossType = BossType.DUNGEON_F5,
                    ignoreBlocks = true,
                    finalDungeonBoss = true
                )
            }

            if (DungeonAPI.isOneOf("F6", "M6") && entity is EntityGiantZombie && !entity.isInvisible) {
                if (floor6Giants && entity.posY > 68) {
                    val extraDelay = checkExtraF6GiantsDelay(entity)
                    return EntityResult(
                        floor6GiantsSpawnTime + extraDelay,
                        floor6GiantsSpawnTime + extraDelay + 1_000 > System.currentTimeMillis()
                    )
                }

                if (floor6Sadan) {
                    return EntityResult(floor6SadanSpawnTime, finalDungeonBoss = true)
                }
            }
        } else if (RiftAPI.inRift()) {
            if (entity is EntityOtherPlayerMP) {
                if (entity.name == "Leech Supreme") {
                    return EntityResult(bossType = BossType.LEECH_SUPREME)
                }

                if (entity.name == "Bloodfiend ") {
                    when {
                        entity.hasMaxHealth(625, true) -> return EntityResult(bossType = BossType.SLAYER_BLOODFIEND_1)
                        entity.hasMaxHealth(1_100, true) -> return EntityResult(bossType = BossType.SLAYER_BLOODFIEND_2)
                        entity.hasMaxHealth(1_800, true) -> return EntityResult(bossType = BossType.SLAYER_BLOODFIEND_3)
                        entity.hasMaxHealth(2_400, true) -> return EntityResult(bossType = BossType.SLAYER_BLOODFIEND_4)
                        entity.hasMaxHealth(3_000, true) -> return EntityResult(bossType = BossType.SLAYER_BLOODFIEND_5)
                    }
                }
            }
            if (entity is EntitySlime && entity.baseMaxHealth == 1_000) {
                return EntityResult(bossType = BossType.BACTE)
            }
        } else {
            if (entity is EntityBlaze && entity.name != "Dinnerbone" && entity.hasNameTagWith(
                    2,
                    "§e﴾ §8[§7Lv200§8] §l§8§lAshfang§r "
                ) && entity.hasMaxHealth(50_000_000, true)
            ) {
                return EntityResult(bossType = BossType.NETHER_ASHFANG)
            }
            if (entity is EntitySkeleton && entity.hasNameTagWith(5, "§e﴾ §8[§7Lv200§8] §l§8§lBladesoul§r ")) {
                return EntityResult(bossType = BossType.NETHER_BLADESOUL)
            }
            if (entity is EntityOtherPlayerMP) {
                if (entity.name == "Mage Outlaw") {
                    return EntityResult(bossType = BossType.NETHER_MAGE_OUTLAW)
                }
                if (entity.name == "DukeBarb " && entity.getLorenzVec().distanceToPlayer() < 30) {
                    return EntityResult(bossType = BossType.NETHER_BARBARIAN_DUKE)
                }
            }
            if (entity is EntityWither && entity.hasNameTagWith(4, "§8[§7Lv100§8] §c§5Vanquisher§r ")) {
                return EntityResult(bossType = BossType.NETHER_VANQUISHER)
            }
            if (entity is EntityEnderman && entity.hasNameTagWith(3, "§c☠ §bVoidgloom Seraph ")) {
                when {
                    entity.hasMaxHealth(300_000, true) -> return EntityResult(bossType = BossType.SLAYER_ENDERMAN_1)
                    entity.hasMaxHealth(12_000_000, true) -> return EntityResult(bossType = BossType.SLAYER_ENDERMAN_2)
                    entity.hasMaxHealth(50_000_000, true) -> return EntityResult(bossType = BossType.SLAYER_ENDERMAN_3)
                    entity.hasMaxHealth(210_000_000, true) -> return EntityResult(bossType = BossType.SLAYER_ENDERMAN_4)
                }
            }
            if (entity is EntityDragon) {
                //TODO testing and use sidebar data
                if (IslandType.THE_END.isInIsland()) {
                    return EntityResult(bossType = BossType.END_ENDER_DRAGON)
                } else if (IslandType.WINTER.isInIsland()) {
                    return EntityResult(bossType = BossType.WINTER_REINDRAKE)
                }
            }
            if (entity is EntityIronGolem && entity.hasNameTagWith(3, "§e﴾ §8[§7Lv100§8] §lEndstone Protector§r ")) {
                return EntityResult(bossType = BossType.END_ENDSTONE_PROTECTOR)
            }
            if (entity is EntityZombie) {
                if (entity.hasNameTagWith(2, "§c☠ §bRevenant Horror")) {
                    when {
                        entity.hasMaxHealth(500, true) -> return EntityResult(bossType = BossType.SLAYER_ZOMBIE_1)
                        entity.hasMaxHealth(20_000, true) -> return EntityResult(bossType = BossType.SLAYER_ZOMBIE_2)
                        entity.hasMaxHealth(400_000, true) -> return EntityResult(bossType = BossType.SLAYER_ZOMBIE_3)
                        entity.hasMaxHealth(1_500_000, true) -> return EntityResult(bossType = BossType.SLAYER_ZOMBIE_4)
                    }
                }
                if (entity.hasNameTagWith(2, "§c☠ §fAtoned Horror ") && entity.hasMaxHealth(10_000_000, true)) {
                    return EntityResult(bossType = BossType.SLAYER_ZOMBIE_5)
                }
            }
            if (entity is EntityLiving && entity.hasNameTagWith(2, "Dummy §a10M§c❤")) {
                return EntityResult(bossType = BossType.DUMMY)
            }
            if (entity is EntityMagmaCube && entity.hasNameTagWith(
                    15,
                    "§e﴾ §8[§7Lv500§8] §l§4§lMagma Boss§r "
                ) && entity.hasMaxHealth(200_000_000, true)
            ) {
                return EntityResult(bossType = BossType.NETHER_MAGMA_BOSS, ignoreBlocks = true)
            }
            if (entity is EntityHorse && entity.hasNameTagWith(
                    15,
                    "§8[§7Lv100§8] §c§6Headless Horseman§r "
                ) && entity.hasMaxHealth(3_000_000, true)
            ) {
                return EntityResult(bossType = BossType.HUB_HEADLESS_HORSEMAN)
            }
            if (entity is EntityBlaze && entity.hasNameTagWith(2, "§c☠ §bInferno Demonlord ")) {
                when {
                    entity.hasBossHealth(2_500_000) -> return EntityResult(bossType = BossType.SLAYER_BLAZE_1)
                    entity.hasBossHealth(10_000_000) -> return EntityResult(bossType = BossType.SLAYER_BLAZE_2)
                    entity.hasBossHealth(45_000_000) -> return EntityResult(bossType = BossType.SLAYER_BLAZE_3)
                    entity.hasBossHealth(150_000_000) -> return EntityResult(bossType = BossType.SLAYER_BLAZE_4)
                }
            }
            if (entity is EntityPigZombie && entity.hasNameTagWith(2, "§c☠ §6ⓉⓎⓅⒽⓄⒺⓊⓈ ")) {
                when {
                    entity.hasBossHealth(10_000_000) -> return EntityResult(bossType = BossType.SLAYER_BLAZE_TYPHOEUS_4)
                    entity.hasBossHealth(5_000_000) -> return EntityResult(bossType = BossType.SLAYER_BLAZE_TYPHOEUS_3)
                    entity.hasBossHealth(1_750_000) -> return EntityResult(bossType = BossType.SLAYER_BLAZE_TYPHOEUS_2)
                    entity.hasBossHealth(500_000) -> return EntityResult(bossType = BossType.SLAYER_BLAZE_TYPHOEUS_1)
                }
            }
            if (entity is EntitySkeleton && entity.hasNameTagWith(2, "§c☠ §3ⓆⓊⒶⓏⒾⒾ ")) {
                when {
                    entity.hasBossHealth(10_000_000) -> return EntityResult(bossType = BossType.SLAYER_BLAZE_QUAZII_4)
                    entity.hasBossHealth(5_000_000) -> return EntityResult(bossType = BossType.SLAYER_BLAZE_QUAZII_3)
                    entity.hasBossHealth(1_750_000) -> return EntityResult(bossType = BossType.SLAYER_BLAZE_QUAZII_2)
                    entity.hasBossHealth(500_000) -> return EntityResult(bossType = BossType.SLAYER_BLAZE_QUAZII_1)
                }
            }

            if (entity is EntitySpider) {
                if (entity.hasNameTagWith(1, "§5☠ §4Tarantula Broodfather ")) {
                    when {
                        entity.hasMaxHealth(740, true) -> return EntityResult(bossType = BossType.SLAYER_SPIDER_1)
                        entity.hasMaxHealth(30_000, true) -> return EntityResult(bossType = BossType.SLAYER_SPIDER_2)
                        entity.hasMaxHealth(900_000, true) -> return EntityResult(bossType = BossType.SLAYER_SPIDER_3)
                        entity.hasMaxHealth(2_400_000, true) -> return EntityResult(bossType = BossType.SLAYER_SPIDER_4)
                    }
                }
                checkArachne(entity)?.let { return it }
            }
            if (entity is EntityWolf && entity.hasNameTagWith(1, "§c☠ §fSven Packmaster ")) {
                when {
                    entity.hasMaxHealth(2_000, true) -> return EntityResult(bossType = BossType.SLAYER_WOLF_1)
                    entity.hasMaxHealth(40_000, true) -> return EntityResult(bossType = BossType.SLAYER_WOLF_2)
                    entity.hasMaxHealth(750_000, true) -> return EntityResult(bossType = BossType.SLAYER_WOLF_3)
                    entity.hasMaxHealth(2_000_000, true) -> return EntityResult(bossType = BossType.SLAYER_WOLF_4)
                }
            }
            if (entity is EntityOtherPlayerMP) {
                if (entity.name == "Minos Inquisitor") return EntityResult(bossType = BossType.MINOS_INQUISITOR)
                if (entity.name == "Minos Champion") return EntityResult(bossType = BossType.MINOS_CHAMPION)
                if (entity.name == "Minotaur ") return EntityResult(bossType = BossType.MINOTAUR)
            }
            if (entity is EntityIronGolem && entity.hasMaxHealth(1_500_000)) {
                return EntityResult(bossType = BossType.GAIA_CONSTURUCT)
            }
            if (entity is EntityGuardian && entity.hasMaxHealth(35_000_000)) {
                return EntityResult(bossType = BossType.THUNDER)
            }

            if (entity is EntityIronGolem && entity.hasMaxHealth(100_000_000)) {
                return EntityResult(bossType = BossType.LORD_JAWBUS)
            }
        }

        return null
    }

    private fun checkArachne(entity: EntitySpider): EntityResult? {
        if (entity.hasNameTagWith(1, "[§7Lv300§8] §cArachne") ||
            entity.hasNameTagWith(1, "[§7Lv300§8] §lArachne")
        ) {
            val maxHealth = entity.baseMaxHealth
            // Ignore the minis
            if (maxHealth == 12 || maxHealth.derpy() == 4000) return null
            return EntityResult(bossType = BossType.ARACHNE_SMALL)
        }
        if (entity.hasNameTagWith(1, "[§7Lv500§8] §cArachne") ||
            entity.hasNameTagWith(1, "[§7Lv500§8] §lArachne")
        ) {
            val maxHealth = entity.baseMaxHealth
            if (maxHealth == 12 || maxHealth.derpy() == 20_000) return null
            return EntityResult(bossType = BossType.ARACHNE_BIG)
        }

        return null
    }

    private fun checkExtraF6GiantsDelay(entity: EntityGiantZombie): Long {
        val uuid = entity.uniqueID

        if (floor6GiantsSeparateDelay.contains(uuid)) {
            return floor6GiantsSeparateDelay[uuid]!!
        }

        val middle = LorenzVec(-8, 0, 56)

        val loc = entity.getLorenzVec()

        var pos = 0

        //first
        if (loc.x > middle.x && loc.z > middle.z) {
            pos = 2
        }

        //second
        if (loc.x > middle.x && loc.z < middle.z) {
            pos = 3
        }

        //third
        if (loc.x < middle.x && loc.z < middle.z) {
            pos = 0
        }

        //fourth
        if (loc.x < middle.x && loc.z > middle.z) {
            pos = 1
        }

        val extraDelay = 900L * pos
        floor6GiantsSeparateDelay[uuid] = extraDelay

        return extraDelay
    }

    fun handleChat(message: String) {
        if (LorenzUtils.inDungeons) {
            when (message) {
                //F1
                "§c[BOSS] Bonzo§r§f: Gratz for making it this far, but I’m basically unbeatable." -> {
                    floor1bonzo1 = true
                    floor1bonzo1SpawnTime = System.currentTimeMillis() + 11_250
                }

                "§c[BOSS] Bonzo§r§f: Oh noes, you got me.. what ever will I do?!" -> {
                    floor1bonzo1 = false
                }

                "§c[BOSS] Bonzo§r§f: Oh I'm dead!" -> {
                    floor1bonzo2 = true
                    floor1bonzo2SpawnTime = System.currentTimeMillis() + 4_200
                }

                "§c[BOSS] Bonzo§r§f: Alright, maybe I'm just weak after all.." -> {
                    floor1bonzo2 = false
                }

                //F2
                "§c[BOSS] Scarf§r§f: ARISE, MY CREATIONS!" -> {
                    floor2summons1 = true
                    floor2summons1SpawnTime = System.currentTimeMillis() + 3_500
                }

                "§c[BOSS] Scarf§r§f: Those toys are not strong enough I see." -> {
                    floor2summons1 = false
                }

                "§c[BOSS] Scarf§r§f: Don't get too excited though." -> {
                    floor2secondPhase = true
                    floor2secondPhaseSpawnTime = System.currentTimeMillis() + 6_300
                }

                "§c[BOSS] Scarf§r§f: Whatever..." -> {
                    floor2secondPhase = false
                }

                //F3
                "§c[BOSS] The Professor§r§f: I was burdened with terrible news recently..." -> {
                    floor3GuardianShield = true
                    floor3GuardianShieldSpawnTime = System.currentTimeMillis() + 16_400
                }

                "§c[BOSS] The Professor§r§f: Even if you took my barrier down, I can still fight." -> {
                    floor3GuardianShield = false
                }

                "§c[BOSS] The Professor§r§f: Oh? You found my Guardians' one weakness?" -> {
                    floor3Professor = true
                    floor3ProfessorSpawnTime = System.currentTimeMillis() + 10_300
                }

                "§c[BOSS] The Professor§r§f: I see. You have forced me to use my ultimate technique." -> {
                    floor3Professor = false

                    floor3ProfessorGuardianPrepare = true
                    floor3ProfessorGuardianPrepareSpawnTime = System.currentTimeMillis() + 10_500
                }

                "§c[BOSS] The Professor§r§f: The process is irreversible, but I'll be stronger than a Wither now!" -> {
                    floor3ProfessorGuardian = true
                }

                "§c[BOSS] The Professor§r§f: What?! My Guardian power is unbeatable!" -> {
                    floor3ProfessorGuardian = false
                }


                //F5
                "§c[BOSS] Livid§r§f: This Orb you see, is Thorn, or what is left of him." -> {
                    floor5lividEntity = DungeonLividFinder.lividEntity
                    floor5lividEntitySpawnTime = System.currentTimeMillis() + 13_000
                }

                //F6
                "§c[BOSS] Sadan§r§f: ENOUGH!" -> {
                    floor6Giants = true
                    floor6GiantsSpawnTime = System.currentTimeMillis() + 7_400
                }

                "§c[BOSS] Sadan§r§f: You did it. I understand now, you have earned my respect." -> {
                    floor6Giants = false
                    floor6Sadan = true
                    floor6SadanSpawnTime = System.currentTimeMillis() + 32_500
                }

                "§c[BOSS] Sadan§r§f: NOOOOOOOOO!!! THIS IS IMPOSSIBLE!!" -> {
                    floor6Sadan = false
                }
            }

            if (message.matchRegex("§c\\[BOSS] (.*) Livid§r§f: Impossible! How did you figure out which one I was\\?!")) {
                floor5lividEntity = null
            }
        }
    }

    fun handleNewEntity(entity: Entity) {
        if (LorenzUtils.inDungeons && floor3ProfessorGuardian && entity is EntityGuardian && floor3ProfessorGuardianEntity == null) {

            floor3ProfessorGuardianEntity = entity
            floor3ProfessorGuardianPrepare = false

        }
    }

    private fun findGuardians() {
        guardians.clear()

        for (entity in EntityUtils.getEntities<EntityGuardian>()) {
            //F3
            if (entity.hasMaxHealth(1_000_000, true) || entity.hasMaxHealth(1_200_000, true)) {
                guardians.add(entity)
            }

            //M3
            if (entity.hasMaxHealth(120_000_000, true) || entity.hasMaxHealth(240_000_000, true)) {
                guardians.add(entity)
            }
        }
    }
}
