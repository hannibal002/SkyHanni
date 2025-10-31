package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SoundUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object AuctionOutbidWarning {

    /**
     * REGEX-TEST: §6[Auction] §aMrBaiacu §eoutbid you by §659,083 coins §efor §fFiredust Dagger §e§lCLICK
     */
    private val outbidPattern by RepoPattern.pattern(
        "auction.outbid",
        "§6\\[Auction].*§eoutbid you by.*§e§lCLICK"
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: HanniChatEvent) {
        if (!HanniMod.feature.inventory.auctions.auctionOutbid) return
        if (!outbidPattern.matches(event.message)) return

        TitleManager.sendTitle("§cYou have been outbid!")
        SoundUtils.playBeepSound()
    }
}
