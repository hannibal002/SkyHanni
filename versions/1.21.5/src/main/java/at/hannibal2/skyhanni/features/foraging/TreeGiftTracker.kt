package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object TreeGiftTracker {

    private val config get() = SkyHanniMod.feature.foraging
    private val patternGroup = RepoPattern.group("foraging.treegift")

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: §9§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬
     */
    private val openCloseRewardPattern by patternGroup.pattern(
        "open-close-reward",
        "§9§l▬{64}"
    )

    /**
     * REGEX-TEST:                  §r§7You helped cut §r§a100% §r§7of the §r§aFig Tree§r§7.
     * REGEX-TEST:              §r§7You helped cut §r§a100% §r§7of the §r§aMangrove Tree§r§7.
     * REGEX-TEST:                  §r§7You helped cut §r§c15.2% §r§7of the §r§aFig Tree§r§7.
     */
    private val percentageContributedPattern by patternGroup.pattern(
        "contribution-percentage",
        " *(?:§.)+You helped cut (?:§.)+(?<percentage>[\\d.]+)% (?:§.)+of the (?:§.)+(?<type>.*) Tree(?:§.)+\\."
    )

    /**
     * REGEX-TEST: §f                       §e+5 rewards gained! §8(hover)
     */
    private val rewardsGainedPattern by patternGroup.pattern(
        "rewards-gained",
        "(?:§.)+ *(?:§.)+\\+(?<count>[\\d,]+) rewards gained! (?:§.)+\\(hover\\)"
    )

    /**
     * REGEX-TEST:                                 §r§d§lBONUS GIFT
     */
    private val bonusGiftSeparatorPattern by patternGroup.pattern(
        "bonus-gift-separator",
        " *(?:§.)+BONUS GIFT"
    )
    // </editor-fold>

}
