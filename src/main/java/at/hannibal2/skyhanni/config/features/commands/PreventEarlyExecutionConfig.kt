package at.hannibal2.skyhanni.config.features.commands

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class PreventEarlyExecutionConfig {

    @Expose
    @ConfigOption(
        name = "Prevent Early Command Execution",
        desc = "Prevent commands from executing before the Server cooldown has ended."
    )
    @ConfigEditorBoolean
    var preventEarlyExecution: Boolean = true

}
