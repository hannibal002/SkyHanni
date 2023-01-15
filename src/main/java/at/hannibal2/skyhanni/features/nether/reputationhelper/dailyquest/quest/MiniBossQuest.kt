package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest

import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.CrimsonMiniBoss

class MiniBossQuest(val miniBoss: CrimsonMiniBoss, state: QuestState, needAmount: Int) :
    ProgressQuest(QuestCategory.MINIBOSS, miniBoss.displayName, state, needAmount)