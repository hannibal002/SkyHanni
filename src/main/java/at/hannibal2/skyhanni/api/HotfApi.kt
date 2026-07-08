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
        override val perkDescription: String,
        @field:Language("RegExp") val chatFallback: String,
        @field:Language("RegExp") val itemFallback: String,
    ) : RotatingPerk {
        SWEEP(
            perkDescription = "+5% ∮ Sweep",
            chatFallback = """Gain \+5% ∮ Sweep\.""",
            itemFallback = """Gain \+5% ∮ Sweep\.""",
        ),
        MANGROVE_FORTUNE(
            perkDescription = "+50 ☘ Mangrove Fortune",
            chatFallback = """Gain \+50 ☘ Mangrove Fortune\.""",
            itemFallback = """Gain \+50 ☘ Mangrove Fortune\.""",
        ),
        FIG_FORTUNE(
            perkDescription = "+50 ☘ Fig Fortune",
            chatFallback = """Gain \+50 ☘ Fig Fortune\.""",
            itemFallback = """Gain \+50 ☘ Fig Fortune\.""",
        ),
        ;

        private val basePath = "foraging.hotf.lottery"
        override val chatPattern by RepoPattern.pattern("$basePath.chat.${asPatternId()}", chatFallback)
        override val itemPattern by RepoPattern.pattern("$basePath.item.${asPatternId()}", itemFallback)
    }
}
