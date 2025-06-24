package at.hannibal2.skyhanni.config.features.rift.area.dreadfarm

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DreadfarmConfig {
    @Expose
    @ConfigOption(
        name = "Agaricus Cap",
        desc = "Count down the time until §eAgaricus Cap (Mushroom) " +
            "§7changes color from brown to red and is breakable."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var agaricusCap: Boolean = true

    @ConfigOption(name = "Volt Crux", desc = "")
    @Accordion
    @Expose
    var voltCrux: VoltCruxConfig = VoltCruxConfig()

    @ConfigOption(name = "Wilted Berberis", desc = "")
    @Accordion
    @Expose
    var wiltedBerberis: WiltedBerberisConfig = WiltedBerberisConfig()
}
