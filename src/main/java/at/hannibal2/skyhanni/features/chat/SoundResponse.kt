package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object SoundResponse {

    private val config get() = SkyHanniMod.feature.chat

    private val repoGroup = RepoPattern.group("chat.sound.response")

    /** REGEX-TEST: meow
    REGEX-TEST:  meow
    REGEX-TEST:  meow
    REGEX-TEST: MEow
    REGEX-TEST: §ameow
    REGEX-TEST: hello §ameow
     * */
    private val meow by repoGroup.pattern("meow", "(?:^|^.* )(?: |§.)*(?i)meow(?: |§.)*(?:\$| .*\$)")

    @SubscribeEvent
    fun onLorenzChat(event: LorenzChatEvent) {
        if (config.meow && meow.matches(event.message)) {
            SoundUtils.playMeowSound()
        }
    }
}
