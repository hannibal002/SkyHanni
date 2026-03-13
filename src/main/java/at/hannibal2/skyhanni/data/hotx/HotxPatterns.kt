package at.hannibal2.skyhanni.data.hotx

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object HotxPatterns {

    private val patternGroup = RepoPattern.group("misc.hotx")

    /**
     * @regexTest §eNew buff§r§r§r: §r§fGain §r§a+5% §r§2∮ Sweep§r§f.
     * @regexTest §eNew buff§r§r§r: §r§fGain §r§a+50 §r§6☘ Mangrove Fortune§r§f.
     * @regexTest §eNew buff§r§r§r: §r§fGain §r§a+50 §r§6☘ Fig Fortune§r§f.
     * @regexTest §8 ■ §7Gain §a+5% §2∮ Sweep§7.
     * @regexTest §8 ■ §7Gain §a+50 §6☘ Mangrove Fortune§7.
     * @regexTest §8 ■ §7Gain §a+50 §6☘ Fig Fortune§7
     *
     * @regexTest §eNew buff§r§r§r: §r§fGain §r§6+50☘ Mining Fortune§r§f.
     * @regexTest §8 ■ §7Gain §6+100⸕ Mining Speed§7.
     * @regexTest §8 ■ §7Gain §6+50☘ Mining Fortune§7.
     * @regexTest §8 ■ §7Gain §a+15% §7more Powder while mining.
     * @regexTest §8 ■ §7§a-20%§7 Pickaxe Ability cooldowns.
     * @regexTest §8 ■ §7§a10x §7chance to find Golden and
     * @regexTest §8 ■ §7Gain §a5x §9Titanium §7drops.
     */
    val rotatingPerkPattern by patternGroup.pattern(
        "perk.generic",
        "(?:§eNew buff§r§r§r: §r§f|§8 ■ §7)(?<perk>.*)"
    )

    // The line that appears before the "current" perk effect in the item tooltip.
    val itemPreEffectPattern by patternGroup.pattern(
        "perk.item.before",
        "§aYour Current Effect"
    )

    fun Enum<*>.asPatternId(): String = name.lowercase().replace("_", ".")
}
