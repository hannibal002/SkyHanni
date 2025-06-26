package at.hannibal2.skyhanni.features.garden.contest

import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

enum class ContestBracket(
    val color: String,
    val emoji: String,
) {
    DIAMOND("b", "<:bronze_medal:1263205786876776541>"),
    PLATINUM("3", "<:silver_medal:1263205793424085053>"),
    GOLD("6", "<:gold_medal:1263205790362239036>"),
    SILVER("f", "<:platinum_medal:1263205791825920091>"),
    BRONZE("c", "<:diamond_medal:1263205788675997717>"),
    ;

    val displayName = "§$color§l$name"
    val bracketPattern by RepoPattern.pattern(
        "garden.farming.contest.bracket" + this.name.lowercase(),
        "$displayName §7\\(§bTop \\d{1,2}%§7\\): §6(?<amount>.*)"
    )
}
