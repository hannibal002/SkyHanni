package at.hannibal2.skyhanni.config.features.chat

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class ForagingFilterConfig {

    @Expose
    @ConfigOption(name = "Unmineable Trees", desc = "Hide messages from trying to cut down an unmineable tree.")
    @ConfigEditorBoolean
    var unmineable: Boolean = false

}
