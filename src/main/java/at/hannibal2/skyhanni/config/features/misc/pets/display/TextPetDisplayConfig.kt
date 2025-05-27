package at.hannibal2.skyhanni.config.features.misc.pets.display

import at.hannibal2.skyhanni.utils.RenderUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

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
            TextElement.PET_NAME,
            TextElement.NEXT_LEVEL,
            TextElement.HELD_ITEM,
        )
    )

    // Todo: Change display names to examples instead of names
    enum class TextElement(private val displayName: String, val label: String) {
        PET_NAME("", "Pet Name"),
        NEXT_LEVEL("","Next Level"),
        OVERFLOW_XP("","Overflow XP"),
        TOTAL_XP("","Total XP"),
        HELD_ITEM("","Held Item"),
        ;

        override fun toString() = displayName
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
        name = "Next Level %",
        desc = "Show a percentage after your exp progress.\n" +
            "§eNext Level must be enabled above."
    )
    @ConfigEditorBoolean
    val nextLevelPercent: Property<Boolean> = Property.of(true)

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
    val textLocation: Property<TextLocationOption> = Property.of(TextLocationOption.RIGHT)

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
}
