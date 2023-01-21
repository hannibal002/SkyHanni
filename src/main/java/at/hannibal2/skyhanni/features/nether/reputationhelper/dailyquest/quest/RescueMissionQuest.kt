package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest

import at.hannibal2.skyhanni.utils.LorenzVec

class RescueMissionQuest(displayItem: String?, location: LorenzVec?, state: QuestState) :
    Quest(displayItem, location, QuestCategory.RESCUE, "Rescue Mission", state, "Rescue the NPC")