package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest

abstract class ProgressQuest(
    displayItem: String?,
    questCategory: QuestCategory,
    displayName: String,
    state: QuestState,
    val needAmount: Int,
    var haveAmount: Int = 0
) : Quest(displayItem, questCategory, displayName, state)