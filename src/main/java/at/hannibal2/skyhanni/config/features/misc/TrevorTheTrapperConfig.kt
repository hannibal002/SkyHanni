package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class TrevorTheTrapperConfig {
    @Expose
    @ConfigOption(
        name = "Enable Data Tracker",
        desc = "Track all of your data from doing Trevor Quests.\n" +
            "Shows based on the setting below."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var dataTracker: Boolean = true

    @Expose
    @ConfigOption(
        name = "Show Between Quests",
        desc = "Show the tracker during and between quests otherwise it will only show during them.\n" +
            "Will show in the Trapper's Den regardless. §cToggle 'Enable Data Tracker' above."
    )
    @ConfigEditorBoolean
    var displayType: Boolean = true

    @Expose
    @ConfigOption(name = "Text Format", desc = "Drag text to change the appearance of the overlay.")
    @ConfigEditorDraggableList
    val textFormat: MutableList<TrackerEntry> = mutableListOf(
        TrackerEntry.TITLE,
        TrackerEntry.QUESTS_STARTED,
        TrackerEntry.TOTAL_PELTS,
        TrackerEntry.PELTS_PER_HOUR,
        TrackerEntry.SPACER_1,
        TrackerEntry.KILLED,
        TrackerEntry.TRACKABLE,
        TrackerEntry.UNTRACKABLE,
        TrackerEntry.UNDETECTED,
        TrackerEntry.ENDANGERED,
        TrackerEntry.ELUSIVE
    )

    enum class TrackerEntry(private val displayName: String) {
        TITLE("§b§lTrevor Data Tracker"),
        QUESTS_STARTED("§b1,428 §9Quests Started"),
        TOTAL_PELTS("§b11,281 §5Total Pelts Gained"),
        PELTS_PER_HOUR("§b2,448 §5Pelts Per Hour"),
        SPACER_1(""),
        KILLED("§b850 §cKilled Animals"),
        SELF_KILLING("§b153 §cSelf Killing Animals"),
        TRACKABLE("§b788 §fTrackable Animals"),
        UNTRACKABLE("§b239 §aUntrackable Animals"),
        UNDETECTED("§b115 §9Undetected Animals"),
        ENDANGERED("§b73 §5Endangered Animals"),
        ELUSIVE("§b12 §6Elusive Animals"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigLink(owner = TrevorTheTrapperConfig::class, field = "dataTracker")
    val position: Position = Position(10, 80)

    @Expose
    @ConfigOption(
        name = "Trapper Solver",
        desc = "Assist in finding Trevor's mobs.\n" +
            "§eNote: May not always work as expected.\n" +
            "§cWill not help you to find rabbits or sheep in the Oasis!"
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var solver: Boolean = true

    @Expose
    @ConfigOption(name = "Mob Dead Warning", desc = "Show a message when Trevor's mob dies.")
    @ConfigEditorBoolean
    var mobDiedMessage: Boolean = true

    @Expose
    @ConfigOption(name = "Warp to Trapper", desc = "Warp to Trevor's Den. Works only inside the Farming Islands.")
    @ConfigEditorBoolean
    @FeatureToggle
    var warpToTrapper: Boolean = false

    @Expose
    @ConfigOption(
        name = "Accept Trapper Quest",
        desc = "Click this key after the chat prompt to accept Trevor's quest."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var acceptQuest: Boolean = false

    @Expose
    @ConfigOption(
        name = "Trapper Hotkey",
        desc = "Press this key to warp to Trevor's Den or to accept the quest. " +
            "§eRequires the relevant above settings to be toggled"
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var keyBind: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(name = "Trapper Cooldown", desc = "Change the color of Trevor and adds a cooldown over his head.")
    @ConfigEditorBoolean
    @FeatureToggle
    var cooldown: Boolean = true

    @Expose
    @ConfigOption(
        name = "Trapper Ready Title",
        desc = "Show a title and play a sound when the cooldown is over and Trapper is ready for the next quest."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var readyTitle: Boolean = true

    @Expose
    @ConfigOption(
        name = "Trapper Cooldown GUI",
        desc = "Show the cooldown on screen in an overlay (intended for Abiphone users)."
    )
    @ConfigEditorBoolean
    var cooldownGui: Boolean = false

    @Expose
    @ConfigLink(owner = TrevorTheTrapperConfig::class, field = "cooldownGui")
    val cooldownGuiPosition: Position = Position(10, 10)
}
