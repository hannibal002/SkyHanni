package at.hannibal2.skyhanni.config.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.OSUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class CFStrayRabbitWarningConfig {
    @Expose
    @ConfigOption(name = "Warning Level", desc = "Warn when stray rabbits of a certain tier appear.")
    @ConfigEditorDropdown
    var rabbitWarningLevel: StrayTypeEntry = StrayTypeEntry.ALL

    @Expose
    @ConfigOption(name = "Highlight Color", desc = "Choose the color that stray rabbits should be highlighted as.")
    @ConfigEditorColour
    var inventoryHighlightColor: ChromaColour = LorenzColor.RED.toChromaColor()

    @Expose
    @ConfigOption(
        name = "Block Closing",
        desc = "Block closing the Chocolate Factory while there is a stray active.\n" +
            "§eHold §cShift §eto bypass"
    )
    @ConfigEditorBoolean
    var blockClosing: Boolean = false

    @Expose
    @ConfigOption(
        name = "Warning Sound",
        desc = "The sound that plays for a special rabbit.\n" +
            "§eYou can use custom sounds, put it in the §bskyhanni/sounds §efolder in your resource pack.\n" +
            "§eThen write §bskyhanni:yourfilename\n" +
            "§cMust be a .ogg file"
    )
    @ConfigEditorText
    val specialRabbitSound: Property<String> = Property.of("note.pling")

    @Expose
    @ConfigOption(name = "Repeat Sound", desc = "How many times the sound should be repeated.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 1f)
    var repeatSound: Int = 20

    @Expose
    @ConfigOption(name = "Flash Screen", desc = "Choose the stray rabbit type to flash the screen for.")
    @ConfigEditorDropdown
    var flashScreenLevel: StrayTypeEntry = StrayTypeEntry.SPECIAL

    enum class StrayTypeEntry(private val displayName: String) {
        SPECIAL("Special Only"),
        LEGENDARY_P("§6Legendary§7+"),
        EPIC_P("§5Epic§7+"),
        RARE_P("§9Rare§7+"),
        UNCOMMON_P("§aUncommon§7+"),
        ALL("All"),
        NONE("None"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Flash Color", desc = "Color of the screen when flashing")
    @ConfigEditorColour
    var flashColor: ChromaColour = ChromaColour.fromStaticRGB(0, 238, 255, 127)

    @ConfigOption(name = "Sounds", desc = "Click to open the list of available sounds.")
    @ConfigEditorButton(buttonText = "OPEN")
    val sounds: Runnable = Runnable(OSUtils::openSoundsListInBrowser)
}
