package at.hannibal2.skyhanni.config.features.hunting

import at.hannibal2.skyhanni.config.OnlyModern
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GalateaMobHighlightConfig {
    @Expose
    @ConfigOption(name = "Birries Highlight", desc = "")
    @OnlyModern
    @Accordion
    var birries = BirriesHighlightConfig()

    @Expose
    @ConfigOption(name = "Hideonleaf Highlight", desc = "")
    @OnlyModern
    @Accordion
    var hideonleaf = HideonleafHighlightConfig()

    @Expose
    @ConfigOption(name = "Invisibug Highlight", desc = "")
    @OnlyModern
    @Accordion
    var invisibug = InvisibugHighlightConfig()
}
