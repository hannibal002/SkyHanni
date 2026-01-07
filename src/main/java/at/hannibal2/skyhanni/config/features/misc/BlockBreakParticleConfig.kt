package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class BlockBreakParticleConfig {

    @JvmField
    @Expose
    @ConfigOption(name = "Block Break Particles", desc = "Hide Block Break particles.")
    @SearchTag("breaking")
    @ConfigEditorBoolean
    @FeatureToggle
    var hide: Boolean = false

    @JvmField
    @Expose
    @ConfigOption(name = "Only on Garden", desc = "Hide Block Break particles only on the Garden.")
    @SearchTag("breaking")
    @ConfigEditorBoolean
    @FeatureToggle
    var onlyInGarden: Boolean = false
}
