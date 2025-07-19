package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ColorfulItemTooltips {

    @Expose
    @ConfigOption(
        name = "Color Item Stat Numbers",
        desc = "Changes the color of the numbers in item lore to the color they are in the SkyBlock stats menu.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Add stat icons",
        desc = "Adds in the stat icons after the stat number.",
    )
    @ConfigEditorBoolean
    var statIcons: Boolean = true

    @Expose
    @ConfigOption(
        name = "Replace percentages",
        desc = "Replaces the percentage sign with the stat icon if the stat ends with a percentage.\n" +
            "§eRequires add stat icons to be enabled.",
    )
    @ConfigEditorBoolean
    var replacePercentages: Boolean = false

    @Expose
    @ConfigOption(
        name = "Replace rift seconds",
        desc = "Replaces the 's' after the rift time stat.\n" +
            "§eRequires add stat icons to be enabled.",
    )
    @ConfigEditorBoolean
    var replaceRiftSeconds: Boolean = true

}
