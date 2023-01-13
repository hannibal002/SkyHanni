package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest

class BossQuest(bossName: String, state: QuestState, needAmount: Int) :
    ProgressQuest(QuestCategory.MINIBOSS, bossName, state, needAmount)