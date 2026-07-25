package at.hannibal2.skyhanni.config.features.chat

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag
import io.github.notenoughupdates.moulconfig.observer.Property

class HuntingFilterConfig {

    @Expose
    @ConfigOption(name = "Redundant Comments", desc = "Hide redundant comments from successfully hunting shards.")
    @SearchTag("panda mochibear invisibug")
    @ConfigEditorBoolean
    val redundantComments: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Swoop Huntaxe", desc = "Hide Swoop's message about monsters only taking damage from axes.")
    @ConfigEditorBoolean
    val swoopAxeMessage: Property<Boolean> = Property.of(false)

}
