package at.hannibal2.skyhanni.config.features.mining.nucleus

import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class CrystalHighlighterColorConfig {
    @Expose
    @ConfigOption(name = "Amber", desc = "§7Default: §#§c§b§6§4§0§0§/#CB6400")
    @ConfigEditorColour
    var amber: Property<String> = Property.of("0:127:203:100:0")

    @Expose
    @ConfigOption(name = "Amethyst", desc = "§7Default: §#§a§8§0§b§c§b§/#A80BCB")
    @ConfigEditorColour
    var amethyst: Property<String> = Property.of("0:127:168:11:203")

    @Expose
    @ConfigOption(name = "Topaz", desc = "§7Default: §#§c§d§c§4§0§0§/#CDC400")
    @ConfigEditorColour
    var topaz: Property<String> = Property.of("0:127:205:196:0")

    @Expose
    @ConfigOption(name = "Jade", desc = "§7Default: §#§6§8§c§b§0§0§/#68CB00")
    @ConfigEditorColour
    var jade: Property<String> = Property.of("0:85:104:203:0")

    @Expose
    @ConfigOption(name = "Sapphire", desc = "§7Default: §#§2§9§6§4§c§b§/#2964CB")
    @ConfigEditorColour
    var sapphire: Property<String> = Property.of("0:127:41:100:203")
}
