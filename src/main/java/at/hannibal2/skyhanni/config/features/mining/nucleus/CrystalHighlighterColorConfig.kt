package at.hannibal2.skyhanni.config.features.mining.nucleus

import at.hannibal2.skyhanni.config.storage.Resettable
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class CrystalHighlighterColorConfig : Resettable() {
    @Expose
    @ConfigOption(name = "Amber", desc = "§7Default: §#§c§b§6§4§0§0§/#CB6400")
    @ConfigEditorColour
    val amber: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(203, 100, 0, 127))

    @Expose
    @ConfigOption(name = "Amethyst", desc = "§7Default: §#§a§8§0§b§c§b§/#A80BCB")
    @ConfigEditorColour
    val amethyst: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(168, 11, 203, 127))

    @Expose
    @ConfigOption(name = "Topaz", desc = "§7Default: §#§c§d§c§4§0§0§/#CDC400")
    @ConfigEditorColour
    val topaz: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(205, 196, 0, 127))

    @Expose
    @ConfigOption(name = "Jade", desc = "§7Default: §#§6§8§c§b§0§0§/#68CB00")
    @ConfigEditorColour
    val jade: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(104, 203, 0, 85))

    @Expose
    @ConfigOption(name = "Sapphire", desc = "§7Default: §#§2§9§6§4§c§b§/#2964CB")
    @ConfigEditorColour
    val sapphire: Property<ChromaColour> = Property.of(ChromaColour.fromStaticRGB(41, 100, 203, 127))

    @ConfigOption(name = "Reset Colors", desc = "Reset all colors to their default values.")
    @ConfigEditorButton(buttonText = "Reset")
    val reset: Runnable = Runnable(::reset)
}
