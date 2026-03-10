package at.hannibal2.skyhanni.config.features.fishing

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class BarnTimerConfig {
    @Expose
    @ConfigOption(
        name = "Barn Fishing Timer",
        desc = "Show the time and amount of own/global sea creatures nearby while barn fishing.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Show Anywhere",
        desc = "Show the Barn Fishing Timer whenever you fish up a sea creature, regardless of location.",
    )
    @ConfigEditorBoolean
    var showAnywhere: Boolean = false

    @Expose
    @ConfigOption(name = "Worm Fishing", desc = "Show the Barn Fishing Timer in the Crystal Hollows.")
    @ConfigEditorBoolean
    val crystalHollows: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Lava Fishing", desc = "Show the Barn Fishing Timer in the Crimson Isle.")
    @ConfigEditorBoolean
    val crimsonIsle: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Warn Personal Cap", desc = "Warns you when you reach your personal sea creature cap.")
    @ConfigEditorBoolean
    var warnPersonalCap: Boolean = true

    @Expose
    @ConfigOption(
        name = "Warn Global Cap",
        desc = "Warns you when you reach the global sea creature cap (Only works if all mobs are in range)."
    )
    @ConfigEditorBoolean
    var warnGlobalCap: Boolean = true

    @Expose
    @ConfigOption(name = "Time Alert", desc = "Warns you when the fishing timer reaches a certain value.")
    @ConfigEditorBoolean
    var timeAlert: Boolean = true

    @Expose
    @ConfigOption(
        name = "Time Alert Seconds",
        desc = "The time in seconds to alert you at.\n" +
            "§cNote: sea creatures despawn after 6 minutes, (360s).",
    )
    @ConfigEditorSlider(minValue = 240f, maxValue = 360f, minStep = 1f)
    var alertTime: Int = 300

    @Expose
    @ConfigOption(name = "Winter Fishing", desc = "Show the Barn Fishing Timer on the Jerry's Workshop island.")
    @ConfigEditorBoolean
    val winterIsland: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Stranded Fishing",
        desc = "Show the Barn Fishing Timer on all the different islands that Stranded players can visit.",
    )
    @ConfigEditorBoolean
    val forStranded: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigLink(owner = BarnTimerConfig::class, field = "enabled")
    val pos: Position = Position(10, 10)
}
