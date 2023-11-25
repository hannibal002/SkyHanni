package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.test.PriceSource
import at.hannibal2.skyhanni.utils.LorenzUtils.addSelector
import at.hannibal2.skyhanni.utils.NEUInternalName

class SkyHanniItemTracker<Data : ItemTrackerData>(
    name: String,
    createNewSession: () -> Data,
    getStorage: (Storage.ProfileSpecific) -> Data,
    drawDisplay: (Data) -> List<List<Any>>,
) : SkyHanniTracker<Data>(name, createNewSession, getStorage, drawDisplay) {

    fun addCoins(coins: Int) {
        addItem(ItemTrackerData.SKYBLOCK_COIN, coins)
    }

    fun addItem(internalName: NEUInternalName, stackSize: Int) {
        modify {
            it.additem(internalName, stackSize)
        }
    }

    fun addPriceFromButton(lists: MutableList<List<Any>>) {
        if (isInventoryOpen()) {
            lists.addSelector<PriceSource>(
                "",
                getName = { type -> type.displayName },
                isCurrent = { it.ordinal == config.priceFrom },
                onChange = {
                    config.priceFrom = it.ordinal
                    update()
                }
            )
        }

    }
}
