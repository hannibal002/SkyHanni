package at.hannibal2.skyhanni.features.inventory.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.bazaar.BazaarTransactionEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.percentageColor
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import java.time.ZoneOffset
import java.time.ZonedDateTime

@SkyHanniModule
object BazaarLimitTracker {
    private const val DAILY_LIMIT = 15_000_000_000.0

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
        if (event.transactionType != BazaarTransactionEvent.TransactionType.FLIP_ORDER) {
            coinsTowardsLimit += event.coinAmount
            if (coinsTowardsLimit >= DAILY_LIMIT) {
                ChatUtils.chat("You reached your daily trade limit in the bazaar!")
            }
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
        if (!isEnabled()) return
        if (!BazaarApi.inBazaarInventory) return

        val color = percentageColor(DAILY_LIMIT.toLong() - coinsTowardsLimit.toLong(), DAILY_LIMIT.toLong()).getChatColor()

        val display = mutableListOf(
            "§aBazaar Daily Limit:",
            "$color${coinsTowardsLimit.toLong().addSeparators()}§7/${DAILY_LIMIT.formatCoin()} coins",
        )
        if (coinsTowardsLimit >= DAILY_LIMIT) {
            display.add("§cLimit reached!")
        }

        config.dailyLimitTrackerPosition.renderStrings(
            display,
            posLabel = "Bazaar Daily Limit Tracker",
        )
    }

    private fun isEnabled() = config.dailyLimitTracker
}
