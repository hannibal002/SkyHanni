package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CompactBestiaryChatMessage {

    private var inBestiary = false
    private var bestiaryDescription = mutableListOf<String>()
    private var acceptMoreDescription = true
    var command = ""
    private var blockedLines = 0

    private var lastBorder: IChatComponent? = null
    private var lastEmpty: IChatComponent? = null

    private var milestoneMessage: String? = null

    private val patternGroup = RepoPattern.group("compactbestiarychat")
    private val titleMessagePattern by patternGroup.pattern(
        "title",
        "§f {34}§6§lBESTIARY"
    )
    private val borderMessagePattern by patternGroup.pattern(
        "border",
        "§3§l▬{64}"
    )
    private val milestonePattern by patternGroup.pattern(
        "milestone",
        "^.+(?<milestone>§8\\d{1,3}➡§e\\d{1,3})$"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.chat.compactBestiaryMessage) return

        val message = event.message

        if (borderMessagePattern.matches(message)) {
            lastBorder = event.chatComponent
        }
        if (message == " ") {
            lastEmpty = event.chatComponent
        }

        if (titleMessagePattern.matches(message)) {
            event.blockedReason = "bestiary"
            ChatManager.retractMessage(lastBorder, "bestiary")
            ChatManager.retractMessage(lastEmpty, "bestiary")

            lastBorder = null
            lastEmpty = null

            for (sibling in event.chatComponent.siblings) {
                sibling.chatStyle?.chatClickEvent?.let {
                    command = it.value
                }
            }
            inBestiary = true
            blockedLines = 0
            bestiaryDescription.add(message)
        } else if (inBestiary) {
            event.blockedReason = "bestiary"
            blockedLines++
            if (blockedLines > 15) {
                blockedLines = 0
                inBestiary = false
            }
            if (borderMessagePattern.matches(message)) {
                inBestiary = false

                val list = bestiaryDescription.map { it.replace("§f", "").trim() }
                val title = list[1]
                ChatUtils.hoverableChat("§6§lBESTIARY §r$title", list.dropLast(1), command, false)
                bestiaryDescription.clear()
                acceptMoreDescription = true
            } else {
                milestoneMessage?.let {
                    ChatUtils.chat("§6§lBESTIARY MILESTONE $it", false)
                    milestoneMessage = null
                }
                milestonePattern.matchMatcher(message) {
                    acceptMoreDescription = false
                    milestoneMessage = group("milestone")
                }
                if (acceptMoreDescription) {
                    bestiaryDescription.add(message)
                }
            }
        }
    }
}
