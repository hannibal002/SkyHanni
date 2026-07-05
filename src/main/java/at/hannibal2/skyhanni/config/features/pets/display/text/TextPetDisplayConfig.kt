package at.hannibal2.skyhanni.config.features.pets.display.text

import at.hannibal2.skyhanni.utils.RenderUtils
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.annotations.ConfigOrder
import io.github.notenoughupdates.moulconfig.observer.Property

interface PetTextDisplaySettings {
    fun enabledTextsProperty(): Property<MutableList<TextPetDisplayConfig.TextElement>>
    fun textLabelsProperty(): Property<Boolean>
    fun nameLevelProperty(): Property<Boolean>
    fun nameSkinSymbolProperty(): Property<Boolean>
    fun nextLevelPercentProperty(): Property<Boolean>
    fun xpFormatProperty(): Property<TextPetDisplayConfig.NumberFormatEntry>
    fun textScaleProperty(): Property<Float>
    fun textLocationProperty(): Property<TextPetDisplayConfig.TextLocationOption>
    fun verticalAlignProperty(): Property<RenderUtils.VerticalAlignment>
    fun horizontalAlignProperty(): Property<RenderUtils.HorizontalAlignment>
}

class TextPetDisplayConfig {
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

    enum class NumberFormatEntry(
        private val displayName: String,
    ) {
        DEFAULT("Default"),
        FORMATTED("Formatted"),
        UNFORMATTED("Unformatted"),
        ;

        override fun toString() = displayName
    }

    enum class TextLocationOption(private val displayName: String) {
        TOP("Top"),
        BOTTOM("Bottom"),
        LEFT("Left"),
        RIGHT("Right"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Equipped Pet Text", desc = "")
    @Accordion
    @ConfigOrder(10)
    val equippedPet: EquippedPetTextConfig = EquippedPetTextConfig()

    class EquippedPetTextConfig : PetTextDisplaySettings {
        @Expose
        @ConfigOption(
            name = "Enabled Text",
            desc = "Show text relating to your pet in the GUI element.\n" +
                "§eItems that are gray are dependent on the items in red."
        )
        @ConfigEditorDraggableList
        @ConfigOrder(10)
        val enabledTexts: Property<MutableList<TextElement>> = Property.of(
            mutableListOf(
                TextElement.PET_NAME,
                TextElement.NEXT_LEVEL,
                TextElement.HELD_ITEM,
            )
        )

        @Expose
        @ConfigOption(
            name = "Text Labels",
            desc = "Show labels before each text line explaining what data it is."
        )
        @ConfigEditorBoolean
        @ConfigOrder(20)
        val textLabels: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(
            name = "Pet Level",
            desc = "Show pet level in the pet name text.\n" +
                "§ePet Name must be enabled above."
        )
        @ConfigEditorBoolean
        @ConfigOrder(30)
        val nameLevel: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(
            name = "Skin Symbol",
            desc = "Show a symbol for pet skin in the pet name text.\n" +
                "§ePet Name must be enabled above."
        )
        @ConfigEditorBoolean
        @ConfigOrder(40)
        val nameSkinSymbol: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(
            name = "Next Level %",
            desc = "Show a percentage after your exp progress.\n" +
                "§eNext Level must be enabled above."
        )
        @ConfigEditorBoolean
        @ConfigOrder(50)
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
        @ConfigOrder(60)
        val xpFormat: Property<NumberFormatEntry> = Property.of(NumberFormatEntry.DEFAULT)

        @Expose
        @ConfigOption(
            name = "Text Scale",
            desc = "How large equipped pet text should be."
        )
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 2.0f, minStep = 0.05f)
        @ConfigOrder(70)
        val textScale: Property<Float> = Property.of(1.0f)

        @Expose
        @ConfigOption(
            name = "Text Location",
            desc = "Where the text will be placed, relative to the Visual Elements above.\n" +
                "§eOnly has any effect if one or more Visual Elements are enabled."
        )
        @ConfigEditorDropdown
        @ConfigOrder(80)
        val textLocation: Property<TextLocationOption> = Property.of(TextLocationOption.RIGHT)

        @Expose
        @ConfigOption(
            name = "Center Target",
            desc = "What equipped pet text should center around."
        )
        @ConfigEditorDropdown
        @ConfigOrder(90)
        val centerTarget: Property<CenterTarget> = Property.of(CenterTarget.EQUIPPED_PET_VISUALS)

        enum class CenterTarget(private val displayName: String) {
            EQUIPPED_PET_VISUALS("Equipped Pet Visuals"),
            ALL_PET_VISUALS("All Pet Visuals"),
            ;

            override fun toString() = displayName
        }

        @Expose
        @ConfigOption(
            name = "Vertical Alignment",
            desc = "How text elements will align vertically.",
        )
        @ConfigEditorDropdown
        @ConfigOrder(100)
        val verticalAlign: Property<RenderUtils.VerticalAlignment> = Property.of(RenderUtils.VerticalAlignment.CENTER)

        @Expose
        @ConfigOption(
            name = "Horizontal Alignment",
            desc = "How text elements will align horizontally.",
        )
        @ConfigEditorDropdown
        @ConfigOrder(110)
        val horizontalAlign: Property<RenderUtils.HorizontalAlignment> = Property.of(RenderUtils.HorizontalAlignment.LEFT)

        override fun enabledTextsProperty() = enabledTexts
        override fun textLabelsProperty() = textLabels
        override fun nameLevelProperty() = nameLevel
        override fun nameSkinSymbolProperty() = nameSkinSymbol
        override fun nextLevelPercentProperty() = nextLevelPercent
        override fun xpFormatProperty() = xpFormat
        override fun textScaleProperty() = textScale
        override fun textLocationProperty() = textLocation
        override fun verticalAlignProperty() = verticalAlign
        override fun horizontalAlignProperty() = horizontalAlign
    }

