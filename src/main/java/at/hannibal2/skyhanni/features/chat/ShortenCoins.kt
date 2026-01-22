package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDoubleOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.findAll
import at.hannibal2.skyhanni.utils.compat.replace
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object ShortenCoins {
    private val config get() = SkyHanniMod.feature.chat
    private val patternGroup = RepoPattern.group("chat.coins")

    /**
     * REGEX-TEST: [Auction] EuropaPlus bought Atmospheric Filter for 2,650,000 coins §lCLICK
     * REGEX-TEST: You sold Cicada Symphony Vinyl x1 for 650,000 Coins!
     * REGEX-TEST: ALLOWANCE! You earned 50,000 coins!
     * REGEX-TEST: [Bazaar] Sell Offer Setup! 5x Enchanted Melon Block for 250,303 coins.
     * REGEX-TEST: [NPC] Sirius: The highest bidder was LOMENJUICE with a bid of 8,620,000 Coins!
     * REGEX-FAIL: You have withdrawn 10.5k coins§r§a! You now have 991.1M coins in your account!
     * REGEX-FAIL: :typing:  -  ✎...
     * REGEX-FAIL: Profile ID: 23a8da75-5655-49b2-89e7-31b9d2a7ab7b
     */
    private val coinsPattern by patternGroup.pattern(
        "format-no-color",
        "(?<amount>\\d[\\d,.]+)(?![\\d.,kMB]) ",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Modify) {
        if (!config.shortenCoinAmounts) return
        val message = event.cleanMessage
        var newComp = event.chatComponent.copy()
        var found = false
        for (amount in coinsPattern.findAll(message)) {
            val trimmed = amount.trim()
            val formatted = trimmed.formatDoubleOrNull() ?: continue
            val editedComp = newComp.replace(Regex("^$trimmed"), formatted.shortFormat())
            if (editedComp != null) {
                newComp = editedComp
                found = true
            }
        }
        if (!found) return

        event.replaceComponent(newComp, "shortened_coins")
    }

    fun Number.formatChatCoins(): String {
        return "§6" + if (config.shortenCoinAmounts) {
            shortFormat()
        } else {
            addSeparators()
        }
    }
}
