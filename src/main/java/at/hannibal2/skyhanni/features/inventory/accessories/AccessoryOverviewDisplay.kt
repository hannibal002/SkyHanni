package at.hannibal2.skyhanni.features.inventory.accessories

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.inventory.accessories.AccessoryOverviewDisplayConfig
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.inventory.AccessoriesUpdatedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.renderables.Renderable

@SkyHanniModule
object AccessoryOverviewDisplay {

    private val config get() = SkyHanniMod.feature.inventory.stats
    private val storage get() = ProfileStorageData.profileSpecific?.stats
    private val inAccBag get() = AccessoryApi.inAccessoryBag
    private val renderCache: MutableMap<AccessoryOverviewDisplayConfig.AccessoryDisplayTab, List<Renderable>> = enumMapOf()

    private var lastBuiltAccHash: Int = 0

    @HandleEvent
    fun onAccessoriesUpdated(event: AccessoriesUpdatedEvent) {
        val newAccessories = event.accessories.takeIf { it.hashCode() != lastBuiltAccHash } ?: return
        lastBuiltAccHash = newAccessories.hashCode()
    }
}
