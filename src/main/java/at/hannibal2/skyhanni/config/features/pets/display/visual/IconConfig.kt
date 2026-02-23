package at.hannibal2.skyhanni.config.features.pets.display.visual

import at.hannibal2.skyhanni.utils.renderables.animated.OrbitDirection
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

class IconConfig {
    @Expose
    @ConfigOption(
        name = "Pet Icon",
        desc = "Show an icon of your current pet.\n" +
            "§cRequired for any options below to work§7.",
    )
    @ConfigEditorBoolean
    val enabled: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Skin Animation", desc = "If your pet has an animated skin, the icon will also animate.")
    @ConfigEditorBoolean
    val skinAnimation: Property<Boolean> = Property.of(true)

    class SkinAnimationConfig {
        @Expose
        @ConfigOption(name = "Enabled", desc = "If your pet has an animated skin, the icon will also animate.")
        @ConfigEditorBoolean
        val enabled: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(
            name = "Animation Speed",
            desc = "How fast the skin animation should play, in ticks per frame"
        )
    }

    @Expose
    @ConfigOption(
        name = "Icon Scale",
        desc = "How large the pet icon should be.",
    )
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 2.0f, minStep = 0.1f)
    val scale: Property<Double> = Property.of(1.0)

    @Expose
    @ConfigOption(name = "Icon Rotation/Spin", desc = "")
    @Accordion
    val rotation: IconRotationConfig = IconRotationConfig()

    open class IconRotationConfig {

        @Expose
        @ConfigOption(name = "Static Rotation", desc = "Set a static rotation offset for the pet icon.")
        @Accordion
        val staticRotation = StaticRotationConfig()

        class StaticRotationConfig {
            @Expose
            @ConfigOption(name = "X Rotation", desc = "Rotate the pet icon on the X axis, by this many degrees.")
            @ConfigEditorSlider(minValue = 0f, maxValue = 360f, minStep = 5f)
            val xRotation: Property<Float> = Property.of(0f)

            @Expose
            @ConfigOption(name = "Y Rotation", desc = "Rotate the pet icon on the Y axis, by this many degrees.")
            @ConfigEditorSlider(minValue = 0f, maxValue = 360f, minStep = 5f)
            val yRotation: Property<Float> = Property.of(0f)

            @Expose
            @ConfigOption(name = "Z Rotation", desc = "Rotate the pet icon on the Z axis, by this many degrees.")
            @ConfigEditorSlider(minValue = 0f, maxValue = 360f, minStep = 5f)
            val zRotation: Property<Float> = Property.of(0f)
        }

        @Expose
        @ConfigOption(name = "Enabled", desc = "Spin the pet icon in place.")
        @ConfigEditorDropdown
        val direction: Property<OrbitDirection> = Property.of(OrbitDirection.NONE)

        @Expose
        @ConfigOption(name = "Spin Speed", desc = "How long in milliseconds it should take for one spin to complete.")
        @ConfigEditorSlider(minValue = 250f, maxValue = 10000f, minStep = 250f)
        val frequency: Property<Float> = Property.of(2000f)
    }
}
