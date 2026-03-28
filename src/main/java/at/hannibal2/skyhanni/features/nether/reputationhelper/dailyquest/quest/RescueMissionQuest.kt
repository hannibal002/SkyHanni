package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest

import at.hannibal2.skyhanni.utils.NeuInternalName
import net.minecraft.world.phys.Vec3

class RescueMissionQuest(displayItem: NeuInternalName, location: Vec3?, state: QuestState) : Quest(
    displayItem,
    location,
    QuestCategory.RESCUE,
    "Rescue Mission",
    state,
    "Rescue the NPC",
)
