package at.hannibal2.skyhanni.config.features.combat.end

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class SummonCounterConfig {
    @Expose
    @ConfigOption(
        name = "Dragons Summoned",
        desc = "Counts the dragons you placed at least one summoning eye for.\n" +
            "§eOpen /dragon guide once to sync it with your real total."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigLink(owner = SummonCounterConfig::class, field = "enabled")
    val position: Position = Position(320, 200)
}
