package at.hannibal2.skyhanni.config.features.event;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class PrimalFearSolverConfig {
    @Expose
    @ConfigOption(name = "Math Primal Fear Solver", desc = "Solver for the Math Primal Fear")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean mathPrimalFear = false;

    @Expose
    @ConfigOption(name = "Public Speaking Primal Fear Solver", desc = "Solver for the Math Primal Fear")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean publicSpeakingPrimalFear = false;
}
