package at.hannibal2.skyhanni.config.features.dev

import at.hannibal2.skyhanni.config.NoConfigLink
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DevToolConfig {
    @Expose
    @ConfigOption(name = "Graph Tools", desc = "")
    @Accordion
    val graph: GraphConfig = GraphConfig()

    @Expose
    @NoConfigLink
    val chatProgressPosition: Position = Position(1, 160)
}
