package at.hannibal2.skyhanni.config.features.pets.display.visual

import at.hannibal2.skyhanni.config.features.pets.display.ResettableScalableConfig
import at.hannibal2.skyhanni.config.storage.Resettable
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.ConfigOrder
import io.github.notenoughupdates.moulconfig.observer.Property

open class IconConfig(
    override val scalar: Float = 1.0f,
) : ResettableScalableConfig {
    companion object {
        private const val DEFAULT_ICON_SCALE = 2.5f
    }

    @Expose
    @ConfigOption(
        name = "Pet Icon",
        desc = "Show an icon of your current pet.\n" +
            "§cRequired for any options below to work§7.",
    )
    @ConfigEditorBoolean
    @ConfigOrder(10)
    val enabled: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Skin Animation", desc = "If your pet has an animated skin, the icon will also animate.")
    @ConfigEditorBoolean
    @ConfigOrder(20)
    val skinAnimation: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Skin Animation Speed",
        desc = "How fast animated pet skins should play.",
    )
    @ConfigEditorSlider(minValue = 0.0f, maxValue = 2.0f, minStep = 0.1f)
    @ConfigOrder(25)
    val skinAnimationSpeed: Property<Float> = Property.of(1.0f)

    @Expose
    @ConfigOption(
        name = "Icon Scale",
        desc = "How large the pet icon should be.",
    )
    @ConfigEditorSlider(minValue = 0.1f, maxValue = 3.0f, minStep = 0.1f)
    @ConfigOrder(30)
    val scale: Property<Float> = Property.of(DEFAULT_ICON_SCALE * scalar)

    @Expose
    @ConfigOption(name = "Icon Rotation/Spin", desc = "")
    @Accordion
    @ConfigOrder(40)
    val rotation: IconRotationConfig = IconRotationConfig()

    class IconRotationConfig : Resettable {
        @Expose
        @ConfigOption(name = "Static Rotation", desc = "Set a static rotation offset for the pet icon.")
        @Accordion
        val staticRotation = StaticRotationConfig()

        class StaticRotationConfig : Resettable {
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

            @ConfigOption(name = "Reset Rotations", desc = "Reset static rotations to the default value of 0.")
            @ConfigEditorButton(buttonText = "Reset")
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
            val note: String = ""

            @Expose
            @ConfigOption(name = "Rotation Speed (X)", desc = "How many degrees per second the pet icon should rotate on the X axis.")
            @ConfigEditorSlider(minValue = -725f, maxValue = 725f, minStep = 25f)
            val speedX: Property<Float> = Property.of(0f)

            @Expose
            @ConfigOption(name = "Rotation Speed (Y)", desc = "How many degrees per second the pet icon should rotate on the Y axis.")
            @ConfigEditorSlider(minValue = -725f, maxValue = 725f, minStep = 25f)
            val speedY: Property<Float> = Property.of(0f)

            @Expose
            @ConfigOption(name = "Rotation Speed (Z)", desc = "How many degrees per second the pet icon should rotate on the Z axis.")
            @ConfigEditorSlider(minValue = -725f, maxValue = 725f, minStep = 1f)
            val speedZ: Property<Float> = Property.of(0f)

            @ConfigOption(name = "Reset Rotation Speeds", desc = "Reset the rotation speeds to the default value of 0.")
            @ConfigEditorButton(buttonText = "Reset")
            val reset: Runnable = Runnable(::reset)
        }
    }

    @ConfigOption(name = "Reset Icon Settings", desc = "Reset the icon settings to the default values.")
    @ConfigEditorButton(buttonText = "Reset")
    @ConfigOrder(50)
    val reset: Runnable = Runnable(::reset)
}
