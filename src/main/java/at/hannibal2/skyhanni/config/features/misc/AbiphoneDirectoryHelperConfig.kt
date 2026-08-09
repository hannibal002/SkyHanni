package at.hannibal2.skyhanni.config.features.misc

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class AbiphoneDirectoryHelperConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable the Abiphone Directory Helper feature")
    @ConfigEditorBoolean
    var enabled: Boolean = true

    @Expose
    @ConfigOption(name = "Wiki on Click", desc = "Open the wiki for the contact when clicking on it")
    @ConfigEditorBoolean
    var wikiOnClick: Boolean = true
}