    @Expose
    @ConfigOption(name = "Exp-Share Pet Text", desc = "")
    @Accordion
    @ConfigOrder(20)
    val expSharePets: ExpSharePetTextConfig = ExpSharePetTextConfig()

    class ExpSharePetTextConfig : PetTextDisplaySettings {
        @Expose
        @ConfigOption(
            name = "Enabled",
            desc = "Show text next to your Exp-Share pet icons."
        )
        @ConfigEditorBoolean
        @ConfigOrder(10)
        val enabled: Property<Boolean> = Property.of(false)

        @Expose
        @ConfigOption(
            name = "Text Mode",
            desc = "Where Exp-Share pet text should be displayed."
        )
        @ConfigEditorDropdown
        @ConfigOrder(20)
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
        @ConfigOrder(30)
        val bundledLocation: Property<BundledTextLocation> = Property.of(BundledTextLocation.BELOW)

        @Expose
        @ConfigOption(
            name = "Bundled Spacing",
            desc = "Space between bundled Exp-Share pet text and the main pet text."
        )
        @ConfigEditorSlider(minValue = 0f, maxValue = 20f, minStep = 1f)
        @ConfigOrder(40)
        val bundledSpacing: Property<Int> = Property.of(9)

        @Expose
        @ConfigOption(
            name = "Text Scale",
            desc = "How large Exp-Share pet text should be."
        )
        @ConfigEditorSlider(minValue = 0.5f, maxValue = 2.0f, minStep = 0.05f)
        @ConfigOrder(50)
        val textScale: Property<Float> = Property.of(1.0f)

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
        @ConfigOrder(60)
        val enabledTexts: Property<MutableList<TextElement>> = Property.of(
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
        @ConfigOrder(70)
        val textLabels: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(
            name = "Pet Level",
            desc = "Show pet level in the pet name text.\n" +
                "§ePet Name must be enabled above."
        )
        @ConfigEditorBoolean
        @ConfigOrder(80)
        val nameLevel: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(
            name = "Skin Symbol",
            desc = "Show a symbol for pet skin in the pet name text.\n" +
                "§ePet Name must be enabled above."
        )
        @ConfigEditorBoolean
        @ConfigOrder(90)
        val nameSkinSymbol: Property<Boolean> = Property.of(true)

        @Expose
        @ConfigOption(
            name = "Next Level %",
            desc = "Show a percentage after your exp progress.\n" +
                "§eNext Level must be enabled above."
        )
        @ConfigEditorBoolean
        @ConfigOrder(100)
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
        @ConfigOrder(110)
        val xpFormat: Property<NumberFormatEntry> = Property.of(NumberFormatEntry.DEFAULT)

        @Expose
        @ConfigOption(
            name = "Text Location",
            desc = "Where attached text will be placed, relative to each Exp-Share pet icon."
        )
        @ConfigEditorDropdown
        @ConfigOrder(120)
        val textLocation: Property<TextLocationOption> = Property.of(TextLocationOption.RIGHT)

        @Expose
        @ConfigOption(
            name = "Vertical Alignment",
            desc = "How text elements will align vertically.",
        )
        @ConfigEditorDropdown
        @ConfigOrder(130)
        val verticalAlign: Property<RenderUtils.VerticalAlignment> = Property.of(RenderUtils.VerticalAlignment.CENTER)

        @Expose
        @ConfigOption(
            name = "Horizontal Alignment",
            desc = "How text elements will align horizontally.",
        )
        @ConfigEditorDropdown
        @ConfigOrder(140)
        val horizontalAlign: Property<RenderUtils.HorizontalAlignment> = Property.of(RenderUtils.HorizontalAlignment.LEFT)

        override fun enabledTextsProperty() = enabledTexts
        override fun textLabelsProperty() = textLabels
        override fun nameLevelProperty() = nameLevel
        override fun nameSkinSymbolProperty() = nameSkinSymbol
        override fun nextLevelPercentProperty() = nextLevelPercent
        override fun xpFormatProperty() = xpFormat
        override fun textScaleProperty() = textScale
        override fun textLocationProperty() = textLocation
        override fun verticalAlignProperty() = verticalAlign
        override fun horizontalAlignProperty() = horizontalAlign
    }
}
