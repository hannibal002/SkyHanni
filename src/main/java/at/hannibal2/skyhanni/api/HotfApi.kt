package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.data.hotx.ChatRepoPatternEnum
import at.hannibal2.skyhanni.data.hotx.ItemRepoPatternEnum
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import org.intellij.lang.annotations.Language

@SkyHanniModule
object HotfApi {

    var lottery: LotteryPerk? = null

    enum class LotteryPerk(
        @Language("RegExp") override val chatPatternRaw: String,
        @Language("RegExp") override val itemPatternRaw: String,
    ) : ChatRepoPatternEnum, ItemRepoPatternEnum {
        SWEEP(
            chatPatternRaw = "Gain §r§a\\+5% §r§2∮ Sweep§r§f\\.",
            itemPatternRaw = "Gain §a\\+5% §2∮ Sweep§7\\."
        ),
        MANGROVE_FORTUNE(
            chatPatternRaw = "Gain §r§a\\+50 §r§6☘ Mangrove Fortune§r§f\\.",
            itemPatternRaw = "Gain §a\\+50 §6☘ Mangrove Fortune§7\\."
        ),
        FIG_FORTUNE(
            chatPatternRaw = "Gain §r§a\\+50 §r§6☘ Fig Fortune§r§f\\.",
            itemPatternRaw = "Gain §a\\+50 §6☘ Fig Fortune§7\\."
        ),
        ;

        override val basePath = "foraging.hotf.lottery"
        override val chatPattern by RepoPattern.pattern("$basePath.chat.$patternId", chatPatternRaw)
        override val itemPattern by RepoPattern.pattern("$basePath.item.$patternId", itemPatternRaw)
    }

}
