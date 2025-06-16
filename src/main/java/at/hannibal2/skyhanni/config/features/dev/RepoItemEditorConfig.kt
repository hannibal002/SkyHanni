package at.hannibal2.skyhanni.config.features.dev

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
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

    // todo save npc

    @Expose
    @ConfigOption(
        name = "Refresh NBT Keybind",
        desc = "Instantly updates the nbt of the item to match the current file on the computer.",
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var refreshNbtKeybind: Int = Keyboard.KEY_NONE

}
