package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.HasLegacyId
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.input.Keyboard

class TooltipTweaksConfig {
    @Expose
    @ConfigOption(
        name = "Compact Descriptions",
        desc = "Hide redundant parts of reforge descriptions, generic counter description, and Farmhand perk explanation."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var compactToolTooltips: Boolean = false

    @Expose
    @ConfigOption(
        name = "Breakdown Hotkey",
        desc = "When the keybind is pressed, show a breakdown of all fortune sources on a tool."
    )
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_LSHIFT)
    var fortuneTooltipKeybind: Int = Keyboard.KEY_LSHIFT

    @Expose
    @ConfigOption(
        name = "Tooltip Format",
        desc = "Show crop-specific Farming Fortune in tooltip.\n" +
            "§fShow: §7Crop-specific Fortune indicated as §6[+196]\n" +
            "§fReplace: §7Edits the total Fortune to include crop-specific Fortune."
    )
    @ConfigEditorDropdown
    var cropTooltipFortune: CropTooltipFortuneEntry = CropTooltipFortuneEntry.SHOW

    enum class CropTooltipFortuneEntry(
        private val displayName: String,
        private val legacyId: Int = -1
    ) : HasLegacyId {
        DEFAULT("Default", 0),
        SHOW("Show", 1),
        REPLACE("Replace", 2),
        ;

        override fun getLegacyId() = legacyId
        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Total Crop Milestone",
        desc = "Show the progress bar till maxed crop milestone in the crop milestone inventory."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var cropMilestoneTotalProgress: Boolean = true
}
