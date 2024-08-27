package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.data.jsonobjects.repo.EssenceShopUpgrade
import at.hannibal2.skyhanni.data.jsonobjects.repo.EssenceShopsJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object EssenceShopHelper {

    private var essenceShops = mutableListOf<EssenceShop>()

    data class EssenceShop(val shopName: String, val upgrades: List<EssenceShopUpgrade>, val essenceId: String? = null) {

        val itemId: String = essenceId ?: "ESSENCE_${shopName.uppercase()}"
        val totalCost: Int = upgrades.sumOf { it.costs?.sum() ?: 0}
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<EssenceShopsJson>("EssenceShops")
        essenceShops = data.shops.map { (type, upgrades, essenceId) ->
            EssenceShop(type, upgrades, essenceId)
        }.toMutableList()
    }
}
