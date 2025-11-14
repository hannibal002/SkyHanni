package at.hannibal2.skyhanni.features.event.spook

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object SpookyChestAlert {
    private val config get() = SkyHanniMod.feature.event.spooky.spookyChests

    private val patternGroup = RepoPattern.group("event.spooky")

    /**
     * REGEX-TEST: §6§lSPOOKY! §r§7A §r§6Trick or Treat Chest §r§7has appeared!
     * REGEX-TEST: §c§lPARTY! §r§7A §r§cParty Chest §r§7has appeared!
     */
    private val chestMessagePattern by patternGroup.pattern(
        "chat.chest",
        "§[6c]§l(?<type>SPOOKY|PARTY)! §r§7A §r§(?<color>[6c])(?<chest>Trick or Treat Chest|Party Chest) §r§7has appeared!",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent) {
        if (!config.enabled) return
        chestMessagePattern.matchMatcher(event.message) {
            TitleManager.sendTitle(
                "§l§${group("color")}" + if (config.compactTitle) {
                    "${group("type")} CHEST"
                } else {
                    "${group("chest").uppercase()}!"
                },
            )
        } ?: return

        if (!config.playSound) return
        SoundUtils.playBeepSound()
    }
}
