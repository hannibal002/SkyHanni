package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SearchableStorageConfig {

    @ConfigOption(
        name = "Searchable Storage",
        desc = "For this feature to work properly you need to at least have opened each storage page once.",
    )
    @ConfigEditorInfoText
    var notice: String = ""

    @Expose
    var displayPosition: Position = Position(-300, 140)
}
