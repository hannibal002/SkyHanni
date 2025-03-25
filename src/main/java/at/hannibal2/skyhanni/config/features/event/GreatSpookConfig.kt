package at.hannibal2.skyhanni.config.features.event

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GreatSpookConfig {
    @Expose
    @ConfigOption(name = "Primal Fear Timer", desc = "Show cooldown timer for next Primal Fear.")
    @ConfigEditorBoolean
    @FeatureToggle
    var primalFearTimer: Boolean = false

    @Expose
    @ConfigOption(name = "Primal Fear Notify", desc = "Play a notification sound when the next Primal Fear can spawn.")
    @ConfigEditorBoolean
    @FeatureToggle
    var primalFearNotification: Boolean = false

    @Expose
    @ConfigLink(owner = GreatSpookConfig::class, field = "primalFearTimer")
    var positionTimer: Position = Position(20, 20, false, true)

    @Expose
    @ConfigOption(
        name = "Fear Stat Display",
        desc = "Show Fear stat as single GUI element.\n" +
            "§eRequires tab list widget enabled and Fear selected to update live."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var fearStatDisplay: Boolean = false

    @Expose
    @ConfigLink(owner = GreatSpookConfig::class, field = "fearStatDisplay")
    var positionFear: Position = Position(30, 30, false, true)

    @Expose
    @ConfigOption(name = "IRL Time Left", desc = "Show the IRL time left before The Great Spook ends.")
    @ConfigEditorBoolean
    @FeatureToggle
    var greatSpookTimeLeft: Boolean = false

    @Expose
    @ConfigLink(owner = GreatSpookConfig::class, field = "greatSpookTimeLeft")
    var positionTimeLeft: Position = Position(40, 40, false, true)

    @ConfigOption(name = "Primal Fear Solvers", desc = "Solvers for the Primal Fears.")
    @Accordion
    @Expose
    var primalFearSolver: PrimalFearSolverConfig = PrimalFearSolverConfig()
}
