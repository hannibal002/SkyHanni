package at.hannibal2.skyhanni.features.nether.reputationhelper.miniboss

import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

class CrimsonMiniBoss(
    val displayName: String,
    val displayItem: NeuInternalName,
    val location: LorenzVec?,
    var doneToday: Boolean = false,
) {
    val pattern by RepoPattern.pattern(
        "crimson.reputationhelper.miniboss.${displayName.lowercase().replace(" ", "_")}",
        " *${displayName.uppercase()} DOWN!"
    )
}
