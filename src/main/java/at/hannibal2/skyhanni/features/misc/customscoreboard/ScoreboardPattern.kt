package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

object ScoreboardPattern {
    val group = RepoPattern.group("features.misc.customscoreboard")
    val lobbycodePattern by group.pattern ("lobbycode", "§(\\d{3}/\\d{2}/\\d{2}) §(?<code>.*)$")
}
