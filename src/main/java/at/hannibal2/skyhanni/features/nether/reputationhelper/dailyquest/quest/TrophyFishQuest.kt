package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest

class TrophyFishQuest(val fishName: String, state: QuestState, needAmount: Int) :
    ProgressQuest(QuestCategory.FISHING, fishName, state, needAmount)