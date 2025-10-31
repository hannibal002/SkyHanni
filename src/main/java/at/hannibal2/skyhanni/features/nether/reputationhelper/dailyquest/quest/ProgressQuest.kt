package at.hannibal2.hanni.features.nether.reputationhelper.dailyquest.quest

import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.NeuInternalName

abstract class ProgressQuest(
    displayItem: NeuInternalName,
    location: LorenzVec?,
    questCategory: QuestCategory,
    displayName: String,
    state: QuestState,
    val needAmount: Int,
    var haveAmount: Int = 0,
) : Quest(displayItem, location, questCategory, displayName, state)
