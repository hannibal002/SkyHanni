package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

object UtilsPatterns {
    private val patternGroup = RepoPattern.group("utils")
    val petLevelPattern by patternGroup.pattern(
        "item.petlevel",
        "(?:§f§f)?§7\\[Lvl (?<level>\\d+)] .*"
    )
    val amountFrontPattern by patternGroup.pattern(
        "item.amount.front",
        "(?: *§8(\\+§\\w)?(?<amount>[\\d.km,]+)(x )?)?(?<name>.*)"
    )
    val amountBehindPattern by patternGroup.pattern(
        "item.amount.behind",
        "(?<name>(?:['\\w-]+ ?)+)(?:§8x(?<amount>[\\d,]+))?"
    )

    val timeAmountPattern by patternGroup.pattern(
        "time.amount",
        "(?:(?<y>\\d+) ?y(?:\\w* ?)?)?(?:(?<d>\\d+) ?d(?:\\w* ?)?)?(?:(?<h>\\d+) ?h(?:\\w* ?)?)?(?:(?<m>\\d+) ?m(?:\\w* ?)?)?(?:(?<s>\\d+) ?s(?:\\w* ?)?)?"
    )

    val playerChatPattern by patternGroup.pattern(
        "string.playerchat",
        "(?<important>.*?)(?:§[f7r])*: .*"
    )
    val chatUsernamePattern by patternGroup.pattern(
        "string.chatusername",
        "^(?:§\\w\\[§\\w\\d+§\\w] )?(?:(?:§\\w)+\\S )?(?<rankedName>(?:§\\w\\[\\w.+] )?(?:§\\w)?(?<username>\\w+))(?: (?:§\\w)?\\[.+?])?"
    )
}
