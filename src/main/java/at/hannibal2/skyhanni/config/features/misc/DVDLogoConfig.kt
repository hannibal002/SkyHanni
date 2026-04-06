package at.hannibal2.skyhanni.config.features.misc

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.OSUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class DVDLogoConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Render a \"DVD Logo\" that will bounce around the screen.")
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigLink(owner = DVDLogoConfig::class, field = "enabled")
    val position: Position = Position(50, 50)

    @Expose
    @ConfigOption(
        name = "Logo Text",
        desc = "The text that will be displayed.\n" +
            "§eUse '&' as the color code character.\n" +
            "§eUse '\\n' as the line break character."
    )
    @ConfigEditorText
    val text: Property<String> = Property.of("&zDVD")

    @Expose
    @ConfigOption(name = "Text Size", desc = "Size of logo text.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 0.5f)
    val textSize: Property<Float> = Property.of(1f)

    @Expose
    @ConfigOption(name = "Logo Speed", desc = "How fast the logo will move.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 0.5f)
    val speed: Property<Float> = Property.of(4f)

    @Expose
    @ConfigOption(
        name = "Edge Hit Sound",
        desc = "The sound that will play when the logo hits an edge of the screen.\nClear this field to disable."
    )
    @ConfigEditorText
    val edgeHitSound: Property<String> = Property.of("block.note_block.pling")

    @Expose
    @ConfigOption(
        name = "Corner Hit Sound",
        desc = "The sound that will play when the logo hits a corner of the screen.\nClear this field to disable."
    )
    @ConfigEditorText
    val cornerHitSound: Property<String> = Property.of("entity.experience_orb.pickup")

    @ConfigOption(name = "Sounds", desc = "Click to open the list of available sounds.")
    @ConfigEditorButton(buttonText = "OPEN")
    val sounds: Runnable = Runnable(OSUtils::openSoundsListInBrowser)
}
