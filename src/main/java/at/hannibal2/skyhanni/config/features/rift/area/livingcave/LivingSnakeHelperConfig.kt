package at.hannibal2.skyhanni.config.features.rift.area.livingcave

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.SearchTag

class LivingSnakeHelperConfig {

    @Expose
    @ConfigOption(
        name = "Highlight Snakes",
        desc = "Highlight the Living Metal Snakes in colors depending on the state of the snake.\n" +
            "Shows head or tail, depending on item in hand.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var highlight: Boolean = true

    @Expose
    @ConfigOption(
        name = "Solo Mode",
        desc = "Highlights both the head and tail if both pickaxe and pungi are in hotbar."
    )
    @SearchTag("snake")
    @ConfigEditorBoolean
    var solo: Boolean = true
}
