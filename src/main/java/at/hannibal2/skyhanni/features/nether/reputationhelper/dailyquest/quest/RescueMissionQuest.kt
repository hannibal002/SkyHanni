package at.hannibal2.hanni.features.nether.reputationhelper.dailyquest.quest

import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.NeuInternalName

class RescueMissionQuest(displayItem: NeuInternalName, location: LorenzVec?, state: QuestState) :
    Quest(displayItem, location, QuestCategory.RESCUE, "Rescue Mission", state, "Rescue the NPC")
