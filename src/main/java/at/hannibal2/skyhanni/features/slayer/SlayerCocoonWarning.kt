package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
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
    fun onChatMessage(event: SkyHanniChatEvent) {
        if (slayerCocoonPattern.matches(event.message)) {
            if (config.cocoonTitle) TitleManager.sendTitle("§lSlayer Boss Cocooned!")
            if (config.cocoonDing) SoundUtils.repeatSound(100, 10, SoundUtils.plingSound)
        }
    }
}
