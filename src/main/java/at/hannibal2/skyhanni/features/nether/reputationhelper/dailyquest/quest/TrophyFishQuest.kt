package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest

class TrophyFishQuest(val fishName: String, displayItem: String?, state: QuestState, needAmount: Int) :
    ProgressQuest(displayItem, QuestCategory.FISHING, fishName, state, needAmount)