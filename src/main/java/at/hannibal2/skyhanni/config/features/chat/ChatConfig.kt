package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import org.lwjgl.input.Keyboard

class ChatConfig {

    @Expose
    @ConfigOption(name = "Peek Chat", desc = "Hold this key to keep the chat open.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_Z)
    var peekChat: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(
        name = "Copy Chat",
        desc = "Right click a chat message to copy it. Holding Shift will copy the message with " +
            "Shwords applied, and holding Ctrl will copy only one line.\n" +
            "§cNote: Will not work correctly with the Chatting mod.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var copyChat: Boolean = false

    @Expose
    @ConfigOption(
        name = "Current Chat Display",
        desc = "Displays a GUI element that indicates what chat you are in (e.g. Party, Guild, Coop, All).",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var currentChatDisplay: Boolean = true

    @Expose
    @ConfigLink(owner = ChatConfig::class, field = "currentChatDisplay")
    val currentChatDisplayPos: Position = Position(3, -21)

    @Expose
    @ConfigOption(
        name = "Shorten Coin Amounts",
        desc = "Replace coin amounts in chat messages with their shortened version.\n" +
            "e.g. §65,100,000 Coins §7-> §65.1M Coins",
    )
    @ConfigEditorBoolean
    @SearchTag("format")
    @FeatureToggle
    var shortenCoinAmounts: Boolean = false

    @Expose
    @ConfigOption(name = "SkyBlock XP in Chat", desc = "Send the SkyBlock XP messages into the chat.")
    @ConfigEditorBoolean
    @FeatureToggle
    var skyBlockXPInChat: Boolean = true

    // Categories
    @Category(name = "Chat Filters", desc = "Configure hidden messages.")
    @Expose
    val filterType: FilterTypesConfig = FilterTypesConfig()

    // TODO jawbus + thunder <- Someone else's todo
    @Category(name = "Compact Chat", desc = "Configure compacted messages.")
    @Expose
    val compact: CompactConfig = CompactConfig()

    @Category(name = "Player Messages", desc = "Configure player messages.")
    @Expose
    val playerMessage: PlayerMessagesConfig = PlayerMessagesConfig()

    @Category(name = "Rare Drop Messages", desc = "Configure rare drop messages.")
    @Expose
    val rareDropMessages: RareDropMessagesConfig = RareDropMessagesConfig()

    @Category(name = "Sound Responses", desc = "Configure sound responses.")
    @Expose
    val soundResponse: ChatSoundResponseConfig = ChatSoundResponseConfig()

    @Category(name = "Translator", desc = "Chat translator settings.")
    @Expose
    val translator: TranslatorConfig = TranslatorConfig()
}
