package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.slayer.SlayerStateChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object SlayerCocoonWarning {

    private val config get() = SlayerApi.config
    /**
     * WRAPPED-REGEX-TEST: "  YOU COCOONED YOUR SLAYER BOSS"
     */
    private val slayerCocoonPattern by RepoPattern.pattern(
        "slayer.cocooned.colorless",
        "\\s+YOU COCOONED YOUR SLAYER BOSS",
    )

    @HandleEvent
    private fun onChatMessage(event: SkyHanniChatEvent.Allow) {
        if (slayerCocoonPattern.matches(event.cleanMessage)) {
            if (config.cocoonTitle) TitleManager.sendTitle("§lSlayer Boss Cocooned!")
            if (config.cocoonDing) SoundUtils.repeatSound(100, 10, SoundUtils.plingSound)
        }
    }
}
