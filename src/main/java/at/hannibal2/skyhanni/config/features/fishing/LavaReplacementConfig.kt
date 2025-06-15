package at.hannibal2.skyhanni.config.features.fishing

import at.hannibal2.skyhanni.config.FeatureToggle
//#if TODO
import at.hannibal2.skyhanni.features.fishing.LavaReplacement.IslandsToReplace
//#endif
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

// todo 1.21 impl needed
class LavaReplacementConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Replace the lava texture with the water texture.")
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(
        name = "Replace Everywhere",
        desc = "Replace the lava texture In All Islands regardless of List Below."
    )
    @ConfigEditorBoolean
    val everywhere: Property<Boolean> = Property.of(true)

    //#if TODO
    @Expose
    @ConfigOption(name = "Islands", desc = "Islands to Replace Lava In.")
    @ConfigEditorDraggableList
    val islands: Property<MutableList<IslandsToReplace>> = Property.of(mutableListOf())
    //#endif
}
