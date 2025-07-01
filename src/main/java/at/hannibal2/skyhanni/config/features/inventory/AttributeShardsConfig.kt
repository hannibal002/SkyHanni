package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class AttributeShardsConfig {

    @Expose
    @ConfigOption(name = "Highlight Disabled Attributes", desc = "Highlight disabled attributes in /attributemenu.")
    @ConfigEditorBoolean
    @FeatureToggle
    var highlightDisabledAttributes: Boolean = true

}
