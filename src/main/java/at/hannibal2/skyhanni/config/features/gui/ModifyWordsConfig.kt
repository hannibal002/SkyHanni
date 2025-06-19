package at.hannibal2.skyhanni.config.features.gui

import at.hannibal2.skyhanni.config.FeatureToggle
//#if TODO
import at.hannibal2.skyhanni.features.misc.visualwords.VisualWordGui
//#endif
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

// todo 1.21 impl needed
class ModifyWordsConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Enable replacing all instances of a word or phrase with another word or phrase."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = true

    //#if TODO
    @ConfigOption(name = "Open Config", desc = "Open the menu to setup the visual words.\n§eCommand: /shwords")
    @ConfigEditorButton(buttonText = "Open")
    var open: Runnable = Runnable(VisualWordGui::onCommand)
    //#endif
}
