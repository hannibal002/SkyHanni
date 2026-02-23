package at.hannibal2.skyhanni.config.features.pets.display.visual

import at.hannibal2.skyhanni.config.storage.Resettable
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

open class IconConfig {
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
    val skinAnimation: SkinAnimationConfig = SkinAnimationConfig()

    class SkinAnimationConfig {
        @Expose
        @ConfigOption(name = "Enabled", desc = "If your pet has an animated skin, the icon will also animate.")
        @ConfigEditorBoolean
        val enabled: Property<Boolean> = Property.of(true)

        // Not used right now, but should be in future
        /*
        @Expose
        @ConfigOption(
            name = "Animation Speed",
            desc = "How fast the skin animation should play, in ticks per frame"
        )
        @ConfigEditorSlider(minValue = 1f, maxValue = 20f, minStep = 1f)
        val animationSpeed: Property<Float> = Property.of(5f)
         */
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

        class StaticRotationConfig : Resettable {
            @Expose
            @ConfigOption(name = "X Rotation", desc = "Rotate the pet icon on the X axis, by this many degrees.")
            @ConfigEditorSlider(minValue = 0f, maxValue = 360f, minStep = 5f)
            val xRotation: Property<Double> = Property.of(0.0)

            @Expose
            @ConfigOption(name = "Y Rotation", desc = "Rotate the pet icon on the Y axis, by this many degrees.")
            @ConfigEditorSlider(minValue = 0f, maxValue = 360f, minStep = 5f)
            val yRotation: Property<Double> = Property.of(0.0)

            @Expose
            @ConfigOption(name = "Z Rotation", desc = "Rotate the pet icon on the Z axis, by this many degrees.")
            @ConfigEditorSlider(minValue = 0f, maxValue = 360f, minStep = 5f)
            val zRotation: Property<Double> = Property.of(0.0)

            @ConfigOption(name = "Reset", desc = "Reset static rotations to the default value of 0.")
            @ConfigEditorButton(buttonText = "Reset Static Rotation")
            val reset: Runnable = Runnable(::reset)
        }

        @Expose
        @ConfigOption(name = "Spin Rotation", desc = "Continuously rotate the pet icon at a set speed.")
        @Accordion
        val spinRotation = SpinRotationConfig()

        class SpinRotationConfig : Resettable {
            @ConfigOption(
                name = "Note",
                desc = "Positive values will rotate clockwise, negative values will rotate counter-clockwise."
            )
            @ConfigEditorInfoText
            val note: Unit = Unit

            @Expose
            @ConfigOption(name = "Rotation Speed (X)", desc = "How many degrees per second the pet icon should rotate on the X axis.")
            @ConfigEditorSlider(minValue = -725f, maxValue = 725f, minStep = 25f)
            val speedX: Property<Double> = Property.of(0.0)

            @Expose
            @ConfigOption(name = "Rotation Speed (Y)", desc = "How many degrees per second the pet icon should rotate on the Y axis.")
            @ConfigEditorSlider(minValue = -725f, maxValue = 725f, minStep = 25f)
            val speedY: Property<Double> = Property.of(0.0)

            @Expose
            @ConfigOption(name = "Rotation Speed (Z)", desc = "How many degrees per second the pet icon should rotate on the Z axis.")
            @ConfigEditorSlider(minValue = -725f, maxValue = 725f, minStep = 1f)
            val speedZ: Property<Double> = Property.of(0.0)

            @ConfigOption(name = "Reset", desc = "Reset the rotation speeds to the default value of 0.")
            @ConfigEditorButton(buttonText = "Reset Rotation Speeds")
            val reset: Runnable = Runnable(::reset)
        }
    }
}
