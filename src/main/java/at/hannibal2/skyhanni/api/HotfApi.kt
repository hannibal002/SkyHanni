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
        @Language("RegExp") val chatFallback: String,
        @Language("RegExp") val itemFallback: String,
    ) : RotatingPerk {
        SWEEP(
            perkDescription = "§a+5% §r§2∮ Sweep",
            chatFallback = "Gain §r§a\\+5% §r§2∮ Sweep§r§f\\.",
            itemFallback = "Gain §a\\+5% §2∮ Sweep§7\\.",
        ),
        MANGROVE_FORTUNE(
            perkDescription = "§a+50 §r§6☘ Mangrove Fortune",
            chatFallback = "Gain §r§a\\+50 §r§6☘ Mangrove Fortune§r§f\\.",
            itemFallback = "Gain §a\\+50 §6☘ Mangrove Fortune§7\\.",
        ),
        FIG_FORTUNE(
            perkDescription = "§a+50 §r§6☘ Fig Fortune",
            chatFallback = "Gain §r§a\\+50 §r§6☘ Fig Fortune§r§f\\.",
            itemFallback = "Gain §a\\+50 §6☘ Fig Fortune§7\\.",
        ),
        ;

        private val basePath = "foraging.hotf.lottery"
        override val chatPattern by RepoPattern.pattern("$basePath.chat.${asPatternId()}", chatFallback)
        override val itemPattern by RepoPattern.pattern("$basePath.item.${asPatternId()}", itemFallback)
    }

}
