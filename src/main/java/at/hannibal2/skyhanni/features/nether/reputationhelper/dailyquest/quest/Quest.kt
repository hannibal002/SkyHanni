package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest

import at.hannibal2.skyhanni.utils.LorenzVec

abstract class Quest(
    val displayItem: String?,
    val location: LorenzVec?,
    val category: QuestCategory,
    val internalName: String,
    var state: QuestState,
    val displayName: String = internalName
)