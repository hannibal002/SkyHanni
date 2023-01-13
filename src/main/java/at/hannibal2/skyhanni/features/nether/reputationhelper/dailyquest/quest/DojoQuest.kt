package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest

import at.hannibal2.skyhanni.utils.LorenzUtils

class DojoQuest(public val dojoName: String, dojoRankGoal: String, state: QuestState) :
    Quest(
        QuestCategory.DOJO,
        "$dojoName Rank $dojoRankGoal",
        state,
        "$dojoName (" + LorenzUtils.getPointsForDojoRank(dojoRankGoal) + " points)"
    )
