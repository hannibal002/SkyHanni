package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName

object PestAPI {
    val config get() = SkyHanniMod.feature.garden.pests

    val vacuumVariants = listOf(
        "SKYMART_VACUUM".asInternalName(),
        "SKYMART_TURBO_VACUUM".asInternalName(),
        "SKYMART_HYPER_VACUUM".asInternalName(),
        "INFINI_VACUUM".asInternalName(),
        "INFINI_VACUUM_HOOVERIUS".asInternalName(),
    )

    fun hasVacuumInHand() = InventoryUtils.itemInHandId in vacuumVariants
}
