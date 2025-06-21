package at.hannibal2.skyhanni.config.features.dev

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class RepoItemEditorConfig {

    @Expose
    @ConfigOption(
        name = "Edit Mode",
        desc = "Enables you to edit repo items for the item repository.\n" +
            "§cOnly turn on if you know what you are doing!\n" +
            "§eThis option is required for the following keybinds to work!",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var editModeEnabled: Boolean = false

    @Expose
    @ConfigOption(name = "Neu Repository", desc = "")
    @Accordion
    var repo: NeuRepositoryConfig = NeuRepositoryConfig()

    @Expose
    @ConfigOption(
        name = "Highlight Missing Repo Items",
        desc = "Highlights each item in the current inventory that is not in your current NEU repo."
    )
    @ConfigEditorBoolean
    var highlightMissingRepo: Boolean = false

    @Expose
    @ConfigOption(
        name = "Edit Mode Keybind",
        desc = "Keybind to open the repo item editor GUI.",
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var openRepoItemEditorKeybind: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(
        name = "Instant Edit Keybind",
        desc = "Keybind to instantly edit the item without opening the repo item editor GUI.",
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var instantEditKeybind: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(
        name = "Save Recipe Keybind",
        desc = "Saves the currently open recipe to the item repo.",
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var saveRecipeKeybind: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(
        name = "Load Inventory as Trades",
        desc = "Keybind will attempt to load the currently open menu as a NPC trades menu.\n" +
            "§eFor inventories such as the community shop or Anita that are missing the sell item button",
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var loadInventoryAsTradesKeybind: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(
        name = "Refresh NBT Keybind",
        desc = "Instantly updates the nbt of the item to match the current file on the computer.",
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var refreshNbtKeybind: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigLink(owner = RepoItemEditorConfig::class, field = "editModeEnabled")
    var displayPosition: Position = Position(-300, 140)

}
