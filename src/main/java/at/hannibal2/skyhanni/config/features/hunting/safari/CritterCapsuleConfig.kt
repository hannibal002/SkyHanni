package at.hannibal2.skyhanni.config.features.hunting.safari

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class CritterCapsuleConfig {

    @Expose
    @ConfigOption(
        name = "Hide Flying Capsules",
        desc = "Hide thrown Critter Capsules while they are in the air. " +
            "Masterful Critter Capsules are only hidden while they are close to you.",
    )
    @ConfigEditorDropdown
    var flyingMode: FlyingMode = FlyingMode.NEVER

    enum class FlyingMode(private val displayName: String) {
        NEVER("Never"),
        WHEN_CLOSE("When Close"),
        ALWAYS("Always"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Close Distance",
        desc = "How close a flying capsule has to be to get hidden. Only used in the When Close mode.",
    )
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 6f, minStep = 0.5f)
    var closeDistance: Float = 2f

    @Expose
    @ConfigOption(
        name = "Hide Capsules on Ground",
        desc = "Hide Critter Capsules lying on the ground. Capsules you do not pick up return to your inventory " +
            "on their own after a few seconds. Masterful Critter Capsules are never hidden.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var hideOnGround: Boolean = false
}
