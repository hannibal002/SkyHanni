package at.hannibal2.skyhanni.features.nether.reputationhelper.miniboss

import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName

class CrimsonMiniBoss(
    val displayName: String,
    val displayItem: NeuInternalName,
    val location: LorenzVec?,
    var doneToday: Boolean = false,
) {
    // Entries are loaded via onRepoReload so can't use RepoPattern.pattern() here
    val pattern = " *${displayName.uppercase()} DOWN!".toPattern()
}
