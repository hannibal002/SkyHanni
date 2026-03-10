package at.hannibal2.skyhanni.config.features.event

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class YearOfTheWitchConfig {

    @Expose
    @ConfigOption(
        name = "Stew Helper",
        desc = "Shows your progress towards the Witches Stews.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var stewHelper: Boolean = true

    @Expose
    @ConfigLink(owner = YearOfTheWitchConfig::class, field = "stewHelper")
    val stewHelperPosition: Position = Position(174, 139)

    @Expose
    @ConfigOption(
        name = "Stew Highlighter",
        desc = "Highlights eaten Stew in green and Stew you can eat in yellow.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var stewHighlighter: Boolean = true
}
