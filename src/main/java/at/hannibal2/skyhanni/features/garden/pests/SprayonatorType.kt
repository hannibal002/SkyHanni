package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

enum class SprayonatorType(
    rawName: String,
    val duration: Duration,
) {
    BASIC("SPRAYONATOR", 30.minutes),
    JUICY("JUICY_SPRAYONATOR", 45.minutes),
    SALTY("SALTY_SPRAYONATOR", 60.minutes),
    ;

    val internalName: NeuInternalName = rawName.toInternalName()

    companion object {
        private fun getByInternalNameOrNull(internalName: NeuInternalName): SprayonatorType? =
            entries.firstOrNull { it.internalName == internalName }

        fun getInHandOrNull(): SprayonatorType? = getByInternalNameOrNull(InventoryUtils.itemInHandId)

        /** Falls back to the last held Sprayonator, as chat messages can arrive after a slot change. */
        fun getRecentlyHeldOrNull(): SprayonatorType? = getInHandOrNull()
            ?: InventoryUtils.pastItemsInHand.asReversed().firstNotNullOfOrNull { getByInternalNameOrNull(it.second) }
    }
}
