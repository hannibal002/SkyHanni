package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.data.hotx.HotfData
import at.hannibal2.skyhanni.data.hotx.HotxPatterns.asPatternId
import at.hannibal2.skyhanni.data.hotx.RotatingPerk
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import org.intellij.lang.annotations.Language

@SkyHanniModule
object HotfApi {

    val lottery: LotteryPerk? get() = HotfData.lotterySlot.currentPerk

    val beekeeper: BeekeeperPerk? get() = HotfData.beekeeperSlot.currentPerk

    enum class LotteryPerk(
        override val displayDescription: String,
        @field:Language("RegExp") val chatFallback: String,
        @field:Language("RegExp") val itemFallback: String,
    ) : RotatingPerk {
        SWEEP(
            displayDescription = "§2+10${SkyblockStat.SWEEP.hypixelIcon} Sweep",
            chatFallback = "Gain \\+10${SkyblockStat.SWEEP.hypixelIcon} Sweep\\.",
            itemFallback = "Gain \\+10${SkyblockStat.SWEEP.hypixelIcon} Sweep\\.",
        ),
        MANGROVE_FORTUNE(
            displayDescription = "§6+50${SkyblockStat.MANGROVE_FORTUNE.hypixelIcon} Mangrove Fortune",
            chatFallback = "Gain \\+50${SkyblockStat.MANGROVE_FORTUNE.hypixelIcon} Mangrove Fortune\\.",
            itemFallback = "Gain \\+50${SkyblockStat.MANGROVE_FORTUNE.hypixelIcon} Mangrove Fortune\\.",
        ),
        FIG_FORTUNE(
            displayDescription = "§6+50${SkyblockStat.FIG_FORTUNE.hypixelIcon} Fig Fortune",
            chatFallback = "Gain \\+50${SkyblockStat.FIG_FORTUNE.hypixelIcon} Fig Fortune\\.",
            itemFallback = "Gain \\+50${SkyblockStat.FIG_FORTUNE.hypixelIcon} Fig Fortune\\.",
        ),
        HELIX_FORTUNE(
            displayDescription = "§6+50${SkyblockStat.HELIX_FORTUNE.hypixelIcon} Helix Fortune",
            chatFallback = "Gain \\+50${SkyblockStat.HELIX_FORTUNE.hypixelIcon} Helix Fortune\\.",
            itemFallback = "Gain \\+50${SkyblockStat.HELIX_FORTUNE.hypixelIcon} Helix Fortune\\.",
        ),
        ;

        private val basePath = "foraging.hotf.lottery"
        override val chatPattern by RepoPattern.pattern("$basePath.chat.${asPatternId()}", chatFallback)
        override val itemPattern by RepoPattern.pattern("$basePath.item.${asPatternId()}", itemFallback)
    }

    // The Critter icon is a private use character that gets lost when copying chat or lore into a text
    // report, so its position is matched permissively instead of literally.
    enum class BeekeeperPerk(
        override val displayDescription: String,
        @field:Language("RegExp") val chatFallback: String,
        @field:Language("RegExp") val itemFallback: String,
    ) : RotatingPerk {
        HONEYHIVE_REFILL(
            displayDescription = "§6Honeyhives §7refill §a20% §7faster",
            chatFallback = "Honeyhives refill 20% faster\\.",
            itemFallback = "Honeyhives refill 20% faster\\.",
        ),
        CRITTER_SPEED(
            displayDescription = "§a20% §7faster §aCritters §7from §6Honeycomb",
            chatFallback = "Trees lathered with Honeycomb attract\\s*\\S?\\s*Critters 20% faster\\.",
            itemFallback = "Trees lathered with Honeycomb attract\\s*\\S?\\s*Critters 20% faster\\.",
        ),
        DOUBLE_HONEYCOMB(
            displayDescription = "§a2x §6Honeycomb",
            chatFallback = "Gain 2x Honeycomb from Honeyhives and Trees lathered with Honeycomb\\.",
            itemFallback = "Gain 2x Honeycomb from Honeyhives and Trees lathered with Honeycomb\\.",
        ),
        SECOND_CRITTER(
            displayDescription = "§a25% §7chance for a second §aCritter",
            chatFallback = "Trees lathered with Honeycomb have a 25% chance to attract a second\\s*\\S?\\s*Critter\\.",
            itemFallback = "Trees lathered with Honeycomb have a 25% chance to attract a second\\s*\\S?\\s*Critter\\.",
        ),
        ;

        private val basePath = "foraging.hotf.beekeeper"
        override val chatPattern by RepoPattern.pattern("$basePath.chat.${asPatternId()}", chatFallback)
        override val itemPattern by RepoPattern.pattern("$basePath.item.${asPatternId()}", itemFallback)
    }
}
