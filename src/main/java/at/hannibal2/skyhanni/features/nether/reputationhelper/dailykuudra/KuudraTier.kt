package at.hannibal2.skyhanni.features.nether.reputationhelper.dailykuudra

import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUInternalName

class KuudraTier(
    val name: String,
    val displayItem: NEUInternalName,
    val location: LorenzVec?,
    val tierNumber: Int,
    var doneToday: Boolean = false
) {
    fun getDisplayName() = "Tier $tierNumber ($name)"
}
