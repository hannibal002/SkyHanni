package at.hannibal2.skyhanni.config.features.garden.visitor

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class VisitorConfig {
    @Expose
    @ConfigOption(name = "Visitor Timer", desc = "")
    @Accordion
    val timer: TimerConfig = TimerConfig()

    @Expose
    @ConfigOption(name = "Visitor Shopping List", desc = "")
    @Accordion
    val shoppingList: ShoppingListConfig = ShoppingListConfig()

    @Expose
    @ConfigOption(name = "Visitor Inventory", desc = "")
    @Accordion
    val inventory: VisitorInventoryConfig = VisitorInventoryConfig()

    @Expose
    @ConfigOption(name = "Visitor Reward Warning", desc = "")
    @Accordion
    val rewardWarning: RewardWarningConfig = RewardWarningConfig()

    @Expose
    @ConfigOption(name = "Visitor Drops Statistics Counter", desc = "")
    @Accordion
    val dropsStatistics: DropsStatisticsConfig = DropsStatisticsConfig()

    @Expose
    @ConfigOption(name = "Notification Chat", desc = "Show in chat when a new visitor is visiting your island.")
    @ConfigEditorBoolean
    @FeatureToggle
    var notificationChat: Boolean = true

    @Expose
    @ConfigOption(name = "Compact Chat", desc = "Compact reward summary messages when you accept an offer.")
    @ConfigEditorBoolean
    @FeatureToggle
    var compactRewardChat: Boolean = false

    @Expose
    @ConfigOption(name = "Notification Title", desc = "Show a title when a new visitor is visiting your island.")
    @ConfigEditorBoolean
    @FeatureToggle
    var notificationTitle: Boolean = true

    @Expose
    @ConfigOption(
        name = "Highlight Status",
        desc = "Highlight the status for visitors with a text above or with color."
    )
    @ConfigEditorDropdown
    var highlightStatus: HighlightMode = HighlightMode.BOTH

    enum class HighlightMode(private val displayName: String) {
        COLOR("Color Only"),
        NAME("Name Only"),
        BOTH("Both"),
        DISABLED("Disabled"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Hypixel Message",
        desc = "Hide the chat message from Hypixel that a new visitor has arrived at your garden."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hypixelArrivedMessage: Boolean = true

    @Expose
    @ConfigOption(
        name = "Hide Chat",
        desc = "Hide chat messages from the visitors in the garden. (Except Beth, Maeve, and Spaceman)"
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideChat: Boolean = true

    @Expose
    @ConfigOption(
        name = "Accept Hotkey",
        desc = "Accept a visitor when you press this keybind while in the visitor GUI.\n" +
            "§eUseful for getting Ephemeral Gratitudes during the 2023 Halloween event."
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var acceptHotkey: Int = Keyboard.KEY_NONE


    @Expose
    @ConfigOption(name = "Highlight Visitors in SkyBlock", desc = "Highlight visitors outside of the Garden.")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightVisitors: Boolean = false

    @Expose
    @ConfigOption(
        name = "Block Interacting with Visitors",
        desc = "Prevent interacting with / unlocking Visitors to allow for Dedication Cycling."
    )
    @ConfigEditorDropdown
    var blockInteracting: VisitorBlockBehaviour = VisitorBlockBehaviour.DONT

    enum class VisitorBlockBehaviour(val displayName: String) {
        DONT("Don't"),
        ALWAYS("Always"),
        ONLY_ON_BINGO("Only on Bingo"),
        ;

        override fun toString() = displayName
    }
}
