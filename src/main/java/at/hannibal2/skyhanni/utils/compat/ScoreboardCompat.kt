package at.hannibal2.skyhanni.utils.compat

import net.minecraft.scoreboard.Score
import net.minecraft.scoreboard.ScoreObjective
import net.minecraft.scoreboard.Scoreboard
//#if MC > 1.21
//$$ import net.minecraft.scoreboard.ScoreboardDisplaySlot
//$$ import net.minecraft.text.Text
//$$ import net.minecraft.scoreboard.ScoreboardEntry
//#endif

fun Scoreboard.getSidebarObjective(): ScoreObjective? {
    //#if MC < 1.21
    return this.getObjectiveInDisplaySlot(1)
    //#else
    //$$ return this.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR)
    //#endif
}

//#if MC < 1.21
@Suppress("UNUSED_PARAMETER")
fun Collection<Score>.getPlayerNames(scoreboard: Scoreboard): List<Score> {
    return this.filter { input: Score? ->
        input != null && input.playerName != null && !input.playerName.startsWith("#")
    }
}
//#else
//$$ fun Collection<ScoreboardEntry>.getPlayerNames(scoreboard: Scoreboard): List<Text> {
//$$     return this.sortedBy { -it.value }
//$$         .map {
//$$             val team = scoreboard.getScoreHolderTeam(it.owner)
//$$             Text.empty().also { main ->
//$$                 team?.prefix?.apply { siblings.forEach { sibling -> main.append(sibling) } }
//$$                 // the soccer ball is because it is like that on 1.8
//$$                 // this could be changed later but for now i think this is fine
//$$                 main.append("⚽")
//$$                 team?.suffix?.apply { siblings.forEach { sibling -> main.append(sibling) } }
//$$             }
//$$         }
//$$ }
//#endif
