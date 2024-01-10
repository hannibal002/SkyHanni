package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

object UtilsPatterns {
    private val patternGroup = RepoPattern.group("utils")
    val petLevelPattern by patternGroup.pattern(
        "item.petlevel",
        "(?:§f§f)?§7\\[Lvl (?<level>\\d+)] .*"
    )
    val timeAmountPattern by patternGroup.pattern(
        "time.amount",
        "(?:(?<y>\\d+) ?y(?:\\w* ?)?)?(?:(?<d>\\d+) ?d(?:\\w* ?)?)?(?:(?<h>\\d+) ?h(?:\\w* ?)?)?(?:(?<m>\\d+) ?m(?:\\w* ?)?)?(?:(?<s>\\d+) ?s(?:\\w* ?)?)?"
    )

}
