package at.hannibal2.skyhanni.utils.compat

import net.minecraft.network.chat.Component
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.PlayerScoreEntry
import net.minecraft.world.scores.Scoreboard

fun Scoreboard.getSidebarObjective(): Objective? {
    return this.getDisplayObjective(DisplaySlot.SIDEBAR)
}

fun Collection<PlayerScoreEntry>.getPlayerNames(scoreboard: Scoreboard): List<Component> {
    return this.sortedBy { it.value }
        .map {
            val team = scoreboard.getPlayersTeam(it.owner)
            Component.empty().also { main ->
                team?.playerPrefix?.apply {
                    if (siblings.isNotEmpty()) siblings.forEach { sibling -> main.append(sibling) }
                    else main.append(this)
                }
                // the soccer ball is because it is like that on 1.8
                // this could be changed later but for now i think this is fine
                main.append("⚽")
                team?.playerSuffix?.apply {
                    if (siblings.isNotEmpty()) siblings.forEach { sibling -> main.append(sibling) }
                    else main.append(this)
                }
            }
        }
}
