package at.hannibal2.skyhanni.data.mob

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.MobFilter.makeMobResult
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.isNPC
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.LorenzUtils.derpy
import at.hannibal2.skyhanni.utils.MobUtils
import at.hannibal2.skyhanni.utils.MobUtils.isDefaultValue
import at.hannibal2.skyhanni.utils.MobUtils.takeNonDefault
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityCaveSpider
import net.minecraft.entity.monster.EntityCreeper
import net.minecraft.entity.monster.EntityGiantZombie
import net.minecraft.entity.monster.EntityIronGolem
import net.minecraft.entity.monster.EntityMagmaCube
import net.minecraft.entity.monster.EntityPigZombie
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.entity.passive.EntityOcelot
import net.minecraft.entity.passive.EntityPig

object IslandExceptions {

    internal fun islandSpecificExceptions(
        baseEntity: EntityLivingBase,
        armorStand: EntityArmorStand?,
        nextEntity: EntityLivingBase?,
    ): MobData.MobResult? {
        return if (DungeonAPI.inDungeon()) {
            when {
                baseEntity is EntityZombie && armorStand != null && (armorStand.name == "§e﴾ §c§lThe Watcher§r§r §e﴿" || armorStand.name == "§3§lWatchful Eye§r") -> MobData.MobResult.found(
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

                baseEntity is EntityIronGolem && MobFilter.wokeSleepingGolemPattern.matches(
                    armorStand?.name ?: ""
                ) -> MobData.MobResult.found(
                    Mob(
                        baseEntity,
                        Mob.Type.DUNGEON,
                        armorStand,
                        "Sleeping Golem"
                    )
                ) // Consistency fix
                else -> null
            }
        } else when (LorenzUtils.skyBlockIsland) {
            IslandType.PRIVATE_ISLAND -> when {
                armorStand?.isDefaultValue() != false -> if (baseEntity.getLorenzVec()
                        .distanceChebyshevIgnoreY(LocationUtils.playerLocation()) < 15.0
                ) MobData.MobResult.found(MobFactories.minionMob(baseEntity)) else MobData.MobResult.notYetFound // TODO fix to always include Valid Mobs on Private Island
                else -> null
            }

            IslandType.THE_RIFT -> when {
                baseEntity is EntitySlime && nextEntity is EntitySlime -> MobData.MobResult.illegal// Bacte Tentacle
                baseEntity is EntitySlime && armorStand != null && armorStand.cleanName()
                    .startsWith("﴾ [Lv10] B") -> MobData.MobResult.found(
                    Mob(
                        baseEntity,
                        Mob.Type.BOSS,
                        armorStand,
                        name = "Bacte"
                    )
                )

                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && baseEntity.name == "Branchstrutter " -> MobData.MobResult.found(
                    Mob(baseEntity, Mob.Type.DISPLAY_NPC, name = "Branchstrutter")
                )

                else -> null
            }

            IslandType.CRIMSON_ISLE -> when {
                baseEntity is EntitySlime && armorStand?.name == "§f§lCOLLECT!" -> MobData.MobResult.found(
                    MobFactories.special(
                        baseEntity,
                        "Heavy Pearl"
                    )
                )

                baseEntity is EntityPig && nextEntity is EntityPig -> MobData.MobResult.illegal // Matriarch Tongue
                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && baseEntity.name == "BarbarianGuard " -> MobData.MobResult.found(
                    Mob(baseEntity, Mob.Type.DISPLAY_NPC, name = "Barbarian Guard")
                )

                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && baseEntity.name == "MageGuard " -> MobData.MobResult.found(
                    Mob(baseEntity, Mob.Type.DISPLAY_NPC, name = "Mage Guard")
                )

                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() && baseEntity.name == "Mage Outlaw" -> MobData.MobResult.found(
                    Mob(baseEntity, Mob.Type.BOSS, armorStand, name = "Mage Outlaw")
                ) // fix for wierd name
                baseEntity is EntityPigZombie && baseEntity.inventory?.get(4)
                    ?.getSkullTexture() == MobFilter.NPC_TURD_SKULL -> MobData.MobResult.found(
                    Mob(
                        baseEntity,
                        Mob.Type.DISPLAY_NPC,
                        name = "Turd"
                    )
                )

                baseEntity is EntityOcelot -> if (MobFilter.createDisplayNPC(baseEntity)) MobData.MobResult.illegal else MobData.MobResult.notYetFound // Maybe a problem in the future
                else -> null
            }

            IslandType.DEEP_CAVERNS -> when {
                baseEntity is EntityCreeper && baseEntity.baseMaxHealth.derpy() == 120 -> MobData.MobResult.found(
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
                baseEntity is EntityCreeper && baseEntity.baseMaxHealth.derpy() == 1_000_000 -> MobData.MobResult.found(
                    MobFactories.basic(baseEntity, "Ghost")
                )

                else -> null
            }

            IslandType.CRYSTAL_HOLLOWS -> when {
                baseEntity is EntityMagmaCube && armorStand != null && armorStand.cleanName() == "[Lv100] Bal ???❤" -> MobData.MobResult.found(
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
                baseEntity is EntityOtherPlayerMP && baseEntity.isNPC() -> MobData.MobResult.found(
                    Mob(
                        baseEntity,
                        Mob.Type.DISPLAY_NPC,
                        name = baseEntity.cleanName()
                    )
                )

                else -> null
            }

            IslandType.KUUDRA_ARENA -> when {
                baseEntity is EntityMagmaCube && nextEntity is EntityMagmaCube -> MobData.MobResult.illegal
                baseEntity is EntityZombie && nextEntity is EntityZombie -> MobData.MobResult.illegal
                baseEntity is EntityZombie && nextEntity is EntityGiantZombie -> MobData.MobResult.illegal
                else -> null
            }

            IslandType.WINTER -> when {
                baseEntity is EntityMagmaCube && MobFilter.jerryMagmaCubePattern.matches(
                    MobUtils.getArmorStand(
                        baseEntity,
                        2
                    )?.name
                ) ->
                    MobData.MobResult.found(
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

    private fun petCareHandler(baseEntity: EntityLivingBase): MobData.MobResult {
        val extraEntityList = listOf(1, 2, 3, 4).mapNotNull { MobUtils.getArmorStand(baseEntity, it) }
        if (extraEntityList.size != 4) return MobData.MobResult.notYetFound
        return MobFilter.petCareNamePattern.matchMatcher(extraEntityList[1].cleanName()) {
            MobData.MobResult.found(
                Mob(
                    baseEntity,
                    Mob.Type.SPECIAL,
                    armorStand = extraEntityList[1],
                    name = this.group("name"),
                    additionalEntities = extraEntityList,
                    levelOrTier = this.group("level").toInt()
                ),
            )
        } ?: MobData.MobResult.somethingWentWrong
    }

    private const val ratSearchStart = 1
    private const val ratSearchUpTo = 11

    private fun ratHandler(baseEntity: EntityZombie, nextEntity: EntityLivingBase?): MobData.MobResult? =
        generateSequence(ratSearchStart) { it + 1 }.take(ratSearchUpTo - ratSearchStart + 1).map { i ->
            MobUtils.getArmorStand(
                baseEntity, i
            )
        }.firstOrNull {
            it != null && it.distanceTo(baseEntity) < 4.0 && it.inventory?.get(4)
                ?.getSkullTexture() == MobFilter.RAT_SKULL
        }?.let {
            MobData.MobResult.found(
                Mob(
                    baseEntity = baseEntity,
                    mobType = Mob.Type.BASIC,
                    armorStand = it,
                    name = "Rat"
                )
            )
        }
            ?: if (nextEntity is EntityZombie) MobData.MobResult.notYetFound else null
}
