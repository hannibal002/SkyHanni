package at.hannibal2.skyhanni.config.features.event;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class PrimalFearSolverConfig {
    @Expose
    @ConfigOption(name = "Math", desc = "Solver for the Math Primal Fear.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean math = false;

    @Expose
    @ConfigOption(name = "Public Speaking", desc = "Solver for the Public Speaking Primal Fear.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean publicSpeaking = false;
}
