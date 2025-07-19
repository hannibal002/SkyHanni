package at.hannibal2.skyhanni.features.inventory.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.BazaarJson
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.bazaar.BazaarTransactionEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.percentageColor
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import java.time.ZoneOffset
import java.time.ZonedDateTime

@SkyHanniModule
object BazaarLimitTracker {
    private var dailyLimit: Double = 15_000_000_000.0
    private var capOrdersAtIntLimit: Boolean = true

    private val config get() = SkyHanniMod.feature.inventory.bazaar
    private val storage get() = ProfileStorageData.playerSpecific?.bazaar

    private var coinsTowardsLimit: Double
        get() = storage?.coinsTowardsLimit ?: 0.0
        set(value) {
            storage?.coinsTowardsLimit = value
        }

    private var lastAccessedDay
        get() = storage?.lastAccessedDay
        set(value) {
            storage?.lastAccessedDay = value
        }

    @HandleEvent
    fun onBazaarTransaction(event: BazaarTransactionEvent) {
        if (event.transactionType == BazaarTransactionEvent.TransactionType.FLIP_ORDER) return
        // Hypixel ignores coins in excess of the integer limit for individual orders
        val coinsUpToIntLimit = when {
            capOrdersAtIntLimit && event.coinAmount >= Int.MAX_VALUE -> Int.MAX_VALUE.toDouble()
            else -> event.coinAmount
        }
        coinsTowardsLimit += coinsUpToIntLimit
        if (coinsTowardsLimit >= dailyLimit) {
            ChatUtils.chat("You reached your daily trade limit in the bazaar!")
        }
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!BazaarApi.inBazaarInventory) return
        checkDate()
    }

    private fun checkDate() {
        val currentDay = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate()
        val lastAccessed = lastAccessedDay

        if (lastAccessed == null || currentDay.isAfter(lastAccessed)) {
            coinsTowardsLimit = 0.0
            lastAccessedDay = currentDay
        }
    }

    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!config.dailyLimitTracker) return
        if (!BazaarApi.inBazaarInventory) return

        val color = percentageColor(dailyLimit.toLong() - coinsTowardsLimit.toLong(), dailyLimit.toLong()).getChatColor()

        val display = buildList {
            addString("§aBazaar Daily Limit:")
            addString("$color${coinsTowardsLimit.toLong().addSeparators()}§7/${dailyLimit.formatCoin()} coins")
            if (coinsTowardsLimit >= dailyLimit) {
                addString("§cLimit reached!")
            }
        }

        config.dailyLimitTrackerPosition.renderRenderables(
            display,
            posLabel = "Bazaar Daily Limit Tracker",
        )
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<BazaarJson>("Bazaar")
        dailyLimit = data.dailyLimit
        capOrdersAtIntLimit = data.capOrdersAtIntLimit
    }
}
