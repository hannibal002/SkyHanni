package at.hannibal2.skyhanni.config.features.garden

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class WrongToolAlertConfig {

    @Expose
    @ConfigOption(
        name = "Wrong Tool Alert",
        desc = "Notifies you when you break a crop with the wrong specialized farming tool."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Show Title", desc = "Displays a title when the warning is sent.")
    @ConfigEditorBoolean
    var showTitle: Boolean = true

}
