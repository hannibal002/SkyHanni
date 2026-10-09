package at.hannibal2.skyhanni.config.features.event.hoppity

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.utils.OSUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.ChromaColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class HoppityCallWarningConfig {
    @Expose
    @ConfigOption(name = "Hoppity Call Warning", desc = "Warn when hoppity is calling you.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Warning Sound",
        desc = "The sound that plays when hoppity calls.\n" +
            "§eYou can use custom sounds, put it in the §bskyhanni/sounds §efolder in your resource pack.\n" +
            "§eThen write §bskyhanni:yourfilename\n" +
            "§cMust be a .ogg file",
    )
    @ConfigEditorText
    val hoppityCallSound: Property<String> = Property.of("block.note_block.pling")

    @Expose
    @ConfigOption(name = "Flash Color", desc = "Color of the screen when flashing")
    @ConfigEditorColour
    var flashColor: ChromaColour = ChromaColour.fromStaticRGB(0, 238, 255, 127)

    @ConfigOption(name = "Sounds", desc = "Click to open the list of available sounds.")
    @ConfigEditorButton(buttonText = "OPEN")
    val sounds: Runnable = Runnable(OSUtils::openSoundsListInBrowser)
}
