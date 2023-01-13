package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest

class FetchQuest(val itemName: String, state: QuestState, needAmount: Int) :
    ProgressQuest(QuestCategory.FETCH, itemName, state, needAmount)