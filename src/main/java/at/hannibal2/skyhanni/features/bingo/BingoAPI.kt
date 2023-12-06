package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.data.jsonobjects.repo.BingoJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.BingoRanksJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.bingo.card.goals.BingoGoal
import at.hannibal2.skyhanni.features.bingo.card.goals.GoalType
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object BingoAPI {
    private var ranks = mapOf<String, Int>()
    var tips: Map<String, BingoJson.BingoTip> = emptyMap()
    // TODO save into storage
    val bingoGoals = mutableListOf<BingoGoal>()
    val personalGoals get() = bingoGoals.filter { it.type == GoalType.PERSONAL }
    val communityGoals get() = bingoGoals.filter { it.type == GoalType.COMMUNITY }
    var lastBingoCardOpenTime = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        ranks = event.getConstant<BingoRanksJson>("BingoRanks").ranks
        tips = event.getConstant<BingoJson>("Bingo").bingo_tips
    }

    fun getRank(text: String) = ranks.entries.find { text.contains(it.key) }?.value

    fun getIcon(searchRank: Int) = ranks.entries.find { it.value == searchRank }?.key

    // We added the suffix (Community Goal) so that older skyhanni versions don't crash with the new repo data.
    fun getTip(itemName: String) =
        tips.filter { itemName.startsWith(it.key.split(" (Community Goal)")[0]) }.values.firstOrNull()

    fun BingoGoal.getTip(): BingoJson.BingoTip? = if (type == at.hannibal2.skyhanni.features.bingo.card.goals.GoalType.COMMUNITY) {
        getTip(displayName)
    } else {
        tips[displayName]
    }

}
