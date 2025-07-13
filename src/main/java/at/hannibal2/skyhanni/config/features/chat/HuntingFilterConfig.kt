package at.hannibal2.skyhanni.config.features.chat

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class HuntingFilterConfig {

    @Expose
    @ConfigOption(name = "Redundant Comments", desc = "Hide redundant comments from successfully hunting shards.")
    @SearchTag("panda mochibear invisibug")
    @ConfigEditorBoolean
    var redundantComments: Boolean = false

}
