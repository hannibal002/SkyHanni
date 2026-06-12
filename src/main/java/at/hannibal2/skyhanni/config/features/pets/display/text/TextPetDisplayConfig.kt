package at.hannibal2.skyhanni.config.features.pets.display.text

import at.hannibal2.skyhanni.utils.RenderUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property

interface PetTextDisplaySettings {
    val enabledTexts: Property<MutableList<TextPetDisplayConfig.TextElement>>
    val textLabels: Property<Boolean>
    val nameLevel: Property<Boolean>
    val nameSkinSymbol: Property<Boolean>
    val nextLevelPercent: Property<Boolean>
    val xpFormat: Property<TextPetDisplayConfig.NumberFormatEntry>
    val textLocation: Property<TextPetDisplayConfig.TextLocationOption>
    val verticalAlign: Property<RenderUtils.VerticalAlignment>
    val horizontalAlign: Property<RenderUtils.HorizontalAlignment>
}

class TextPetDisplayConfig : PetTextDisplaySettings {
    @Expose
    @ConfigOption(
        name = "Enabled Text",
        desc = "Show text relating to your pet in the GUI element.\n" +
            "§eItems that are gray are dependent on the items in red."
    )
    @ConfigEditorDraggableList
    override val enabledTexts: Property<MutableList<TextElement>> = Property.of(
        mutableListOf(
            TextElement.PET_NAME,
            TextElement.NEXT_LEVEL,
            TextElement.HELD_ITEM,
        )
    )

    enum class TextElement(private val displayName: String, private val label: String = "") {
        PET_NAME("§7[Lvl 100] §6Mithril Golem §5✦"),
        NEXT_LEVEL("§b2,000§9/§b4,000 §7- §e50%", "Next Level"),
        OVERFLOW_XP("§7+§b2,000,000", "Overflow XP"),
        TOTAL_XP("§b1,250,000", "Total XP"),
        HELD_ITEM("§9Dwarf Turtle Shelmet", "Held Item"),
        ;

        fun getFormattedLabel() = label.takeIf { it.isNotEmpty() }?.let { "§e$it§7: " }.orEmpty()
        override fun toString() = getFormattedLabel() + displayName
    }

