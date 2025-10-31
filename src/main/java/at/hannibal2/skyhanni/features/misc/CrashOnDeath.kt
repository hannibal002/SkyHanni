package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.crash.CrashReport

@HanniModule
object CrashOnDeath {
    private val config get() = HanniMod.feature.misc

    /**
     * REGEX-TEST: §c ☠ §r§7You were killed by §r§4§lMagma Boss§r§7§r§7.
     */
    private val pattern by RepoPattern.pattern(
        "ownplayer.death.chat",
        "§c ☠ §r§7You (?<reason>.+)",
    )

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!isEnabled()) return

        if (pattern.matches(event.message)) {
            Minecraft.getMinecraft().crashed(CrashReport("Not Reading", Throwable("Don't toggle all the Options")))
        }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.crashOnDeath
}
