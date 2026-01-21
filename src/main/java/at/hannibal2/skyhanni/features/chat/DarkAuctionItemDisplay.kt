package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.ChatFormatting

@SkyHanniModule
object DarkAuctionItemDisplay {
    private val config get() = SkyHanniMod.feature.chat

    private val patternGroup = RepoPattern.group("chat.darkauction.itemdisplay")

    /**
     * REGEX-TEST: [NPC] Sirius: First up we have a Nether Artifact, the starting bid is 50,000 Coins!
     * REGEX-TEST: [NPC] Sirius: Next up we have a Protection VII Book, the starting bid is 100,000 Coins!
     * REGEX-TEST: [NPC] Sirius: Next up we have an Ender Artifact, the starting bid is 50,000 Coins!
     */
    private val nextItemPattern by patternGroup.pattern(
        "next-item",
        "\\[NPC] Sirius: (?:First|Next) up we have an? (?<item>[^,]+), the starting bid is [\\d,]+ Coins!",
    )

    /**
     * REGEX-TEST: [NPC] Sirius: The highest bidder was qtLuna with a bid of 25,950,000 Coins!
     */
    private val highestBidPattern by patternGroup.pattern(
        "highest-bid",
        "\\[NPC] Sirius: The highest bidder was \\w+ with a bid of [\\d,]+ Coins!",
    )

    private var lastItem: String? = null

    @HandleEvent
    fun onWorldChange() {
        lastItem = null
    }

    @HandleEvent(onlyOnIsland = IslandType.DARK_AUCTION)
    fun onChat(event: SkyHanniChatEvent) {
        if (!config.darkAuctionItemDisplay) return

        val message = event.cleanMessage

        nextItemPattern.matchMatcher(message) {
            lastItem = group("item")
            ChatUtils.debug("lastItem set to $lastItem")
            return
        }

        highestBidPattern.matchMatcher(message) {
            ChatUtils.debug("lastItem is $lastItem")
            val item = lastItem ?: return@matchMatcher

            event.replaceComponent(
                event.chatComponent.copy().appendWithColor(" ($item)", ChatFormatting.GRAY),
                "dark_auction_item",
            )
            lastItem = null
            return
        }
    }
}
