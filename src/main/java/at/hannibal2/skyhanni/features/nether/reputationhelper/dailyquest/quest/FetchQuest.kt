package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest

import at.hannibal2.skyhanni.utils.NeuInternalName
import net.minecraft.world.phys.Vec3

class FetchQuest(
    val itemName: String,
    location: Vec3?,
    displayItem: NeuInternalName,
    state: QuestState,
    needAmount: Int,
) :
    ProgressQuest(displayItem, location, QuestCategory.FETCH, itemName, state, needAmount)
