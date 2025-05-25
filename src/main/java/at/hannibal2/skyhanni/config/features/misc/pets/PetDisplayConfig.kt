package at.hannibal2.skyhanni.config.features.misc.pets

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.DependentDisplayManager
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.features.misc.pets.TElement
import at.hannibal2.skyhanni.features.misc.pets.TLO
import at.hannibal2.skyhanni.features.misc.pets.VElement
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import kotlin.collections.emptySet

class PetDisplayConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a GUI element for the currently active pet.")
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigLink(owner = PetDisplayConfig::class, field = "enabled")
    val position: Position = Position(200, 200)

    @Expose
    @ConfigOption(name = "Visual Elements", desc = "")
    @Accordion
    val visual: VisualPetDisplayConfig = VisualPetDisplayConfig()

    class VisualPetDisplayConfig {
        @Expose
        @ConfigOption(
            name = "Enabled Visuals",
            desc = "Show visuals relating to your pet in the GUI element.\n" +
                "§eItems that are gray are dependent on the items in red."
        )
        @ConfigEditorDraggableList
        val enabledVisuals: Property<MutableList<VisualElement>> = Property.of(
            mutableListOf(
                VElement.PET_ICON,
                VElement.RARITY_BACKGROUND,
                VElement.XP_RING,
            )
        )

        enum class VisualElement(
            val displayName: String,
            val dependentOn: Collection<VisualElement> = emptySet()
        ) {
            PET_ICON("Pet Icon"),
            RARITY_BACKGROUND(
                displayName = "Rarity Background",
                dependentOn = setOf(PET_ICON),
            ),
            XP_RING(
                displayName = "Xp Ring",
                dependentOn = setOf(PET_ICON, RARITY_BACKGROUND),
            ),
            SEPARATOR_RING(
                displayName = "Separator Ring",
                dependentOn = setOf(RARITY_BACKGROUND, XP_RING),
            )
            ;

            override fun toString() = manager.displayName(this)
        }

        @Expose
        @ConfigOption(
            name = "Icon Scale",
            desc = "How large the icon should be - Default is 1.7\n" +
                "§ePet Icon must be enabled above."
        )
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 2.5f, minStep = 0.1f)
        val iconScale: Property<Double> = Property.of(1.7)

        @Expose
        @ConfigOption(
            name = "Skin Animation",
            desc = "If your pet has an animated skin, display the animated skin for the icon.\n" +
                "§ePet Icon must be enabled above."
        )
        @ConfigEditorBoolean
        val skinAnimation: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(
            name = "Icon Spin",
            desc = "Spin the pet icon in place.\n" +
                "§ePet Icon must be enabled above."
        )
        @ConfigEditorDropdown
        val spinDirection: Property<SpinDirection> = Property.of(SpinDirection.NONE)

        enum class SpinDirection(private val displayName: String) {
            NONE("No Spinning"),
            CLOCKWISE("Clockwise"),
            COUNTER_CLOCKWISE("Counter-Clockwise"),
            ;

            override fun toString() = displayName
        }

        @Expose
        @ConfigOption(
            name = "Spin Speed",
            desc = "How long in seconds it should take for one spin to complete.\n" +
                "§ePet Icon and §eIcon Spin must be enabled above."
        )
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 10f, minStep = 0.5f)
        val spinFrequency: Property<Float> = Property.of(2.0f)

        @SkyHanniModule
        companion object {
            private val manager = DependentDisplayManager(
                VisualElement.entries,
                { SkyHanniMod.feature.misc.pets.display.visual.enabledVisuals },
                VisualElement::dependentOn,
                VisualElement::displayName
            )

            @HandleEvent
            fun onConfigLoad() = manager.onConfigLoad()
        }
    }

    @Expose
    @ConfigOption(name = "Text Elements", desc = "")
    @Accordion
    val text: TextPetDisplayConfig = TextPetDisplayConfig()

    class TextPetDisplayConfig {
        @Expose
        @ConfigOption(
            name = "Enabled Text",
            desc = "Show text relating to your pet in the GUI element.\n" +
                "§eItems that are gray are dependent on the items in red."
        )
        @ConfigEditorDraggableList
        val enabledTexts: Property<MutableList<TextElement>> = Property.of(
            mutableListOf(
                TElement.PET_NAME,
                TElement.PET_LEVEL,
                TElement.SKIN_SYMBOL,
                TElement.NEXT_LEVEL,
                TElement.HELD_ITEM,
            )
        )

        enum class TextElement(
            val displayName: String,
            val dependentOn: Collection<TextElement> = emptySet()
        ) {
            PET_NAME("Pet Name"),
            PET_LEVEL(
                displayName = "Pet Level",
                dependentOn = setOf(PET_NAME),
            ),
            SKIN_SYMBOL(
                displayName = "Skin Symbol",
                dependentOn = setOf(PET_NAME),
            ),

            NEXT_LEVEL("Next Level"),
            NEXT_LEVEL_PERCENTAGE(
                displayName = "Next Level Percentage",
                dependentOn = setOf(NEXT_LEVEL),
            ),

            OVERFLOW_XP("Overflow XP"),
            TOTAL_XP("Total XP"),
            HELD_ITEM("Held Item"),
            ;

            override fun toString() = manager.displayName(this)
        }

        @Expose
        @ConfigOption(
            name = "Text Labels",
            desc = "Show labels before each text line explaining what data it is."
        )
        @ConfigEditorBoolean
        val textLabels: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(
            name = "XP Format",
            desc = "Either show default, formatted, or unformatted numbers.\n" +
                "§eDefault: §72,240/2.2k\n" +
                "§eFormatted: §72.2k/2.2k\n" +
                "§eUnformatted: §72,240/2,200"
        )
        @ConfigEditorDropdown
        val xpFormat: Property<NumberFormatEntry> = Property.of(NumberFormatEntry.DEFAULT)

        enum class NumberFormatEntry(
            private val displayName: String,
        ) {
            DEFAULT("Default"),
            FORMATTED("Formatted"),
            UNFORMATTED("Unformatted"),
            ;

            override fun toString() = displayName
        }

        @Expose
        @ConfigOption(
            name = "Text Location",
            desc = "Where the text will be placed, relative to the Visual Elements above.\n" +
                "§eOnly has any effect if one or more Visual Elements are enabled."
        )
        @ConfigEditorDropdown
        val textLocation: Property<TextLocationOption> = Property.of(TLO.RIGHT)

        enum class TextLocationOption(private val displayName: String) {
            TOP("Top"),
            BOTTOM("Bottom"),
            LEFT("Left"),
            RIGHT("Right"),
            ;

            override fun toString() = displayName
        }

        @Expose
        @ConfigOption(
            name = "Vertical Alignment",
            desc = "How text elements will align vertically.",
        )
        @ConfigEditorDropdown
        val verticalAlign: Property<RenderUtils.VerticalAlignment> = Property.of(RenderUtils.VerticalAlignment.CENTER)

        @Expose
        @ConfigOption(
            name = "Horizontal Alignment",
            desc = "How text elements will align horizontally.",
        )
        @ConfigEditorDropdown
        val horizontalAlign: Property<RenderUtils.HorizontalAlignment> = Property.of(RenderUtils.HorizontalAlignment.LEFT)

        @SkyHanniModule
        companion object {
            private val manager = DependentDisplayManager(
                TextElement.entries,
                { SkyHanniMod.feature.misc.pets.display.text.enabledTexts },
                TextElement::dependentOn,
                TextElement::displayName
            )

            @HandleEvent
            fun onConfigLoad() = manager.onConfigLoad()
        }
    }
}
