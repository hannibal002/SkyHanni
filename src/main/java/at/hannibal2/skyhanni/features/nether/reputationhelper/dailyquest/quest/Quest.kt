package at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest

import at.hannibal2.skyhanni.utils.NeuInternalName
import net.minecraft.world.phys.Vec3

abstract class Quest(
    val displayItem: NeuInternalName,
    val location: Vec3?,
    val category: QuestCategory,
    val internalName: String,
    var state: QuestState,
    val displayName: String = internalName,
)
