package at.hannibal2.skyhanni.config.features.fishing.trophyfishing

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.fishing.trophy.TrophyCollectionDisplayConfig
import at.hannibal2.skyhanni.config.features.fishing.trophy.TrophyCollectionDisplayConfig.HideCaught
import at.hannibal2.skyhanni.config.features.fishing.trophy.TrophyCollectionDisplayConfig.TextPart
import at.hannibal2.skyhanni.config.features.fishing.trophy.TrophyCollectionDisplayConfig.TrophySorting
import at.hannibal2.skyhanni.config.features.fishing.trophy.TrophyCollectionDisplayConfig.WhenToShow
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import io.github.notenoughupdates.moulconfig.observer.Property
import org.lwjgl.glfw.GLFW

class TrophyFishDisplayConfig : TrophyCollectionDisplayConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show a display of all trophy fishes ever caught.")
    @ConfigEditorBoolean
    @FeatureToggle
    override val enabled: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "When Show", desc = "Change when the trophy fish display should be visible in Crimson Isle.")
    @ConfigEditorDropdown
    override val whenToShow: Property<WhenToShow> = Property.of(WhenToShow.ALWAYS)

    @Expose
    @ConfigOption(name = "Keybind", desc = "")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    override var keybind: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(
        name = "Trophy Fishing Gear",
        desc = "Only show when: wearing 2+ Hunter Armor pieces, full Ember Armor, trophy line on your rod, using hot bait, " +
            "or you're too low level to fish for sea creatures."
    )
    @ConfigEditorBoolean
    override val requireArmor: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Highlight New", desc = "Highlight new trophies green for couple seconds.")
    @ConfigEditorBoolean
    override val highlightNew: Property<Boolean> = Property.of(true)

    @Expose
    @ConfigOption(name = "Extra space", desc = "Space between each line of text.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 10f, minStep = 1f)
    override val extraSpace: Property<Int> = Property.of(1)

    @Expose
    @ConfigOption(name = "Sorted By", desc = "Sorting type of trophy fish in the display.")
    @ConfigEditorDropdown
    override val sortingType: Property<TrophySorting> = Property.of(TrophySorting.ITEM_RARITY)

    @Expose
    @ConfigOption(name = "Reverse Order", desc = "Reverse the sorting order.")
    @ConfigEditorBoolean
    override val reverseOrder: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Text Order", desc = "Drag text to change the line format.")
    @ConfigEditorDraggableList
    override val textOrder: Property<MutableList<TextPart>> = Property.of(
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

    @Expose
    @ConfigOption(name = "Show ✖", desc = "Instead of the number 0, show §c✖ §7if not found.")
    @ConfigEditorBoolean
    override val showCross: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Show ✔", desc = "Instead of the exact numbers, show §e§l✔ §7if found.")
    @ConfigEditorBoolean
    override val showCheckmark: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigOption(name = "Only Show Missing", desc = "Only show Trophy Fish that are still missing at this rarity.")
    @ConfigEditorDropdown
    override val onlyShowMissing: Property<HideCaught> = Property.of(HideCaught.NONE)

    @Expose
    @ConfigOption(
        name = "Show If Caught Higher Tier",
        desc = "Show Trophy Fish missing at the chosen tier even if a higher tier has already been caught.",
    )
    @ConfigEditorBoolean
    override val showCaughtHigher: Property<Boolean> = Property.of(false)

    @Expose
    @ConfigLink(owner = TrophyFishDisplayConfig::class, field = "enabled")
    override val position: Position = Position(144, 139)
}
