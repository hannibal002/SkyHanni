package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.UtilsPatterns.seasonPattern
import kotlin.time.Duration.Companion.seconds

enum class SkyblockSeason(
    val season: String,
    val perk: String,
    private val abbreviatedPerk: String,
    private val middleMonth: Int, // 0 indexed
) {

    SPRING("§dSpring", "§7Gain §6+25☘ Farming Fortune§7.", "§6+25☘", 1),
    SUMMER("§6Summer", "§7Gain §3+20☯ Farming Wisdom§7.", "§3+20☯", 4),
    AUTUMN("§eAutumn", "§4Pests §7spawn §a15% §7more often.", "§a15%+§4ൠ", 7),
    WINTER("§9Winter", "§7Visitors give §a5% §7more §cCopper.", "§a5%+§cC", 10),
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
    }
}

enum class SkyblockSeasonModifier(private val displayName: String) {
    EARLY("Early"),
    NONE(""),
    LATE("Late"),
    ;

    override fun toString(): String = displayName
}
