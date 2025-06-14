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
            "§eOnly turn on if you know what you are doing!",
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

}
