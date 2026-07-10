package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.data.hotx.HotxPatterns.asPatternId
import at.hannibal2.skyhanni.data.hotx.RotatingPerk
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
            displayDescription = "§a+5% §r§2\uE023 Sweep",
            chatFallback = "Gain \\+5% \uE023 Sweep\\.",
            itemFallback = "Gain \\+5% \uE023 Sweep\\.",
        ),
        MANGROVE_FORTUNE(
            displayDescription = "§a+50 §r§6\uE054 Mangrove Fortune",
            chatFallback = "Gain \\+50 \uE054 Mangrove Fortune\\.",
            itemFallback = "Gain \\+50 \uE054 Mangrove Fortune\\.",
        ),
        FIG_FORTUNE(
            displayDescription = "§a+50 §r§6\uE054 Fig Fortune",
            chatFallback = "Gain \\+50 \uE054 Fig Fortune\\.",
            itemFallback = "Gain \\+50 \uE054 Fig Fortune\\.",
        ),
        ;

        private val basePath = "foraging.hotf.lottery"
        override val chatPattern by RepoPattern.pattern("$basePath.chat.${asPatternId()}", chatFallback)
        override val itemPattern by RepoPattern.pattern("$basePath.item.${asPatternId()}", itemFallback)
    }
}
