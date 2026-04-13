package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.CrashReport
import net.minecraft.client.Minecraft

@SkyHanniModule
object CrashOnTTTFail {
    private val config get() = SkyHanniMod.feature.misc

    /**
     * REGEX-TEST: PUZZLE FAIL! Webhead1104 lost Tic Tac Toe! Yikes!
     */
    private val tttFailPattern by RepoPattern.pattern(
        "dungeons.tttfail.colorless",
        "PUZZLE FAIL! (?<name>.*) lost Tic Tac Toe! Yikes!",
    )

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        tttFailPattern.matchMatcher(event.chatComponent) {
            if (group("name") == PlayerUtils.getName()) {
                Minecraft.getInstance().delayCrash(
                    CrashReport(
                        "SkyHanni Crash on TTT Fail",
                        Throwable("Disable Crash on TTT Fail in SkyHanni if you want to stop crashing"),
                    ),
                )
            }
        }
    }

    private fun isEnabled(): Boolean = DungeonApi.inDungeon() && config.crashOnTTTFail
}
