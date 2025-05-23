package at.hannibal2.skyhanni.config.features.misc.pets

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.DependentDisplayManager
import at.hannibal2.skyhanni.config.core.config.Position
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

private typealias TLO = PetDisplayConfig.TextPetDisplayConfig.TextLocationOption

class PetDisplayConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a GUI element for the currently active pet.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigLink(owner = PetDisplayConfig::class, field = "enabled")
    val position: Position = Position(-330, -15)

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
        val enabledVisuals: Property<MutableList<VisualElement>> = Property.of(mutableListOf())

        enum class VisualElement(
            val displayName: String,
            val dependentOn: Collection<VisualElement> = emptySet()
        ) {
            ITEM_STACK("Item Stack"),
            RARITY_BACKGROUND(
                displayName = "Rarity Background",
                dependentOn = setOf(ITEM_STACK),
            ),
            XP_RING(
                displayName = "Xp Ring",
                dependentOn = setOf(ITEM_STACK, RARITY_BACKGROUND),
            ),
            PET_HELD_ITEM(
                displayName = "Pet Held Item",
                dependentOn = setOf(ITEM_STACK),
            ),
            ;

            override fun toString() = manager.displayName(this)
        }

        @Expose
        @ConfigOption(
            name = "Skin Animation",
            desc = "If your pet has an animated skin, display the animated skin for the item.\n" +
                "§eItem Stack must be enabled above."
        )
        @ConfigEditorBoolean
        var skinAnimation: Boolean = true

        @Expose
        @ConfigOption(
            name = "Item Spin",
            desc = "Spin the active pet item in place.\n" +
                "§eItem Stack must be enabled above."
        )
        @ConfigEditorDropdown
        var spinDirection: Property<SpinDirection> = Property.of(SpinDirection.NONE)

        enum class SpinDirection(private val displayName: String) {
            NONE("No Spinning"),
            CLOCKWISE("Clockwise"),
            COUNTER_CLOCKWISE("Counter-Clockwise"),
            ;

            override fun toString() = displayName
        }

        @Expose
        @ConfigOption(
            name = "Held Item Scale",
            desc = "How large the pet's held item should be. Default: 0.7\n" +
                "§ePet Item must be enabled above."
        )
        @ConfigEditorSlider(minValue = 0.1f, maxValue = 2.0f, minStep = 0.05f)
        var petItemScale: Property<Float> = Property.of(0.7f)

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
        val enabledTexts: Property<MutableList<TextElement>> = Property.of(mutableListOf())

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
            NEXT_LEVEL_PROGRESS("Next Level Progress"),
            TOTAL_XP("Total XP"),
            HELD_ITEM("Held Item"),
            ;

            override fun toString() = manager.displayName(this)
        }

        @Expose
        @ConfigOption(
            name = "XP Format",
            desc = "Either show default, formatted, or unformatted numbers.\n" +
                "§eDefault: §72,240/2.2k\n" +
                "§eFormatted: §72.2k/2.2k\n" +
                "§eUnformatted: §72,240/2,200"
        )
        @ConfigEditorDropdown
        var xpFormat: NumberFormatEntry = NumberFormatEntry.DEFAULT

        enum class NumberFormatEntry(
            private val displayName: String,
        ) {
            DEFAULT("Default"),
            FORMATTED("Formatted"),
            UNFORMATTED("UnFormatted"),
            ;

            override fun toString() = displayName
        }

        @Expose
        @ConfigOption(
            name = "Text Location",
            desc = "Where the text will be placed, relative to the Visual Elements above.\n" +
                "§eOnly has any effect if one or more Visual Elements are enabled."
        )
        var textLocation: Property<TextLocationOption> = Property.of(TLO.RIGHT)

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
        var verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.CENTER

        @Expose
        @ConfigOption(
            name = "Horizontal Alignment",
            desc = "How text elements will align horizontally.",
        )
        @ConfigEditorDropdown
        var horizontalAlignment: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT

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

    @Expose
    @ConfigOption(
        name = "Transparent Second Pet",
        desc = "Keep the last equipped pet in the background at this transparency. Set to 0 to disable." +
            "Second display will mirror the above configuration."
    )
    @ConfigEditorSlider(minValue = 0f, maxValue = 40f, minStep = 1f)
    var secondPetTransparency: Property<Int> = Property.of(0)
}
