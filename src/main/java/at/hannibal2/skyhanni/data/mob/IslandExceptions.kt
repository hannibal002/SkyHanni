package at.hannibal2.skyhanni.data.mob

import at.hannibal2.skyhanni.data.ElectionApi.derpy
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.MobFilter.makeMobResult
import at.hannibal2.skyhanni.utils.EntityUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.isNpc
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.MobUtils
import at.hannibal2.skyhanni.utils.MobUtils.getNextEntity
import at.hannibal2.skyhanni.utils.MobUtils.isDefaultValue
import at.hannibal2.skyhanni.utils.MobUtils.takeNonDefault
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.compat.getEntityHelmet
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.animal.Ocelot
import net.minecraft.world.entity.animal.Pig
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.CaveSpider
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.monster.Giant
import net.minecraft.world.entity.monster.MagmaCube
import net.minecraft.world.entity.monster.Slime
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.entity.monster.ZombifiedPiglin

object IslandExceptions {

    internal fun islandSpecificExceptions(
        baseEntity: LivingEntity,
        armorStand: ArmorStand?,
        nextEntity: LivingEntity?,
    ): MobData.MobResult? =
        when (SkyBlockUtils.currentIsland) {
            IslandType.CATACOMBS -> dungeon(baseEntity, armorStand, nextEntity)
            IslandType.PRIVATE_ISLAND -> privateIsland(armorStand, baseEntity)
            IslandType.THE_RIFT -> theRift(baseEntity, nextEntity, armorStand)
            IslandType.CRIMSON_ISLE -> crimsonIsle(baseEntity, armorStand, nextEntity)
            IslandType.DEEP_CAVERNS -> deepCaverns(baseEntity)
            IslandType.DWARVEN_MINES -> dwarvenMines(baseEntity)
            IslandType.CRYSTAL_HOLLOWS -> crystalHollows(baseEntity, armorStand)
            IslandType.HUB -> hub(baseEntity, armorStand, nextEntity)
            IslandType.GARDEN -> garden(baseEntity)
            IslandType.KUUDRA_ARENA -> kuudraArena(baseEntity, nextEntity)
            IslandType.WINTER -> winterIsland(baseEntity)
            IslandType.GALATEA -> ModernIslandExceptions.galatea(baseEntity, armorStand, nextEntity)

            else -> null
        }

    private fun dungeon(
        baseEntity: LivingEntity,
        armorStand: ArmorStand?,
        nextEntity: LivingEntity?,
    ) = when {
        baseEntity is Zombie &&
            armorStand != null &&
            (armorStand.name.formattedTextCompatLessResets() == "§e﴾ §5♃ §c§lThe Watcher§r§r §e﴿" || armorStand.name.formattedTextCompatLessResets() == "§3§lWatchful Eye§r") ->
            MobData.MobResult.found(
                MobFactories.special(baseEntity, armorStand.cleanName(), armorStand),
            )

        baseEntity is CaveSpider -> MobUtils.getClosestArmorStand(baseEntity, 2.0).takeNonDefault()
            .makeMobResult { MobFactories.dungeon(baseEntity, it) }

        baseEntity is RemotePlayer && baseEntity.isNpc() && baseEntity.name.formattedTextCompatLessResets() == "Shadow Assassin" ->
            MobUtils.getClosestArmorStandWithName(baseEntity, 3.0, "Shadow Assassin")
                .makeMobResult { MobFactories.dungeon(baseEntity, it) }

        baseEntity is RemotePlayer && baseEntity.isNpc() && baseEntity.name.formattedTextCompatLessResets() == "The Professor" ->
            MobUtils.getArmorStand(baseEntity, 9)
                .makeMobResult { MobFactories.boss(baseEntity, it) }

        baseEntity is RemotePlayer &&
            baseEntity.isNpc() &&
            (nextEntity is Giant || nextEntity == null) &&
            baseEntity.name.formattedTextCompatLessResets().contains("Livid") -> MobUtils.getArmorStand(baseEntity, 10)
            ?.takeIf { getNextEntity(it, -1)?.takeIf { entity -> entity.name.formattedTextCompatLessResets().contains("Livid") } == null }
            .makeMobResult { MobFactories.boss(baseEntity, it, overriddenName = "Real Livid") }

        baseEntity is IronGolem && MobFilter.wokeSleepingGolemPattern.matches(armorStand?.name.formattedTextCompatLessResets().orEmpty()) ->
            MobData.MobResult.found(Mob(baseEntity, Mob.Type.DUNGEON, armorStand, "Sleeping Golem")) // Consistency fix

        else -> null
    }

