package at.hannibal2.skyhanni.features.hunting.safari

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.hunting.SafariRunStartEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object SafariRunApi {

    private val patternGroup = RepoPattern.group("hunting.safari.run")

    /**
     * REGEX-TEST: [NPC] Safari Manager: I already saw your ticket, so you're free to go!
     * REGEX-TEST: [NPC] Safari Manager: Looks good to me. Have fun out there!
     */
    private val runStartMessage by patternGroup.pattern(
        "start",
        "(?:\\[NPC] )?(?:Safari Manager: )?(?:I already saw your ticket, so you're free to go[.!]|Looks good to me\\. Have fun out there!)",
    )

    @HandleEvent
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (runStartMessage.matches(event.messageComponent.getText().removeColor())) {
            SafariRunStartEvent().post()
        }
    }

}
