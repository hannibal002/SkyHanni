package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest

import at.hannibal2.skyhanni.utils.LorenzUtils

class DojoQuest(val dojoName: String, displayItem: String?, dojoRankGoal: String, state: QuestState) :
    Quest(
        displayItem,
        QuestCategory.DOJO,
        "$dojoName Rank $dojoRankGoal",
        state,
        "$dojoName (" + LorenzUtils.getPointsForDojoRank(dojoRankGoal) + " points)"
    )
