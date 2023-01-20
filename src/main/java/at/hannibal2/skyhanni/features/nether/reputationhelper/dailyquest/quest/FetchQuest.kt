package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest

class FetchQuest(val itemName: String, displayItem: String?, state: QuestState, needAmount: Int) :
    ProgressQuest(displayItem, QuestCategory.FETCH, itemName, state, needAmount)