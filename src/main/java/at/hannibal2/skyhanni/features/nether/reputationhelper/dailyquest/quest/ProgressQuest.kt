package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest

abstract class ProgressQuest(
    questCategory: QuestCategory,
    displayName: String,
    state: QuestState,
    val needAmount: Int,
    var haveAmount: Int = 0
) : Quest(questCategory, displayName, state)