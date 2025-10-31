package at.hannibal2.hanni.features.slayer

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.SlayerApi
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SoundUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object SlayerCocoonWarning {
    /**
     * REGEX-TEST:   §r§c§lYOU COCOONED YOUR SLAYER BOSS
     */
    private val slayerCocoonPattern by RepoPattern.pattern(
        "slayer.cocooned",
        "\\s+§r§c§lYOU COCOONED YOUR SLAYER BOSS",
    )

    private val config get() = SlayerApi.config

    @HandleEvent
    fun onChatMessage(event: HanniChatEvent) {
        if (slayerCocoonPattern.matches(event.message)) {
            if (config.cocoonTitle) TitleManager.sendTitle("§lSlayer Boss Cocooned!")
            if (config.cocoonDing) SoundUtils.repeatSound(100, 10, SoundUtils.plingSound)
        }
    }
}
