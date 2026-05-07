package at.hannibal2.skyhanni.config.features.chat

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GrandFeastMessagesConfig {
    @Expose
    @ConfigOption(name = "Master Chef Ted", desc = "Hide messages from Ted about Kernels getting added to your purse while farming.")
    @ConfigEditorBoolean
    var masterChef: Boolean = false

    @Expose
    @ConfigOption(
        name = "Seasoning Kernels",
        desc = "Replace \"automatically donated\" with your Kernel count in the Seasoning Rare Crop message during a Grand Feast.")
    @ConfigEditorBoolean
    var seasoningKernels: Boolean = false
}
