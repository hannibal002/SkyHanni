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

        val essenceItemId: String = essenceId ?: "ESSENCE_${shopName.uppercase()}"
        val totalCost: Int = upgrades.sumOf { it.costs?.sum() ?: 0}
    }

    data class EssenceShopProgress(val essenceName: String, val purchasedUpgrades: Map<String, Int>) {
        private val essenceShop = essenceShops.find { it.shopName.equals(essenceName, ignoreCase = true) }
        val remainingUpgrades: MutableMap<String, MutableList<Int>> = essenceShop?.upgrades?.associate {
            it.name to buildList {
                val purchasedAmount = purchasedUpgrades[it.name]
                if (purchasedAmount == null) addAll(it.costs ?: emptyList())
                else addAll(it.costs?.drop(purchasedAmount) ?: emptyList())
            }.toMutableList()
        }?.toMutableMap() ?: mutableMapOf()
        val nonRepoUpgrades = purchasedUpgrades.any { purchasedUpgrade ->
            essenceShop?.upgrades?.none { it.name.equals(purchasedUpgrade.key, ignoreCase = true) } == true
        }
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<EssenceShopsJson>("EssenceShops")
        essenceShops = data.shops.map { (type, upgrades, essenceId) ->
            EssenceShop(type, upgrades, essenceId)
        }.toMutableList()
    }
}
