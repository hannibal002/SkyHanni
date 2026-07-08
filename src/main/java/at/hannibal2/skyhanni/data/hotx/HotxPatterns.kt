package at.hannibal2.skyhanni.data.hotx

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object HotxPatterns {

    private val patternGroup = RepoPattern.group("misc.hotx")

    /**
     * REGEX-TEST: New buff: Gain +5% ∮ Sweep.
     * REGEX-TEST: New buff: Gain +50 ☘ Mangrove Fortune.
     * REGEX-TEST: New buff: Gain +50 ☘ Fig Fortune.
     * REGEX-TEST: ■ Gain +5% ∮ Sweep.
     * REGEX-TEST: ■ Gain +50 ☘ Mangrove Fortune.
     * REGEX-TEST: ■ Gain +50 ☘ Fig Fortune.
     *
     * REGEX-TEST: New buff: Gain +50☘ Mining Fortune.
     * REGEX-TEST: ■ Gain +100⸕ Mining Speed.
     * REGEX-TEST: ■ Gain +50☘ Mining Fortune.
     * REGEX-TEST: ■ Gain +15% more Powder while mining.
     * REGEX-TEST: ■ -20% Pickaxe Ability cooldowns.
     * REGEX-TEST: ■ 10x chance to find Golden and
     * REGEX-TEST: ■ Gain 5x Titanium drops.
     */
    val rotatingPerkPattern by patternGroup.pattern(
        "perk.generic",
        """(?:New buff: |■ )(?<perk>.*)"""
    )

    // The line that appears before the "current" perk effect in the item tooltip.
    val itemPreEffectPattern by patternGroup.pattern(
        "perk.item.before",
        "Your Current Effect"
    )

    fun Enum<*>.asPatternId(): String = name.lowercase().replace("_", ".")
}
