package at.hannibal2.skyhanni.features.damageindicator

import at.hannibal2.skyhanni.features.dungeon.DungeonData
import at.hannibal2.skyhanni.utils.EntityUtils.hasNameTagWith
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.LorenzUtils.matchRegex
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.boss.EntityWither
import net.minecraft.entity.monster.*
import net.minecraft.entity.passive.EntityHorse
import net.minecraft.entity.passive.EntityWolf
import java.util.*

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
            if (DungeonData.isOneOf("F1", "M1")) {
                if (floor1bonzo1) {
                    if (entity is EntityOtherPlayerMP) {
                        if (entity.name == "Bonzo ") {
                            return EntityResult(floor1bonzo1SpawnTime)
                        }
                    }
                }
                if (floor1bonzo2) {
                    if (entity is EntityOtherPlayerMP) {
                        if (entity.name == "Bonzo ") {
                            return EntityResult(floor1bonzo2SpawnTime, finalDungeonBoss = true)
                        }
                    }
                }
            }

            if (DungeonData.isOneOf("F2", "M2")) {
                if (entity.name == "Summon ") {
                    if (entity is EntityOtherPlayerMP) {
                        if (floor2summons1) {
                            if (!floor2summonsDiedOnce.contains(entity)) {
                                if (entity.health.toInt() != 0) {
                                    return EntityResult(floor2summons1SpawnTime)
                                } else {
                                    floor2summonsDiedOnce.add(entity)
                                }
                            }
                        }
                        if (floor2secondPhase) {
                            return EntityResult(floor2secondPhaseSpawnTime)
                        }
                    }
                }

                if (floor2secondPhase) {
                    if (entity is EntityOtherPlayerMP) {
                        //TODO only show scarf after (all/at least x) summons are dead?
                        val result = entity.name == "Scarf "
                        if (result) {
                            return EntityResult(floor2secondPhaseSpawnTime, finalDungeonBoss = true)
                        }
                    }
                }
            }

            if (DungeonData.isOneOf("F3", "M3")) {
                if (entity is EntityGuardian) {
                    if (floor3GuardianShield) {
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
                }

                if (floor3Professor) {
                    if (entity is EntityOtherPlayerMP) {
                        if (entity.name == "The Professor") {
                            return EntityResult(
                                floor3ProfessorSpawnTime,
                                floor3ProfessorSpawnTime + 1_000 > System.currentTimeMillis()
                            )
                        }
                    }
                }
                if (floor3ProfessorGuardianPrepare) {
                    if (entity is EntityOtherPlayerMP) {
                        if (entity.name == "The Professor") {
                            return EntityResult(floor3ProfessorGuardianPrepareSpawnTime, true)
                        }
                    }
                }

                if (entity is EntityGuardian) {
                    if (floor3ProfessorGuardian) {
                        if (entity == floor3ProfessorGuardianEntity) {
                            return EntityResult(finalDungeonBoss = true)
                        }
                    }
                }
            }

            if (DungeonData.isOneOf("F4", "M4")) {
                if (entity is EntityGhast) {
                    return EntityResult(bossType = BossType.DUNGEON_F4_THORN,
                        ignoreBlocks = true,
                        finalDungeonBoss = true)
                }

            }

            if (DungeonData.isOneOf("F5", "M5")) {
                if (entity is EntityOtherPlayerMP) {
                    if (entity == floor5lividEntity) {
                        return EntityResult(floor5lividEntitySpawnTime, true, finalDungeonBoss = true)
                    }
                }
            }

            if (DungeonData.isOneOf("F6", "M6")) {
                if (entity is EntityGiantZombie && !entity.isInvisible) {
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
            }
        } else {

            val maxHealth = entity.baseMaxHealth.toInt()
            if (entity is EntityBlaze) {
                if (entity.name != "Dinnerbone") {
                    if (entity.hasNameTagWith(2, "§e﴾ §8[§7Lv200§8] §l§8§lAshfang§r ")) {
                        if (maxHealth == 50_000_000) {
                            return EntityResult(bossType = BossType.NETHER_ASHFANG)
                        }
                        //Derpy
                        if (maxHealth == 100_000_000) {
                            return EntityResult(bossType = BossType.NETHER_ASHFANG)
                        }
                    }
                }
            }
            if (entity is EntitySkeleton) {
                if (entity.hasNameTagWith(5, "§e﴾ §8[§7Lv200§8] §l§8§lBladesoul§r ")) {
                    return EntityResult(bossType = BossType.NETHER_BLADESOUL)
                }
            }
            if (entity is EntityOtherPlayerMP) {
                if (entity.name == "Mage Outlaw") {
                    return EntityResult(bossType = BossType.NETHER_MAGE_OUTLAW)
                }
                if (entity.name == "DukeBarb ") {
                    return EntityResult(bossType = BossType.NETHER_BARBARIAN_DUKE)
                }
            }
            if (entity is EntityWither) {
                if (entity.hasNameTagWith(4, "§8[§7Lv100§8] §c§5Vanquisher§r ")) {
                    return EntityResult(bossType = BossType.NETHER_VANQUISHER)
                }
            }
            if (entity is EntityEnderman) {
                if (entity.hasNameTagWith(3, "§c☠ §bVoidgloom Seraph ")) {
                    when (maxHealth) {
                        300_000, 600_000 -> {
                            return EntityResult(bossType = BossType.SLAYER_ENDERMAN_1)
                        }
                        12_000_000, 24_000_000 -> {
                            return EntityResult(bossType = BossType.SLAYER_ENDERMAN_2)
                        }
                        50_000_000, 100_000_000 -> {
                            return EntityResult(bossType = BossType.SLAYER_ENDERMAN_3)
                        }
                        210_000_000, 420_000_000 -> {
                            return EntityResult(bossType = BossType.SLAYER_ENDERMAN_4)
                        }
                    }
                }
            }
            if (entity is EntityDragon) {
                //TODO testing and use sidebar data
                return EntityResult(bossType = BossType.END_ENDER_DRAGON)
            }
            if (entity is EntityIronGolem) {
                if (entity.hasNameTagWith(3, "§e﴾ §8[§7Lv100§8] §lEndstone Protector§r ")) {
                    return EntityResult(bossType = BossType.END_ENDSTONE_PROTECTOR)
                }
            }
            if (entity is EntityZombie) {
                if (entity.hasNameTagWith(2, "§c☠ §bRevenant Horror")) {
                    when (maxHealth) {
                        500, 1_000 -> {
                            return EntityResult(bossType = BossType.SLAYER_ZOMBIE_1)
                        }
                        20_000, 40_000 -> {
                            return EntityResult(bossType = BossType.SLAYER_ZOMBIE_2)
                        }
                        400_000, 800_000 -> {
                            return EntityResult(bossType = BossType.SLAYER_ZOMBIE_3)
                        }
                        1_500_000, 3_000_000 -> {
                            return EntityResult(bossType = BossType.SLAYER_ZOMBIE_4)
                        }
                    }
                }
                if (entity.hasNameTagWith(2, "§c☠ §fAtoned Horror ")) {
                    if (maxHealth == 10_000_000 || maxHealth == 20_000_000) {
                        return EntityResult(bossType = BossType.SLAYER_ZOMBIE_5)
                    }
                }
            }
            if (entity is EntityLiving) {
                if (entity.hasNameTagWith(2, "Dummy §a10M§c❤")) {
                    return EntityResult(bossType = BossType.DUMMY)
                }
            }
            if (entity is EntityMagmaCube) {
                if (entity.hasNameTagWith(15, "§e﴾ §8[§7Lv500§8] §l§4§lMagma Boss§r ")) {
                    if (maxHealth == 200_000_000) {
                        return EntityResult(bossType = BossType.NETHER_MAGMA_BOSS, ignoreBlocks = true)
                    }
                    //Derpy
                    if (maxHealth == 400_000_000) {
                        return EntityResult(bossType = BossType.NETHER_MAGMA_BOSS, ignoreBlocks = true)
                    }
                }
            }
            if (entity is EntityHorse) {
                if (entity.hasNameTagWith(15, "§8[§7Lv100§8] §c§6Headless Horseman§r ")) {
                    if (maxHealth == 3_000_000) {
                        return EntityResult(bossType = BossType.HUB_HEADLESS_HORSEMAN)
                    }
                    //Derpy
                    if (maxHealth == 6_000_000) {
                        return EntityResult(bossType = BossType.HUB_HEADLESS_HORSEMAN)
                    }
                }
            }
            if (entity is EntityBlaze) {
                if (entity.hasNameTagWith(2, "§c☠ §bInferno Demonlord ")) {
                    when (maxHealth) {
                        2_500_000, 5_000_000 -> {
                            return EntityResult(bossType = BossType.SLAYER_BLAZE_1)
                        }
                    }
                }
            }
            if (entity is EntitySpider) {
                if (entity.hasNameTagWith(1, "§5☠ §4Tarantula Broodfather ")) {
                    when (maxHealth) {
                        740, 1_500 -> {
                            return EntityResult(bossType = BossType.SLAYER_SPIDER_1)
                        }
                        30_000, 60_000 -> {
                            return EntityResult(bossType = BossType.SLAYER_SPIDER_2)
                        }
                        900_000, 1_800_000 -> {
                            return EntityResult(bossType = BossType.SLAYER_SPIDER_3)
                        }
                        2_400_000, 4_800_000 -> {
                            return EntityResult(bossType = BossType.SLAYER_SPIDER_4)
                        }
                    }
                }
            }
            if (entity is EntityWolf) {
                if (entity.hasNameTagWith(1, "§c☠ §fSven Packmaster ")) {
                    when (maxHealth) {
                        2_000, 4_000 -> {
                            return EntityResult(bossType = BossType.SLAYER_WOLF_1)
                        }
                        40_000, 80_000 -> {
                            return EntityResult(bossType = BossType.SLAYER_WOLF_2)
                        }
                        750_000, 1_500_000 -> {
                            return EntityResult(bossType = BossType.SLAYER_WOLF_3)
                        }
                        2_000_000, 4_000_000 -> {
                            return EntityResult(bossType = BossType.SLAYER_WOLF_4)
                        }
                    }
                }
            }
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

                "§c[BOSS] The Professor§r§f: Oh? You found my Guardians one weakness?" -> {
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
                    floor5lividEntity = findLivid()
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
        if (LorenzUtils.inDungeons) {
            if (floor3ProfessorGuardian) {
                if (entity is EntityGuardian) {
                    if (floor3ProfessorGuardianEntity == null) {
                        floor3ProfessorGuardianEntity = entity
                        floor3ProfessorGuardianPrepare = false
                    }
                }
            }
        }
    }

    private fun findGuardians() {
        guardians.clear()

        for (entity in Minecraft.getMinecraft().theWorld.loadedEntityList) {
            if (entity is EntityGuardian) {

                val maxHealth = entity.baseMaxHealth.toInt()

                //F3
                if (maxHealth == 1_000_000 || maxHealth == 1_200_000) {
                    guardians.add(entity)
                }

                //F3  Derpy
                if (maxHealth == 2_000_000 || maxHealth == 2_400_000) {
                    guardians.add(entity)
                }

                //M3
                if (maxHealth == 240_000_000 || maxHealth == 280_000_000) {
                    guardians.add(entity)
                }

                //M3 Derpy
                if (maxHealth == 120_000_000 || maxHealth == 140_000_000) {
                    guardians.add(entity)
                }
            }
        }
    }

    private fun findLivid(): EntityOtherPlayerMP? {
        for (entity in Minecraft.getMinecraft().theWorld.loadedEntityList) {
            if (entity is EntityOtherPlayerMP) {
                if (entity.name == "Livid ") {
                    return entity
                }
            }
        }

        return null
    }
}