    @Expose
    @ConfigOption(
        name = "Text Labels",
        desc = "Show labels before each text line explaining what data it is."
    )
    @ConfigEditorBoolean
    override val textLabels: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Pet Level",
        desc = "Show pet level in the pet name text.\n" +
            "§ePet Name must be enabled above."
    )
    @ConfigEditorBoolean
    override val nameLevel: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Skin Symbol",
        desc = "Show a symbol for pet skin in the pet name text.\n" +
            "§ePet Name must be enabled above."
    )
    @ConfigEditorBoolean
    override val nameSkinSymbol: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "Next Level %",
        desc = "Show a percentage after your exp progress.\n" +
            "§eNext Level must be enabled above."
    )
    @ConfigEditorBoolean
    override val nextLevelPercent: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(
        name = "XP Format",
        desc = "Either show default, formatted, or unformatted numbers.\n" +
            "§eDefault: §72,240/2.2k\n" +
            "§eFormatted: §72.2k/2.2k\n" +
            "§eUnformatted: §72,240/2,200"
    )
    @ConfigEditorDropdown
    override val xpFormat: Property<NumberFormatEntry> = Property.of(NumberFormatEntry.DEFAULT)

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
    override val textLocation: Property<TextLocationOption> = Property.of(TextLocationOption.RIGHT)

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
    override val verticalAlign: Property<RenderUtils.VerticalAlignment> = Property.of(RenderUtils.VerticalAlignment.CENTER)

    @Expose
    @ConfigOption(
        name = "Horizontal Alignment",
        desc = "How text elements will align horizontally.",
    )
    @ConfigEditorDropdown
    override val horizontalAlign: Property<RenderUtils.HorizontalAlignment> = Property.of(RenderUtils.HorizontalAlignment.LEFT)

    @Expose
    @ConfigOption(name = "Exp-Share Pet Text", desc = "")
    @Accordion
    val expSharePets: ExpSharePetTextConfig = ExpSharePetTextConfig()

    class ExpSharePetTextConfig : PetTextDisplaySettings {
        @Expose
        @ConfigOption(
            name = "Enabled",
            desc = "Show text next to your Exp-Share pet icons."
        )
        @ConfigEditorBoolean
        val enabled: Property<Boolean> = Property.of(false)

        @Expose
        @ConfigOption(
            name = "Text Mode",
            desc = "Where Exp-Share pet text should be displayed."
        )
        @ConfigEditorDropdown
        val textMode: Property<TextMode> = Property.of(TextMode.BUNDLED_WITH_MAIN)

        enum class TextMode(private val displayName: String) {
            BUNDLED_WITH_MAIN("Bundled"),
            ATTACHED_TO_ICONS("Attached"),
            ;

            override fun toString() = displayName
        }

        @Expose
        @ConfigOption(
            name = "Bundled Location",
            desc = "Where bundled Exp-Share pet text should be placed around the main pet text."
        )
        @ConfigEditorDropdown
        val bundledLocation: Property<BundledTextLocation> = Property.of(BundledTextLocation.BELOW)

        @Expose
        @ConfigOption(
            name = "Bundled Spacing",
            desc = "Space between bundled Exp-Share pet text and the main pet text."
        )
        @ConfigEditorSlider(minValue = 0f, maxValue = 20f, minStep = 1f)
        val bundledSpacing: Property<Int> = Property.of(9)

        enum class BundledTextLocation(private val displayName: String) {
            ABOVE("Above Main Text"),
            BELOW("Below Main Text"),
            SPLIT("Split Around Main Text"),
            ;

            override fun toString() = displayName
        }

        @Expose
        @ConfigOption(
            name = "Enabled Text",
            desc = "Show text relating to your Exp-Share pets.\n" +
                "§eItems that are gray are dependent on the items in red."
        )
        @ConfigEditorDraggableList
        override val enabledTexts: Property<MutableList<TextElement>> = Property.of(
            mutableListOf(
                TextElement.PET_NAME,
                TextElement.NEXT_LEVEL,
            )
        )

        @Expose
        @ConfigOption(
            name = "Text Labels",
            desc = "Show labels before each text line explaining what data it is."
        )
        @ConfigEditorBoolean
        override val textLabels: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(
            name = "Pet Level",
            desc = "Show pet level in the pet name text.\n" +
                "§ePet Name must be enabled above."
        )
        @ConfigEditorBoolean
        override val nameLevel: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(
            name = "Skin Symbol",
            desc = "Show a symbol for pet skin in the pet name text.\n" +
                "§ePet Name must be enabled above."
        )
        @ConfigEditorBoolean
        override val nameSkinSymbol: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(
            name = "Next Level %",
            desc = "Show a percentage after your exp progress.\n" +
                "§eNext Level must be enabled above."
        )
        @ConfigEditorBoolean
        override val nextLevelPercent: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(
            name = "XP Format",
            desc = "Either show default, formatted, or unformatted numbers.\n" +
                "§eDefault: §72,240/2.2k\n" +
                "§eFormatted: §72.2k/2.2k\n" +
                "§eUnformatted: §72,240/2,200"
        )
        @ConfigEditorDropdown
        override val xpFormat: Property<NumberFormatEntry> = Property.of(NumberFormatEntry.DEFAULT)

        @Expose
        @ConfigOption(
            name = "Text Location",
            desc = "Where attached text will be placed, relative to each Exp-Share pet icon."
        )
        @ConfigEditorDropdown
        override val textLocation: Property<TextLocationOption> = Property.of(TextLocationOption.RIGHT)

        @Expose
        @ConfigOption(
            name = "Vertical Alignment",
            desc = "How text elements will align vertically.",
        )
        @ConfigEditorDropdown
        override val verticalAlign: Property<RenderUtils.VerticalAlignment> = Property.of(RenderUtils.VerticalAlignment.CENTER)

        @Expose
        @ConfigOption(
            name = "Horizontal Alignment",
            desc = "How text elements will align horizontally.",
        )
        @ConfigEditorDropdown
        override val horizontalAlign: Property<RenderUtils.HorizontalAlignment> = Property.of(RenderUtils.HorizontalAlignment.LEFT)
    }
}
