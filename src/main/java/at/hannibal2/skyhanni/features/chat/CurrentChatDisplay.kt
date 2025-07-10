package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.hypixel.chat.event.PrivateMessageChatEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EnumUtils.toFormattedName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiChat
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CurrentChatDisplay {

    private val config get() = SkyHanniMod.feature.chat
    private val storage get() = ProfileStorageData.playerSpecific

    private var privateMessageEnd = SimpleTimeMark.farPast()
    private var privateMessagePlayer: String? = null

    private var currentChat: ChatType?
        get() = storage?.currentChat
        set(value) {
            storage?.currentChat = value
        }

    private var lastClosedChatTime = SimpleTimeMark.farPast()
    private var display: String? = null
    private val maxPrivateMessageTime = 5.minutes

    private val patternGroup = RepoPattern.group("chat.current-chat")

    /**
     * REGEX-TEST: §aYou are now in the §r§6OFFICER§r§a channel
     */
    private val changedChatPattern by patternGroup.pattern(
        "changed",
        "§aYou are now in the §r§6(?<chat>.+)§r§a channel",
    )

    /**
     * REGEX-TEST: §cYou are not in a party and were moved to the ALL channel.
     * REGEX-TEST: §cThe conversation you were in expired and you have been moved back to the ALL channel.
     */
    @Suppress("MaxLineLength")
    private val allChatPattern by patternGroup.pattern(
        "all",
        "§cYou are not in a party and were moved to the ALL channel\\.|§cThe conversation you were in expired and you have been moved back to the ALL channel\\.",
    )

    /**
     * REGEX-TEST: §aOpened a chat conversation with §r§b[MVP§r§5+§r§b] martimavocado§r§a for the next 5 minutes. Use §r§b/chat a§r§a to leave
     */
    @Suppress("MaxLineLength")
    private val openPrivateMessagePattern by patternGroup.pattern(
        "private.open",
        "^§aOpened a chat conversation with (?:§.)*(?:\\[.+])?(?:§.|\\s)*(?<player>.*)§r§a for the next 5 minutes\\. Use §r§b\\/chat a§r§a to leave",
    )

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        val message = event.message
        changedChatPattern.matchMatcher(message) {
            return updateChat(ChatType.fromName(group("chat")))
        }
        if (allChatPattern.matches(message)) {
            return updateChat(ChatType.ALL)
        }
        openPrivateMessagePattern.matchMatcher(message) {
            privateMessageEnd = maxPrivateMessageTime.fromNow()
            return updateChat(ChatType.PRIVATE, group("player"))
        }
    }

    @HandleEvent
    fun onPrivateMessageChat(event: PrivateMessageChatEvent) {
        if (currentChat == ChatType.PRIVATE && privateMessagePlayer == event.author.cleanPlayerName()) {
            privateMessageEnd = maxPrivateMessageTime.fromNow()
            update()
        }
    }

    private fun drawDisplay() = buildString {
        val chat = currentChat ?: return@buildString
        append("§aChat: ")
        if (chat == ChatType.PRIVATE) {
            append(privateMessagePlayer?.let { "§6$it " } ?: "§cUnknown ")
            append(if (privateMessageEnd.isInPast()) "§c(EXPIRED)" else "§b${privateMessageEnd.timeUntil().format()}")
            return@buildString
        }
        append(chat.displayName)
        if (chat != ChatType.PARTY) return@buildString
        val size = PartyApi.partyMembers.size
        append(
            if (size == 0) " §c(NOT IN PARTY)"
            else " §a(${size + 1} members)", // Add 1 because the party list in PartyApi doesn't include yourself
        )
    }

    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        if (!isEnabled()) return
        update()
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun updateChat(currentChat: ChatType, privateMessagePlayer: String? = null) {
        this.currentChat = currentChat
        this.privateMessagePlayer = privateMessagePlayer
        update()
    }

    @HandleEvent(GuiRenderEvent::class)
    fun onRenderOverlay() {
        if (!isEnabled()) return
        if (Minecraft.getMinecraft().currentScreen !is GuiChat && lastClosedChatTime.passedSince() > 2.seconds) return
        config.currentChatDisplayPos.renderString(display, posLabel = "Current Chat")
    }

    @JvmStatic
    fun onCloseChat() {
        lastClosedChatTime = SimpleTimeMark.now()
    }

    private fun isEnabled() = config.currentChatDisplay

    enum class ChatType(
        color: LorenzColor? = null,
        chatName: String? = null,
        displayName: String? = null,
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
            fun fromName(name: String) = entries.find { it.chatName.equals(name, true) } ?: error("unknown chat type '$name'")
        }
    }

}
