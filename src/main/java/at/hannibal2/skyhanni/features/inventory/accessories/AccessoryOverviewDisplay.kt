package at.hannibal2.skyhanni.features.inventory.accessories

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object AccessoryOverviewDisplay {

    private val config get() = SkyHanniMod.feature.inventory.accessory
    private val storage get() = ProfileStorageData.profileSpecific?.stats
    private val inAccBag get() = AccessoryApi.inAccessoryBag

    private var lastBuiltAccHash: Int = 0
}
