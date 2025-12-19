package at.hannibal2.skyhanni.utils.compat

import net.minecraft.world.scores.Score
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.Scoreboard
//#if MC > 1.21
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.PlayerScoreEntry
//#endif

fun Scoreboard.getSidebarObjective(): Objective? {
    //#if MC < 1.21
    //$$ return this.getObjectiveForSlot(1)
    //#else
    return this.getDisplayObjective(DisplaySlot.SIDEBAR)
    //#endif
}

//#if MC < 1.21
//$$ @Suppress("UNUSED_PARAMETER")
//$$ fun Collection<ScoreboardPlayerScore>.getPlayerNames(scoreboard: Scoreboard): List<ScoreboardPlayerScore> {
//$$     return this.filter { input: ScoreboardPlayerScore? ->
//$$         input != null && input.playerName != null && !input.playerName.startsWith("#")
//$$     }
//$$ }
//#else
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
//#endif
