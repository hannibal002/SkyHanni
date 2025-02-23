package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.StringUtils.toFormattedName
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object CurrentChatDisplay {

    private val config get() = SkyHanniMod.feature.chat
    private val storage get() = ProfileStorageData.playerSpecific

    @Suppress("unused")
    enum class ChatTypes(
        color: LorenzColor? = null,
        chatName: String? = null,
        displayName: String? = null
    ) {
        ALL(LorenzColor.YELLOW),
        PARTY(LorenzColor.BLUE),
        GUILD(LorenzColor.DARK_GREEN),
        OFFICER(LorenzColor.DARK_AQUA),
        PRIVATE,
        SKYBLOCK_COOP(LorenzColor.AQUA, "SKYBLOCK CO-OP", "CO-OP"),
        ;

        private val chatName = chatName ?: name

        val displayName = color?.getChatColor().orEmpty() + (displayName ?: toFormattedName())

        companion object {
            fun fromName(name: String) = entries.find { it.chatName.equals(name, true) }
        }
    }

    private val patternGroun = RepoPattern.group("chat.currentchat")

    /**
     * REGEX-TEST: §aYou are now in the §r§6OFFICER§r§a channel
     */
    private val changedChatPattern by patternGroun.pattern(
        "changed",
        "§aYou are now in the §r§6(?<chat>.+)§r§a channel"
    )

    /**
     * REGEX-TEST: §cYou are not in a party and were moved to the ALL channel.
     * REGEX-TEST: §cThe conversation you were in expired and you have been moved back to the ALL channel.
     */
    private val allChatPattern by patternGroun.pattern(
        "all",
        "§cYou are not in a party and were moved to the ALL channel\\.|§cThe conversation you were in expired and you have been moved back to the ALL channel\\."
    )

    /**
     * REGEX-TEST: §aOpened a chat conversation with §r§b[MVP§r§5+§r§b] martimavocado§r§a for the next 5 minutes. Use §r§b/chat a§r§a to leave
     */
    private val openPrivateMessagePattern by patternGroun.pattern(
        "private.open",
        "^§aOpened a chat conversation with (?:§.)*(?:\\[.+])?(?:§.|\\s)*(?<player>.*)§r§a for the next 5 minutes. Use §r§b/chat a§r§a to leave"
    )

    /**
     * REGEX-TEST: §dTo §r§b[MVP§r§5+§r§b] martimavocado§r§7: §r§7balls
     */
    private val privateMessagePattern by patternGroun.pattern(
        "privatel.message",
        "^§d(?:From|To) (?:§.)*(?:\\[.+])?(?:§.|\\s)*(?<player>.+?)(?:§.)*:"
    )

    private var privateMessageEnd = SimpleTimeMark.farPast()
    private var privateMessagePlayer: String? = null

    private var currentChat: ChatTypes?
        get() = storage?.currentChat
        set(value) {
            storage?.currentChat = value
        }

    private var display: String? = null

    private val maxPrivateMessageTime = 5.minutes

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        val message = event.message
        changedChatPattern.matchMatcher(message) {
            val chat = group("chat")
            privateMessagePlayer = null
            currentChat = ChatTypes.fromName(chat)
            update()
            return
        }
        if (allChatPattern.matches(message)) {
            currentChat = ChatTypes.ALL
            privateMessagePlayer = null
            update()
            return
        }
        openPrivateMessagePattern.matchMatcher(message) {
            privateMessageEnd = maxPrivateMessageTime.fromNow()
            currentChat = ChatTypes.PRIVATE
            privateMessagePlayer = group("player")
            update()
            return
        }
        privateMessagePattern.findMatcher(event.message) {
            if (currentChat == ChatTypes.PRIVATE && privateMessagePlayer == group("player")) {
                privateMessageEnd = maxPrivateMessageTime.fromNow()
                update()
            }
            return
        }
    }

    private fun drawDisplay() = buildString {
        val chat = currentChat ?: return@buildString
        append("§aChat: ")
        if (chat == ChatTypes.PRIVATE) {
            append(privateMessagePlayer?.let { "§6$it " } ?: "§cUnknown ")
            append(if (privateMessageEnd.isInPast()) "§c(EXPIRED)" else "§b${privateMessageEnd.timeUntil().format()}")
            return@buildString
        }
        append(chat.displayName)
        if (chat != ChatTypes.PARTY) return@buildString
        val size = PartyApi.partyMembers.size
        append(
            if (size == 0) " §c(NOT IN PARTY)"
            else " §a(${size + 1} members)" // Add 1 because the party list in PartyApi doesn't include yourself
        )
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        update()
    }

    private fun update() {
        display = drawDisplay()
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        config.currentChatDisplayPos.renderString(display, posLabel = "Current Chat")
    }

    private fun isEnabled() = config.currentChatDisplay

}
