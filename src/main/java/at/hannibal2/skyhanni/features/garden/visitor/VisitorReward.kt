package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.config.features.garden.visitor.DropsStatisticsConfig
import at.hannibal2.skyhanni.utils.AutoUpdatingItemStack
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

private typealias StatsEntry = DropsStatisticsConfig.DropsStatisticsTextEntry

enum class VisitorReward(rawInternalName: String, val displayName: String) {
    FLOWERING_BOUQUET("FLOWERING_BOUQUET", "§9Flowering Bouquet"),
    OVERGROWN_GRASS("OVERGROWN_GRASS", "§9Overgrown Grass"),
    GREEN_BANDANA("GREEN_BANDANA", "§9Green Bandana"),
    DEDICATION("DEDICATION;4", "§9Dedication IV"),
    MUSIC_RUNE("MUSIC_RUNE;1", "§9Music Rune"),
    SPACE_HELMET("DCTR_SPACE_HELM", "§cSpace Helmet"),
    CULTIVATING("CULTIVATING;1", "§9Cultivating I"),
    REPLENISH("REPLENISH;1", "§9Replenish I"),
    DELICATE("DELICATE;5", "§9Delicate V"),
    COPPER_DYE("DYE_COPPER", "§8Copper Dye"),
    JUNGLE_KEY("JUNGLE_KEY", "§5Jungle Key"),
    FRUIT_BOWL("FRUIT_BOWL", "§9Fruit Bowl"),
    HARVEST_HARBINGER("POTION_HARVEST_HARBINGER;5", "§9Harvest Harbinger V"),
    HYPERCHARGE_CHIP("HYPERCHARGE_GARDEN_CHIP", "§9Hypercharge Chip"),
    QUICKDRAW_CHIP("QUICKDRAW_GARDEN_CHIP", "§9Quickdraw Chip"),
    FARMING_EXP_BOOST_EPIC("PET_ITEM_FARMING_SKILL_BOOST_EPIC", "§5Farming Exp Boost"),
    UNFULFILLED_JERRYSEED("UNFULFILLED_JERRYSEED", "§aUnfulfilled Jerryseed"),
    VOTER_BADGE("VOTER_BADGE", "§fVoter's Badge"),
    VOTER_BADGE_VIP("VOTER_BADGE_VIP", "§aVIP Voter's Badge"),
    VOTER_BADGE_ELITE("VOTER_BADGE_ELITE", "§9Elite Voter's Badge"),
    VOTER_BADGE_SUPREME("VOTER_BADGE_SUPREME", "§5Supreme Voter's Badge"),
    ;

    private val internalName = rawInternalName.toInternalName()
    val itemStack by AutoUpdatingItemStack(internalName)
    // TODO use this instead of hard coded item names once moulconfig no longer calls toString before the neu repo gets loaded
//     val displayName by lazy { itemStack.nameWithEnchantment ?: internalName.asString() }

    companion object {
        fun getByInternalName(internalName: NeuInternalName) = entries.firstOrNull { it.internalName == internalName }
    }

    // Todo: Remove this when enum names of this and DropsStatisticsTextEntry are in sync
    fun toStatsTextEntryOrNull() = when (this) {
        DEDICATION -> StatsEntry.DEDICATION_IV
        MUSIC_RUNE -> StatsEntry.MUSIC_RUNE_I
        CULTIVATING -> StatsEntry.CULTIVATING_I
        REPLENISH -> StatsEntry.REPLENISH_I
        else -> {
            try {
                StatsEntry.valueOf(name)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    override fun toString() = displayName
}
