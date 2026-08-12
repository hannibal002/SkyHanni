package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.NumberUtil.formatLongOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SkyblockCurrency
import at.hannibal2.skyhanni.utils.SkyblockCurrency.COPPER
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpace

/**
 * Remembers how much of a currency the player owns, for currencies that Hypixel only shows on
 * some islands. Without this the amount would be unknown everywhere else.
 */
@SkyHanniModule
object CurrencyApi {

    private val storage get() = ProfileStorageData.profileSpecific?.currencies

    private fun SkyblockCurrency.setAmount(amount: Long) {
        storage?.put(this, amount)
    }

    fun SkyblockCurrency.getFromStorage(): Long? = storage?.get(this)

    // reading every line instead of only the new ones, so the amount is still picked up
    // when the profile storage loads after the scoreboard
    @HandleEvent(onlyOnSkyblock = true)
    private fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        for (line in event.new) {
            ScoreboardPattern.copperPattern.matchMatcher(line.trimWhiteSpace().removeResets()) {
                COPPER.setAmount(group("copper").formatLong())
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.COPPER)) return

        // the widget group matches anything, so the amount is not guaranteed to be a number
        TabWidget.COPPER.matchMatcherFirstLine {
            group("copper").formatLongOrNull()?.let { COPPER.setAmount(it) }
        }
    }

    @HandleEvent
    private fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Currencies")
        event.addIrrelevant {
            for ((currency, amount) in storage.orEmpty()) {
                add("$currency: $amount")
            }
        }
    }
}
