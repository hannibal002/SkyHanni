package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object AuctionOutbidWarning {

    /**
     * REGEX-TEST: [Auction] MrBaiacu outbid you by 59,083 coins for Firedust Dagger CLICK
     */
    private val outbidPattern by RepoPattern.pattern(
        "auction.outbid.colorless",
        "\\[Auction].*outbid you by.*CLICK"
    )

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!SkyHanniMod.feature.inventory.auctions.auctionOutbid) return
        if (!outbidPattern.matches(event.cleanMessage)) return

        TitleManager.sendTitle("§cYou have been outbid!")
        SoundUtils.playBeepSound()
    }
}
