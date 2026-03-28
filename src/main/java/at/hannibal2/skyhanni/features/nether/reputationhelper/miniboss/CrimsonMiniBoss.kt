package at.hannibal2.skyhanni.features.nether.reputationhelper.miniboss

import at.hannibal2.skyhanni.utils.NeuInternalName
import net.minecraft.world.phys.Vec3
import java.util.regex.Pattern

class CrimsonMiniBoss(
    val displayName: String,
    val displayItem: NeuInternalName,
    val location: Vec3?,
    val pattern: Pattern,
    var doneToday: Boolean = false,
)
