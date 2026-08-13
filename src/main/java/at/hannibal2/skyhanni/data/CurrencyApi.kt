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
            val message = line.trimWhiteSpace().removeResets()

            ScoreboardPattern.copperPattern.matchMatcher(message) {
                SkyblockCurrency.COPPER.setAmount(group("copper").formatLong())
            }
            // while sowdust is gained, hypixel shortens the number, those lines are skipped on purpose
            ScoreboardPattern.sowdustPattern.matchMatcher(message) {
                SkyblockCurrency.SOWDUST.setAmount(group("sowdust").formatLong())
            }
        }
    }

    // the widget groups match anything, so the amounts are not guaranteed to be numbers
    @HandleEvent(onlyOnSkyblock = true)
    private fun onWidgetUpdate(event: WidgetUpdateEvent) {
        when {
            event.isWidget(TabWidget.COPPER) -> TabWidget.COPPER.matchMatcherFirstLine {
                group("copper").formatLongOrNull()?.let { SkyblockCurrency.COPPER.setAmount(it) }
            }

            event.isWidget(TabWidget.SOWDUST) -> TabWidget.SOWDUST.matchMatcherFirstLine {
                group("sowdust").formatLongOrNull()?.let { SkyblockCurrency.SOWDUST.setAmount(it) }
            }
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
