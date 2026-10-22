package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.ConfigEditorKeyMapping
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.features.garden.farming.GardenCustomKeybinds
import at.hannibal2.skyhanni.utils.InputKeys
import com.google.gson.annotations.Expose
import com.mojang.blaze3d.platform.InputConstants
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class KeyBindConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Use custom keybinds while holding a farming tool in your hand.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Include Squeaky Mousemat",
        desc = "Also use custom keybinds while holding a Squeaky Mousemat in your hand.\n" +
            "§eRequires main toggle to be enabled!",
    )
    @ConfigEditorBoolean
    var mousemat: Boolean = false

    @Expose
    @ConfigOption(
        name = "Include Vacuum",
        desc = "Also use custom keybinds while holding a Vacuum in your hand.\n" +
            "§eRequires main toggle to be enabled!",
    )
    @ConfigEditorBoolean
    var vacuum: Boolean = false

    @Expose
    @ConfigOption(
        name = "Include Fishing Rod",
        desc = "Also use custom keybinds while holding a fishing rod in your hand.\n" +
            "§eRequires main toggle to be enabled!",
    )
    @ConfigEditorBoolean
    var fishingRod: Boolean = false

    @Expose
    @ConfigOption(
        name = "Include Empty Hand with Sun's Grasp",
        desc = "Also use custom keybinds while holding nothing in your hand if you have a Sun's Grasp equipped.\n" +
            "§eRequires main toggle to be enabled!",
    )
    @ConfigEditorBoolean
    var sunsGrasp: Boolean = true

    // TODO Rename excludeBarn to excludeUnfarmablePlots
    @Expose
    @ConfigOption(name = "Exclude Unfarmable Plots", desc = "Disable this feature while on the barn plot or in a greenhouse.")
    @ConfigEditorBoolean
    var excludeBarn: Boolean = false

    @ConfigOption(name = "Disable All", desc = "Disable all keys.")
    @ConfigEditorButton(buttonText = "Disable")
    val presetDisable: Runnable = Runnable(GardenCustomKeybinds::disableAll)

    @ConfigOption(name = "Set Default", desc = "Reset all keys to default.")
    @ConfigEditorButton(buttonText = "Default")
    val presetDefault: Runnable = Runnable(GardenCustomKeybinds::defaultAll)

    @Expose
    @ConfigOption(name = "Attack", desc = "")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.MOUSE_BUTTON_LEFT, type = MOUSE)
    val attack: Property<InputConstants.Key> = Property.of(InputKeys.MOUSE_BUTTON_LEFT)

    @Expose
    @ConfigOption(name = "Use Item", desc = "")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.MOUSE_BUTTON_RIGHT, type = MOUSE)
    val useItem: Property<InputConstants.Key> = Property.of(InputKeys.MOUSE_BUTTON_RIGHT)

    @Expose
    @ConfigOption(name = "Move Left", desc = "")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_A)
    val left: Property<InputConstants.Key> = Property.of(InputKeys.KEY_A)

    @Expose
    @ConfigOption(name = "Move Right", desc = "")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_D)
    val right: Property<InputConstants.Key> = Property.of(InputKeys.KEY_D)

    @Expose
    @ConfigOption(name = "Move Forward", desc = "")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_W)
    val forward: Property<InputConstants.Key> = Property.of(InputKeys.KEY_W)

    @Expose
    @ConfigOption(name = "Move Back", desc = "")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_S)
    val back: Property<InputConstants.Key> = Property.of(InputKeys.KEY_S)

    @Expose
    @ConfigOption(name = "Jump", desc = "")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_SPACE)
    val jump: Property<InputConstants.Key> = Property.of(InputKeys.KEY_SPACE)

    @Expose
    @ConfigOption(name = "Sneak", desc = "")
    @ConfigEditorKeyMapping(defaultKey = InputConstants.KEY_LSHIFT)
    val sneak: Property<InputConstants.Key> = Property.of(InputKeys.KEY_LSHIFT)
}
