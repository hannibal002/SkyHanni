package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class JacobFarmingContestConfig {
    @Expose
    @ConfigOption(
        name = "Unclaimed Rewards",
        desc = "Highlight contests with unclaimed rewards in the Jacob inventory."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightRewards: Boolean = true

    @Expose
    @ConfigOption(name = "Contest Time", desc = "Add the real time format to the Contest description.")
    @ConfigEditorBoolean
    @FeatureToggle
    var realTime: Boolean = true

    @Expose
    @ConfigOption(
        name = "Open On Elite",
        desc = "Open the contest on §eelitebot.dev§7 when pressing this key in Jacob's menu or the calendar."
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var openOnElite: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(
        name = "Medal Icon",
        desc = "Add a symbol that shows what medal you received in this Contest. " +
            "§eIf you use a texture pack this may cause conflicting icons."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var medalIcon: Boolean = true

    @Expose
    @ConfigOption(
        name = "Finnegan Icon",
        desc = "Use a different indicator for when the Contest happens during Mayor Finnegan."
    )
    @ConfigEditorBoolean
    var finneganIcon: Boolean = true
}
