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
            displayDescription = "§a+5% §r§2${SkyblockStat.SWEEP.hypixelIcon} Sweep",
            chatFallback = "Gain \\+5% ${SkyblockStat.SWEEP.hypixelIcon} Sweep\\.",
            itemFallback = "Gain \\+5% ${SkyblockStat.SWEEP.hypixelIcon} Sweep\\.",
        ),
        MANGROVE_FORTUNE(
            displayDescription = "§a+50 §r§6${SkyblockStat.MANGROVE_FORTUNE.hypixelIcon} Mangrove Fortune",
            chatFallback = "Gain \\+50 ${SkyblockStat.MANGROVE_FORTUNE.hypixelIcon} Mangrove Fortune\\.",
            itemFallback = "Gain \\+50 ${SkyblockStat.MANGROVE_FORTUNE.hypixelIcon} Mangrove Fortune\\.",
        ),
        FIG_FORTUNE(
            displayDescription = "§a+50 §r§6${SkyblockStat.FIG_FORTUNE.hypixelIcon} Fig Fortune",
            chatFallback = "Gain \\+50 ${SkyblockStat.FIG_FORTUNE.hypixelIcon} Fig Fortune\\.",
            itemFallback = "Gain \\+50 ${SkyblockStat.FIG_FORTUNE.hypixelIcon} Fig Fortune\\.",
        ),
        ;

        private val basePath = "foraging.hotf.lottery"
        override val chatPattern by RepoPattern.pattern("$basePath.chat.${asPatternId()}", chatFallback)
        override val itemPattern by RepoPattern.pattern("$basePath.item.${asPatternId()}", itemFallback)
    }
}
