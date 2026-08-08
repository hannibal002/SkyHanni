package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object ScoreboardPattern {
    private val group = RepoPattern.group("features.gui.customscoreboard")

    // Lines from the scoreboard
    private val scoreboardGroup by group.exclusiveGroup("scoreboard")

    // Main scoreboard
    private val mainSB = scoreboardGroup.group("main")

    /**
     * REGEX-TEST: Motes: §5137,242
     */
    val motesPattern by mainSB.pattern(
        "motes",
        "(?:§.)*Motes: (?:§.)*(?<motes>[\\d,]+).*",
    )

    // dungeon scoreboard
    private val dungeonSB = scoreboardGroup.group("dungeon")
    /**
     * REGEX-TEST: §a[H] §6Eisengolem §7[Lv48]
     * REGEX-TEST: §e[M] §b04032006 §a7,361§c❤
     */
    @Suppress("MaxLineLength")
    val teammatesPattern by dungeonSB.pattern(
        "teammates",
        "(?:§.)*(?<classAbbv>\\[\\w]) (?:§.)*(?<username>\\w{2,16}) (?:(?:§.)*(?<classLevel>\\[Lvl?(?<level>[\\w,.]+)?]?)|(?:§(?<color>.))*(?<health>[\\w,.]+)(?:§.)*.?)",
    )

    // mining
    private val miningSB = scoreboardGroup.group("mining")

    /**
     * REGEX-TEST: §2᠅ §fMithril§f: §235,448
     * REGEX-TEST: §d᠅ §fGemstone§f: §d36,758
     * REGEX-TEST: §b᠅ §fGlacite§f: §b29,537
     * REGEX-TEST: §2᠅ §fMithril Powder§f: §235,448
     * REGEX-TEST: §d᠅ §fGemstone Powder§f: §d36,758
     * REGEX-TEST: §b᠅ §fGlacite Powder§f: §b29,537
     */
    val powderPattern by miningSB.pattern(
        "powder",
        "(?:§.)*᠅ §.(?<type>Gemstone|Mithril|Glacite)(?: Powder)?(?:§.)*:? (?:§.)*(?<amount>[\\d,.]*)",
    )
}
