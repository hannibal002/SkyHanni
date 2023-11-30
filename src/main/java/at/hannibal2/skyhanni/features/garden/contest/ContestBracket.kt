package at.hannibal2.skyhanni.features.garden.contest

enum class ContestBracket(val color: String) {
    DIAMOND("b"),
    PLATINUM("3"),
    GOLD("6"),
    SILVER("f"),
    BRONZE("c"),
    ;

    val displayName = "§$color§l$name"
    val pattern = "$displayName §7\\(§bTop \\d{1,2}%§7\\): §6(?<amount>.*)".toPattern()
}
