package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerNameSource
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class HideArmorConfig {
    @Expose
    @ConfigOption(name = "Player Selection", desc = "Select which players to hide armor for.")
    @ConfigEditorDraggableList
    val playerSelection: Property<MutableList<PlayerNameSource>> = Property.of(mutableListOf())

    @Expose
    @ConfigOption(
        name = "Invert Selection",
        desc = "Hide armor for players not included in the selection."
    )
    @ConfigEditorBoolean
    var invertSelection: Boolean = false

    @Expose
    @ConfigOption(name = "Only Helmet", desc = "Only hide the helmet.")
    @ConfigEditorBoolean
    var onlyHelmet: Boolean = false
}
