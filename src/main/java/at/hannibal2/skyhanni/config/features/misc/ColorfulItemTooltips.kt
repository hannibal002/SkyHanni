package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ColorfulItemTooltips {

    @Expose
    @ConfigOption(
        name = "Add Stat Icons",
        desc = "Adds in the stat icons after the stat number.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var statIcons: Boolean = false

    @Expose
    @ConfigOption(
        name = "Replace Percentages",
        desc = "Replaces the percentage sign with the stat icon if the stat ends with a percentage.\n" +
            "§eRequires add stat icons to be enabled.",
    )
    @ConfigEditorBoolean
    var replacePercentages: Boolean = false

    @Expose
    @ConfigOption(
        name = "Replace Rift Seconds",
        desc = "Replaces the 's' after the rift time stat.\n" +
            "§eRequires add stat icons to be enabled.",
    )
    @ConfigEditorBoolean
    var replaceRiftSeconds: Boolean = true

}
