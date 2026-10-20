package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

enum class StarlynSisterType(val inventoryName: String, val couponName: NeuInternalName) {
    AGATHA("Agatha's Shop", "AGATHA_COUPON".toInternalName()),
    MIRIA("Miria's Shop", "MIRIA_COUPON".toInternalName()),
}
