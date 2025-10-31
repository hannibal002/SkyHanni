package at.hannibal2.hanni.features.chat

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.formatDouble
import at.hannibal2.hanni.utils.NumberUtil.shortFormat
import at.hannibal2.hanni.utils.RegexUtils.replace
import at.hannibal2.hanni.utils.chat.TextHelper.asComponent
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object ShortenCoins {
    private val config get() = HanniMod.feature.chat
    private val patternGroup = RepoPattern.group("chat.coins")

    /**
     * REGEX-TEST: §6[Auction] §aEuropaPlus §ebought §fAtmospheric Filter §efor §62,650,000 coins §lCLICK
     * REGEX-TEST: §aYou sold §r§aCicada Symphony Vinyl§r§8 x1 §r§afor §r§650,000 Coins§r§a!
     * REGEX-TEST: §6§lALLOWANCE! §r§eYou earned §r§650,000 coins§r§e!
     * REGEX-TEST: §6[Bazaar] §r§7§r§eSell Offer Setup! §r§a5§r§7x §r§9Enchanted Melon Block §r§7for §r§6250,303 coins§r§7.
     * REGEX-FAIL: §aYou have withdrawn §r§610.5k coins§r§a! You now have §r§6991.1M coins §r§ain your account!
     * REGEX-FAIL: §6:typing:  §f-  §e✎§6...
     */
    private val coinsPattern by patternGroup.pattern(
        "format",
        "§6(?<amount>\\d[\\d,.]+)(?![\\d.,kMB])",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: HanniChatEvent) {
        if (!config.shortenCoinAmounts) return
        val message = event.message
        val modifiedMessage = coinsPattern.replace(message) {
            "§6${group("amount").formatDouble().shortFormat()}"
        }.takeIf { it != message } ?: return

        val originalComponent = event.chatComponent.siblings.firstOrNull() ?: event.chatComponent

        val newComponent = modifiedMessage.asComponent {
            chatStyle = originalComponent.chatStyle
        }
        event.replaceComponent(newComponent, "shortened_coins")
    }

    fun Number.formatChatCoins(): String {
        return "§6" + if (config.shortenCoinAmounts) {
            shortFormat()
        } else {
            addSeparators()
        }
    }
}
