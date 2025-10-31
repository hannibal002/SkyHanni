package at.hannibal2.hanni.features.nether.reputationhelper.dailyquest.quest

import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.NeuInternalName

abstract class Quest(
    val displayItem: NeuInternalName,
    val location: LorenzVec?,
    val category: QuestCategory,
    val internalName: String,
    var state: QuestState,
    val displayName: String = internalName,
)
