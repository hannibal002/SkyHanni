package at.hannibal2.skyhanni.features.nether.kuudra

import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

enum class KuudraTier(val displayName: String) {
    BASIC("Basic"),
    HOT("Hot"),
    BURNING("Burning"),
    FIERY("Fiery"),
    INFERNAL("Infernal"),
    ;

    var doneToday: Boolean = false

    private var intLocation: LorenzVec? = null
    private var intTierNumber: Int = ordinal + 1
    private var intDisplayItem: NeuInternalName = "KUUDRA_${name}_TIER_KEY".toInternalName()

    val location: LorenzVec? get() = intLocation
    val tierNumber: Int get() = intTierNumber
    val displayItem: NeuInternalName get() = intDisplayItem

    private fun setTierNumber(tierNumber: Int) { this.intTierNumber = tierNumber }
    private fun setLocation(location: LorenzVec?) { this.intLocation = location }
    private fun setDisplayItem(displayItem: NeuInternalName) { this.intDisplayItem = displayItem }

    fun getTieredDisplayName() = "Tier $intTierNumber ($displayName)"

    companion object {
        fun addRepoData(
            displayName: String,
            displayItem: NeuInternalName,
            location: LorenzVec?,
            tier: Int,
        ) {
            val target = entries.firstOrNull { it.displayName == displayName } ?: return
            target.setLocation(location)
            target.setDisplayItem(displayItem)
            target.setTierNumber(tier)
        }
    }
}
