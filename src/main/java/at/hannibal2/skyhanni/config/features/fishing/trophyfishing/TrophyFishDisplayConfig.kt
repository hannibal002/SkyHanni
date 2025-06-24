package at.hannibal2.skyhanni.config.features.fishing.trophyfishing

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import org.lwjgl.input.Keyboard

class TrophyFishDisplayConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a display of all trophy fishes ever caught.")
    @ConfigEditorBoolean
    @FeatureToggle
    val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "When Show", desc = "Change when the trophy fish display should be visible in Crimson Isle.")
    @ConfigEditorDropdown
    val whenToShow: Property<WhenToShow> = Property.of(WhenToShow.ALWAYS)

    enum class WhenToShow(private val displayName: String) {
        ALWAYS("Always"),
        ONLY_IN_INVENTORY("In inventory"),
        ONLY_WITH_ROD_IN_HAND("Rod in hand"),
        ONLY_WITH_KEYBIND("On keybind"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Keybind", desc = "")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    var keybind: Int = Keyboard.KEY_NONE

    @Expose
    @ConfigOption(name = "Hunter Armor", desc = "Only show when wearing a full Hunter Armor or Ember Armor.")
    @ConfigEditorBoolean
    val requireArmor: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Highlight New", desc = "Highlight new trophies green for couple seconds.")
    @ConfigEditorBoolean
    val highlightNew: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Extra space", desc = "Space between each line of text.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 10f, minStep = 1f)
    val extraSpace: Property<Int> = Property.of(1)

    @Expose
    @ConfigOption(name = "Sorted By", desc = "Sorting type of items in sack.")
    @ConfigEditorDropdown
    val sortingType: Property<TrophySorting> = Property.of(TrophySorting.ITEM_RARITY)

    enum class TrophySorting(private val displayName: String) {
        ITEM_RARITY("Item Rarity"),
        TOTAL_AMOUNT("Total Amount"),
        BRONZE_AMOUNT("Bronze Amount"),
        SILVER_AMOUNT("Silver Amount"),
        GOLD_AMOUNT("Gold Amount"),
        DIAMOND_AMOUNT("Diamond Amount"),
        HIGHEST_RARITY("Highest Rarity"),
        NAME("Name Alphabetical"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Reverse Order", desc = "Reverse the sorting order.")
    @ConfigEditorBoolean
    val reverseOrder: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Text Order", desc = "Drag text to change the line format.")
    @ConfigEditorDraggableList
    val textOrder: Property<MutableList<TextPart>> = Property.of(
        mutableListOf(
            TextPart.NAME,
            TextPart.ICON,
            TextPart.TOTAL,
            TextPart.BRONZE,
            TextPart.SILVER,
            TextPart.GOLD,
            TextPart.DIAMOND,
        ),
    )

    enum class TextPart(private val displayName: String) {
        ICON("Item Icon"),
        NAME("Item Name"),
        BRONZE("Amount Bronze"),
        SILVER("Amount Silver"),
        GOLD("Amount Gold"),
        DIAMOND("Amount Diamond"),
        TOTAL("Amount Total"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Show ✖", desc = "Instead of the number 0, show §c✖ §7if not found.")
    @ConfigEditorBoolean
    val showCross: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Show ✔", desc = "Instead of the exact numbers, show §e§l✔ §7if found.")
    @ConfigEditorBoolean
    val showCheckmark: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Only Show Missing", desc = "Only show Trophy Fish that are still missing at this rarity.")
    @ConfigEditorDropdown
    val onlyShowMissing: Property<HideCaught> = Property.of(HideCaught.NONE)

    enum class HideCaught(private val displayName: String) {
        NONE("Show All"),
        BRONZE("Bronze"),
        SILVER("Silver"),
        GOLD("Gold"),
        DIAMOND("Diamond"),
        ;

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(
        name = "Show If Caught Higher Tier",
        desc = "Show Trophy Fish missing at the chosen tier even if a higher tier has already been caught.",
    )
    @ConfigEditorBoolean
    val showCaughtHigher: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigLink(owner = TrophyFishDisplayConfig::class, field = "enabled")
    val position: Position = Position(144, 139)
}
