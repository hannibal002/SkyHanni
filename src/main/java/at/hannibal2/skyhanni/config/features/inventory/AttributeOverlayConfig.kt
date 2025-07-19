package at.hannibal2.skyhanni.config.features.inventory

import at.hannibal2.skyhanni.config.FeatureToggle
import at.hannibal2.skyhanni.features.inventory.attribute.AttributeApi
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class AttributeOverlayConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the attribute name and level on the item.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    // TODO: add way of making config options with data classes from repo
    @Expose
    @ConfigOption(name = "Attributes Shown", desc = "List of attributes shown.")
    @ConfigEditorDraggableList
    val attributesList: MutableList<AttributeApi.AttributeType> = AttributeApi.AttributeType.entries.toMutableList()

    @Expose
    @ConfigOption(name = "Min Level", desc = "Minimum level to show the attributes of.")
    @ConfigEditorSlider(minValue = 1f, maxValue = 10f, minStep = 1f)
    var minimumLevel: Int = 1

    @Expose
    @ConfigOption(
        name = "Highlight Good Rolls",
        desc = "Highlights Good attribute combinations.\n" +
            "§cNote: These are subjective and ever changing. If you\n" +
            "§c want to suggest changes, please do so in the discord."
    )
    @ConfigEditorBoolean
    var highlightGoodRolls: Boolean = true

    @Expose
    @ConfigOption(
        name = "Highlight Good Attribute",
        desc = "Highlights attributes that are in one of the Good Rolls\n" +
            "combinations for that item."
    )
    @ConfigEditorBoolean
    var highlightGoodAttributes: Boolean = false

    @Expose
    @ConfigOption(
        name = "Good Rolls Override Level",
        desc = "Makes it so that Good Rolls are always shown no matter the attribute level."
    )
    @ConfigEditorBoolean
    var goodRollsOverrideLevel: Boolean = true

    @Expose
    @ConfigOption(
        name = "Good Rolls ignore list",
        desc = "Highlights attributes in good rolls even if they aren't in the attributes list."
    )
    @ConfigEditorBoolean
    var ignoreList: Boolean = false

    @Expose
    @ConfigOption(name = "Hide non Good Rolls", desc = "Hides attributes that are not considered good rolls.")
    @ConfigEditorBoolean
    var hideNonGoodRolls: Boolean = false
}