    private fun privateIsland(
        armorStand: ArmorStand?,
        baseEntity: LivingEntity,
    ) = when {
        armorStand?.isDefaultValue() != false ->
            if (baseEntity.getLorenzVec().distanceChebyshevIgnoreY(LocationUtils.playerLocation()) < 15.0) {
                // TODO fix to always include Valid Mobs on Private Island
                MobData.MobResult.found(MobFactories.minionMob(baseEntity))
            } else MobData.MobResult.notYetFound

        else -> null
    }

    private fun theRift(
        baseEntity: LivingEntity,
        nextEntity: LivingEntity?,
        armorStand: ArmorStand?,
    ) = when {
        baseEntity is Slime && nextEntity is Slime ->
            MobData.MobResult.found(Mob(baseEntity, Mob.Type.SPECIAL, armorStand, "Bacte Tentacle"))

        baseEntity is Slime && armorStand != null && armorStand.cleanName().startsWith("﴾ [Lv10] B") ->
            MobData.MobResult.found(Mob(baseEntity, Mob.Type.BOSS, armorStand, name = "Bacte"))

        baseEntity is RemotePlayer && baseEntity.isNpc() && baseEntity.name.formattedTextCompatLessResets() == "Branchstrutter " ->
            MobData.MobResult.found(Mob(baseEntity, Mob.Type.DISPLAY_NPC, name = "Branchstrutter"))

        else -> null
    }

    private fun crimsonIsle(
        baseEntity: LivingEntity,
        armorStand: ArmorStand?,
        nextEntity: LivingEntity?,
    ) = when {
        baseEntity is Slime && MobFilter.heavyPearlPattern.matches(armorStand?.name.formattedTextCompatLessResets()) ->
            MobData.MobResult.found(MobFactories.special(baseEntity, "Heavy Pearl"))

        baseEntity is Pig && nextEntity is Pig -> MobData.MobResult.illegal // Matriarch Tongue
        baseEntity is RemotePlayer && baseEntity.isNpc() && baseEntity.name.formattedTextCompatLessResets() == "BarbarianGuard " ->
            MobData.MobResult.found(Mob(baseEntity, Mob.Type.DISPLAY_NPC, name = "Barbarian Guard"))

        baseEntity is RemotePlayer && baseEntity.isNpc() && baseEntity.name.formattedTextCompatLessResets() == "MageGuard " ->
            MobData.MobResult.found(Mob(baseEntity, Mob.Type.DISPLAY_NPC, name = "Mage Guard"))

        baseEntity is RemotePlayer && baseEntity.isNpc() && baseEntity.name.formattedTextCompatLessResets() == "Mage Outlaw" ->
            // fix for wierd name
            MobData.MobResult.found(Mob(baseEntity, Mob.Type.BOSS, armorStand, name = "Mage Outlaw"))

        baseEntity is ZombifiedPiglin &&
            baseEntity.getEntityHelmet()?.getSkullTexture() == MobFilter.NPC_TURD_SKULL ->
            MobData.MobResult.found(Mob(baseEntity, Mob.Type.DISPLAY_NPC, name = "Turd"))

        baseEntity is Ocelot -> if (MobFilter.createDisplayNpc(baseEntity)) {
            MobData.MobResult.illegal
        } else {
            MobData.MobResult.notYetFound // Maybe a problem in the future
        }

        else -> null
    }

