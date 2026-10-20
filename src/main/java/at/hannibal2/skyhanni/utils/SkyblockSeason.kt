package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.UtilsPatterns.seasonPattern
import kotlin.time.Duration.Companion.seconds

enum class SkyblockSeason(
    val season: String,
    val perk: String,
    private val abbreviatedPerk: String,
    private val middleMonth: Int, // 0 indexed
) {

    SPRING(
        "§dSpring",
        "§7Gain §6+25${SkyblockStat.FARMING_FORTUNE.hypixelIcon} Farming Fortune§7.",
        "§6+25${SkyblockStat.FARMING_FORTUNE.hypixelIcon}",
        1
    ),
    SUMMER(
        "§6Summer",
        "§7Gain §3+20${SkyblockStat.FARMING_WISDOM.hypixelIcon} Farming Wisdom§7.",
        "§3+20${SkyblockStat.FARMING_WISDOM.hypixelIcon}",
        4
    ),
    AUTUMN(
        "§eAutumn",
        "§4Pests §7spawn §a15% §7more often.",
        "§a15%+§\uE07F",
        7
    ),
    WINTER(
        "§9Winter",
        "§7Visitors give §a5% §7more §cCopper.",
        "§a5%+§cC",
        10
    ),
    ;

    override fun toString(): String = season

    fun isSeason(): Boolean = currentSeason == this
    fun getPerk(abbreviate: Boolean): String = if (abbreviate) abbreviatedPerk else perk
    fun getSeasonName(abbreviate: Boolean): String = if (abbreviate) season.take(4) else season

    fun getMonth(modifier: SkyblockSeasonModifier? = null): Int = middleMonth + when (modifier) {
        SkyblockSeasonModifier.EARLY -> -1
        SkyblockSeasonModifier.LATE -> 1
        else -> 0
    }

    companion object {
        val currentSeason by RecalculatingValue(1.seconds) {
            getSeasonByName(SkyBlockTime.now().monthName)
        }

        private fun getSeasonByName(name: String): SkyblockSeason? =
            seasonPattern.matchMatcher(name) { entries.find { it.season.endsWith(group("season")) } }

        fun getSeasonByMonth(month: Int): Pair<SkyblockSeason, SkyblockSeasonModifier?> {
            val season = when ((month - 1) / 3) {
                0 -> SPRING
                1 -> SUMMER
                2 -> AUTUMN
                3 -> WINTER
                else -> throw IllegalArgumentException("Invalid month: $month")
            }

            val modifier = when ((month - 1) % 3) {
                0 -> SkyblockSeasonModifier.EARLY
                1 -> null
                2 -> SkyblockSeasonModifier.LATE
                else -> throw IllegalArgumentException("Invalid month: $month")
            }

            return season to modifier
        }
    }
}

enum class SkyblockSeasonModifier(private val displayName: String) {
    EARLY("Early"),
    NONE(""),
    LATE("Late"),
    ;

    override fun toString(): String = displayName
}
