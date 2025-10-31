package at.hannibal2.hanni.features.nether.reputationhelper.dailyquest.quest

import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.NeuInternalName

class TrophyFishQuest(
    val fishName: String,
    location: LorenzVec?,
    displayItem: NeuInternalName,
    state: QuestState,
    needAmount: Int,
) :
    ProgressQuest(displayItem, location, QuestCategory.FISHING, fishName, state, needAmount)