    private fun deepCaverns(baseEntity: LivingEntity) = when {
        baseEntity is Creeper && baseEntity.baseMaxHealth.derpy() == 120 ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BASIC, name = "Sneaky Creeper", levelOrTier = 3),
            )

        else -> null
    }

    private fun dwarvenMines(baseEntity: LivingEntity) = when {
        baseEntity is Creeper && baseEntity.baseMaxHealth.derpy() == 1_000_000 ->
            MobData.MobResult.found(MobFactories.basic(baseEntity, "Ghost"))

        else -> null
    }

    private fun crystalHollows(
        baseEntity: LivingEntity,
        armorStand: ArmorStand?,
    ) = when {
        baseEntity is MagmaCube &&
            armorStand != null &&
            armorStand.cleanName() == "[Lv100] Bal ???❤" ->
            MobData.MobResult.found(
                Mob(baseEntity, Mob.Type.BOSS, armorStand, "Bal", levelOrTier = 100),
            )

        else -> null
    }

    private fun hub(
        baseEntity: LivingEntity,
        armorStand: ArmorStand?,
        nextEntity: LivingEntity?,
    ) = when {
        baseEntity is Ocelot &&
            armorStand?.isDefaultValue() == false &&
            // TODO fix pattern
            armorStand.name.formattedTextCompatLessResets().startsWith("§8[§7Lv155§8] §cAzrael§r") ->
            MobUtils.getArmorStand(baseEntity, 1)
                .makeMobResult { MobFactories.basic(baseEntity, it) }

        baseEntity is Ocelot && (nextEntity is Ocelot || nextEntity == null) ->
            MobUtils.getArmorStand(baseEntity, 3)
                .makeMobResult { MobFactories.basic(baseEntity, it) }

        baseEntity is RemotePlayer &&
            baseEntity.name.formattedTextCompatLessResets()
                .let { it == "Minos Champion" || it == "Minos Inquisitor" || it == "Minotaur " } &&
            armorStand != null ->
            MobUtils.getArmorStand(baseEntity, 2)
                .makeMobResult { MobFactories.basic(baseEntity, it, listOf(armorStand)) }

        baseEntity is Zombie &&
            armorStand?.isDefaultValue() == true &&
            MobUtils.getNextEntity(baseEntity, 4)?.name.formattedTextCompatLessResets()?.startsWith("§e") == true ->
            petCareHandler(baseEntity)

        baseEntity is Zombie && armorStand != null && !armorStand.isDefaultValue() -> null // Impossible Rat
        baseEntity is Zombie -> ratHandler(baseEntity, nextEntity) // Possible Rat
        baseEntity is Pig && MobFilter.shinyPig.matches(armorStand?.cleanName()) -> MobData.MobResult.found(
            Mob(
                baseEntity,
                Mob.Type.SPECIAL,
                armorStand,
                "SHINY PIG",
            ),
        )

        else -> null
    }

    private fun garden(baseEntity: LivingEntity) = when {
        baseEntity is RemotePlayer && baseEntity.isNpc() ->
            MobData.MobResult.found(Mob(baseEntity, Mob.Type.DISPLAY_NPC, name = baseEntity.cleanName()))

        else -> null
    }

    private fun kuudraArena(
        baseEntity: LivingEntity,
        nextEntity: LivingEntity?,
    ) = when {
        baseEntity is MagmaCube && nextEntity is MagmaCube -> MobData.MobResult.illegal
        baseEntity is Zombie && nextEntity is Zombie -> MobData.MobResult.illegal
        baseEntity is Zombie && nextEntity is Giant -> MobData.MobResult.illegal

        else -> null
    }

    private fun winterIsland(baseEntity: LivingEntity): MobData.MobResult? {
        val armorStand = MobUtils.getArmorStand(baseEntity, 2)
        return when {
            baseEntity is MagmaCube &&
                MobFilter.jerryMagmaCubePattern.matches(armorStand?.name.formattedTextCompatLessResets()) ->
                MobData.MobResult.found(Mob(baseEntity, Mob.Type.BOSS, armorStand, "Jerry Magma Cube"))

            else -> null
        }
    }

    private const val RAT_SEARCH_START = 1
    private const val RAT_SEARCH_UP_TO = 11

    private fun ratHandler(baseEntity: Zombie, nextEntity: LivingEntity?): MobData.MobResult? =
        generateSequence(RAT_SEARCH_START) { it + 1 }
            .take(RAT_SEARCH_UP_TO - RAT_SEARCH_START + 1)
            .map { i -> MobUtils.getArmorStand(baseEntity, i) }
            .firstOrNull {
                it != null &&
                    it.distanceTo(baseEntity) < 4.0 &&
                    it.wearingSkullTexture(MobFilter.RAT_SKULL_TEXTURE)
            }?.let {
                MobData.MobResult.found(Mob(baseEntity, mobType = Mob.Type.BASIC, armorStand = it, name = "Rat"))
            } ?: if (nextEntity is Zombie) MobData.MobResult.notYetFound else null

    private fun petCareHandler(baseEntity: LivingEntity): MobData.MobResult {
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
                    levelOrTier = this.group("level").toInt(),
                ),
            )
        } ?: MobData.MobResult.somethingWentWrong
    }
}
