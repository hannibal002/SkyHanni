package at.hannibal2.skyhanni.config.features.hunting

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GalateaMobHighlightConfig {
    @Expose
    @ConfigOption(name = "Birries Highlight", desc = "")
    @Accordion
    var birries = BirriesHighlightConfig()

    @Expose
    @ConfigOption(name = "Hideonleaf Highlight", desc = "")
    @Accordion
    var hideonleaf = HideonleafHighlightConfig()

    @Expose
    @ConfigOption(name = "Invisibug Highlight", desc = "")
    @Accordion
    var invisibug = InvisibugHighlightConfig()
}
