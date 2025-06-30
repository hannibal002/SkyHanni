package at.hannibal2.skyhanni.config.features.dev

import TestCanSeeFaceConfig
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class DevToolConfig {
    @Expose
    @ConfigOption(name = "Graph Tools", desc = "")
    @Accordion
    val graph: GraphConfig = GraphConfig()

    @Expose
    @ConfigOption(name = "LOS Face Test", desc = "")
    @Accordion
    val canSeeFace: TestCanSeeFaceConfig = TestCanSeeFaceConfig()
}
