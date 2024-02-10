package at.hannibal2.skyhanni.features.itemabilities.abilitycooldown

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class ItemAbility(
    @Deprecated("just dont use it", ReplaceWith("cooldown"))
    private val cooldownInSeconds: Int,
    val manaCost: Int,
    vararg alternateInternalNames: String,

    var lastActivation: SimpleTimeMark = SimpleTimeMark.farPast(),
    var specialColor: LorenzColor? = null,
    var lastItemClick: SimpleTimeMark = SimpleTimeMark.farPast(),

    val alternativePosition: Boolean = false,
    val ignoreMageCooldownReduction: Boolean = false,
    val remainingMana: (Int) -> Int = { it - manaCost },
    val riftManaCost: Int = manaCost,
    val remainingManaRift: (Int) -> Int = { it - riftManaCost },
    private val cooldown: Duration = cooldownInSeconds.seconds,
    val allowRecastAfter: Duration = cooldown,
    val isAllowed: (Unit) -> Boolean = { true },
) {

    HYPERION(5, 300, "SCYLLA", "VALKYRIE", "ASTRAEA", ignoreMageCooldownReduction = true),
    GYROKINETIC_WAND_LEFT(30, 1200, "GYROKINETIC_WAND", alternativePosition = true),
    GYROKINETIC_WAND_RIGHT(10, 220, "GYROKINETIC_WAND", isAllowed = {
        LorenzUtils.inDungeons || LorenzUtils.inKuudraFight
    }),
    GIANTS_SWORD(30, 100),
    ICE_SPRAY_WAND(5, 50),
    ATOMSPLIT_KATANA(4, 200, "VORPAL_KATANA", "VOIDEDGE_KATANA", ignoreMageCooldownReduction = true),
    RAGNAROCK_AXE(20, 500),

    // todo support for mana disintegrator
    WAND_OF_ATONEMENT(7, 240, "WAND_OF_HEALING", "WAND_OF_MENDING", "WAND_OF_RESTORATION"),
    GOLEM_SWORD(3, 70),
    END_STONE_SWORD(5, 0, remainingMana = { 0 }, isAllowed = {
        !LorenzUtils.inDungeons
    }),

    // TODO show while the ability is running
    SOUL_ESOWARD(20, 350),

    PIGMAN_SWORD(5, 400),
    EMBER_ROD(30, 150),
    STAFF_OF_THE_VOLCANO(30, 100),
    STARLIGHT_WAND(2, 120),
    VOODOO_DOLL(5, 200),
    WEIRD_TUBA(20, 150, riftManaCost = 60),
    WEIRDER_TUBA(30, 120, riftManaCost = 60, allowRecastAfter = 20.seconds),
    FIRE_FREEZE_STAFF(10, 500, allowRecastAfter = 0.seconds),
    SWORD_OF_BAD_HEALTH(5, 0),
    WITHER_CLOAK(10, 0),
    HOLY_ICE(4, 20),
    VOODOO_DOLL_WILTED(3, 180),
    FIRE_FURY_STAFF(20, 1000, allowRecastAfter = 0.seconds),
    SHADOW_FURY(15, 0, "STARRED_SHADOW_FURY"),
    ENDER_BOW(30, 50),
    LIVID_DAGGER(5, 150),
    FIRE_VEIL_WAND(5, 300, allowRecastAfter = 0.seconds),
    INK_WAND(30, 60),
    ROGUE_SWORD(30, 50, ignoreMageCooldownReduction = true),
    TALBOTS_THEODOLITE(10, 98),
    ANCESTRAL_SPADE(3, 98, isAllowed = {
//         IslandType.HUB.isInIsland() && DianaAPI.isRitualActive()
        IslandType.HUB.isInIsland()
    }),
    MOODY_GRAPPLESHOT(1, 30),
    FLOWER_OF_TRUTH(1, 0),
    BOUQUET_OF_LIES(1, 0),
    WAND_OF_VOLCANO(1, 0),
    ASPECT_OF_THE_END(0, 50),
    ASPECT_OF_THE_VOID(0, 45),
    AURORA_STAFF(1, 0),

    // TODO incoming damage reduction for 10s
    ENRAGER(500, 20),
    ;

    var newVariant = false
    val internalNames: List<NEUInternalName>

    init {
        val internalNames = mutableListOf<NEUInternalName>()
        alternateInternalNames.forEach {
            internalNames.add(it.asInternalName())
        }
        internalNames.add(name.asInternalName())
        this.internalNames = internalNames
    }

    fun activate(color: LorenzColor? = null, customCooldown: Duration = (cooldown)) {
        specialColor = color
        lastActivation = SimpleTimeMark.now() - (cooldown - customCooldown)
    }

    fun isOnCooldown(): Boolean = getDuration().isPositive()

    fun getCooldown(): Duration {
        // Some items aren't really a cooldown but an effect over time, so don't apply cooldown multipliers
        if (this == WAND_OF_ATONEMENT || this == RAGNAROCK_AXE) {
            return cooldown
        }

        return cooldown * getMultiplier()
    }

    fun getDurationText(): String = getDuration().format(showMilliSeconds = getDuration() < 1.6.seconds).dropLast(1)

    fun getDuration() = getCooldown() - lastActivation.passedSince()

    fun setItemClick() {
        if (lastItemClick.passedSince() < allowRecastAfter) return
        lastItemClick = SimpleTimeMark.now()
        lastActivation = SimpleTimeMark.now()
    }

    companion object {

        fun getByInternalName(internalName: NEUInternalName): ItemAbility? {
            return entries.firstOrNull { internalName in it.internalNames }
        }

        fun ItemAbility.getMultiplier(): Double {
            return getMageCooldownReduction() ?: 1.0
        }

        private fun ItemAbility.getMageCooldownReduction(): Double? {
            if (ignoreMageCooldownReduction) return null
            if (!LorenzUtils.inDungeons) return null
            if (DungeonAPI.playerClass != DungeonAPI.DungeonClass.MAGE) return null

            var abilityCooldownMultiplier = 1.0
            abilityCooldownMultiplier -= if (DungeonAPI.isUniqueClass) {
                0.5 // 50% base reduction at level 0
            } else {
                0.25 // 25% base reduction at level 0
            }

            // 1% ability reduction every other level
            abilityCooldownMultiplier -= 0.01 * floor(DungeonAPI.playerClassLevel / 2f)

            return abilityCooldownMultiplier
        }
    }
}
