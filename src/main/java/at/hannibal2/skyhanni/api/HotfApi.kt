package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.data.hotx.HotxPatterns.asPatternId
import at.hannibal2.skyhanni.data.hotx.RotatingPerk
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import org.intellij.lang.annotations.Language

@SkyHanniModule
object HotfApi {

    var lottery: LotteryPerk? = null

    enum class LotteryPerk(
        override val displayDescription: String,
        @field:Language("RegExp") val chatFallback: String,
        @field:Language("RegExp") val itemFallback: String,
    ) : RotatingPerk {
        SWEEP(
            displayDescription = "§2+10${SkyblockStat.SWEEP} Sweep",
            chatFallback = "Gain \\+10${SkyblockStat.SWEEP} Sweep\\.",
            itemFallback = "Gain \\+10${SkyblockStat.SWEEP} Sweep\\.",
        ),
        MANGROVE_FORTUNE(
            displayDescription = "§6+50${SkyblockStat.MANGROVE_FORTUNE} Mangrove Fortune",
            chatFallback = "Gain \\+50${SkyblockStat.MANGROVE_FORTUNE} Mangrove Fortune\\.",
            itemFallback = "Gain \\+50${SkyblockStat.MANGROVE_FORTUNE} Mangrove Fortune\\.",
        ),
        FIG_FORTUNE(
            displayDescription = "§6+50${SkyblockStat.FIG_FORTUNE} Fig Fortune",
            chatFallback = "Gain \\+50${SkyblockStat.FIG_FORTUNE} Fig Fortune\\.",
            itemFallback = "Gain \\+50${SkyblockStat.FIG_FORTUNE} Fig Fortune\\.",
        ),
        HELIX_FORTUNE(
            displayDescription = "§6+50${SkyblockStat.HELIX_FORTUNE} Helix Fortune",
            chatFallback = "Gain \\+50${SkyblockStat.HELIX_FORTUNE} Helix Fortune\\.",
            itemFallback = "Gain \\+50${SkyblockStat.HELIX_FORTUNE} Helix Fortune\\.",
        ),
        ;

        private val basePath = "foraging.hotf.lottery"
        override val chatPattern by RepoPattern.pattern("$basePath.chat.${asPatternId()}", chatFallback)
        override val itemPattern by RepoPattern.pattern("$basePath.item.${asPatternId()}", itemFallback)
    }
}
