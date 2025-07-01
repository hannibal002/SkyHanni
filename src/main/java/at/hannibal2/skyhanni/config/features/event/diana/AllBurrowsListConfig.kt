package at.hannibal2.skyhanni.config.features.event.diana

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class AllBurrowsListConfig {
    @Expose
    @ConfigOption(
        name = "Save Found Burrows",
        desc = "Save the location of every found burrow in a locally stored list."
    )
    @ConfigEditorBoolean
    var save: Boolean = true

    @Expose
    @ConfigOption(name = "Show All Burrows", desc = "Show the list of all ever found burrows in the world.")
    @ConfigEditorBoolean
    var showAll: Boolean = false
}
