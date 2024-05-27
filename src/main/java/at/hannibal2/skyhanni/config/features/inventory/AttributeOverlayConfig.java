package at.hannibal2.skyhanni.config.features.inventory;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.features.inventory.attribute.AttributeAPI;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AttributeOverlayConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Show the attribute name and level on the item.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Attributes Shown", desc = "List of attributes shown")
    @ConfigEditorDraggableList
    public List<AttributeAPI.Attribute> attributesList = new ArrayList<>(Arrays.asList(
        AttributeAPI.Attribute.ARACHNO,
        AttributeAPI.Attribute.ATTACK_SPEED,
        AttributeAPI.Attribute.BLAZING,
        AttributeAPI.Attribute.COMBO,
        AttributeAPI.Attribute.ELITE,
        AttributeAPI.Attribute.ENDER,
        AttributeAPI.Attribute.IGNITION,
        AttributeAPI.Attribute.LIFE_RECOVERY,
        AttributeAPI.Attribute.MANA_STEAL,
        AttributeAPI.Attribute.MIDAS_TOUCH,
        AttributeAPI.Attribute.UNDEAD,
        AttributeAPI.Attribute.WARRIOR,
        AttributeAPI.Attribute.DEADEYE,
        AttributeAPI.Attribute.ARACHNO_RESISTANCE,
        AttributeAPI.Attribute.BLAZING_RESISTANCE,
        AttributeAPI.Attribute.BREEZE,
        AttributeAPI.Attribute.DOMINANCE,
        AttributeAPI.Attribute.ENDER_RESISTANCE,
        AttributeAPI.Attribute.EXPERIENCE,
        AttributeAPI.Attribute.FORTITUDE,
        AttributeAPI.Attribute.LIFE_REGENERATION,
        AttributeAPI.Attribute.LIFELINE,
        AttributeAPI.Attribute.MAGIC_FIND,
        AttributeAPI.Attribute.MANA_POOL,
        AttributeAPI.Attribute.MANA_REGENERATION,
        AttributeAPI.Attribute.VITALITY,
        AttributeAPI.Attribute.SPEED,
        AttributeAPI.Attribute.UNDEAD_RESISTANCE,
        AttributeAPI.Attribute.VETERAN,
        AttributeAPI.Attribute.BLAZING_FORTUNE,
        AttributeAPI.Attribute.FISHING_EXPERIENCE,
        AttributeAPI.Attribute.INFECTION,
        AttributeAPI.Attribute.DOUBLE_HOOK,
        AttributeAPI.Attribute.FISHERMAN,
        AttributeAPI.Attribute.FISHING_SPEED,
        AttributeAPI.Attribute.HUNTER,
        AttributeAPI.Attribute.TROPHY_HUNTER
    ));

    @Expose
    @ConfigOption(
        name = "Min Level",
        desc = "Minimum level to show the attributes of.\n" +
            "(Overridden by Highlight Godrolls)"
    )
    @ConfigEditorSlider(minValue = 0, maxValue = 10, minStep = 1)
    public int minimumLevel = 0;

    @Expose
    @ConfigOption(
        name = "Highlight Godrolls",
        desc = "Highlights \"Godroll\" attribute combinations.\n" +
            "§cNote: These are subjective and ever changing. If you\n" +
            "§c want to suggest changes, please do so in the discord."
    )
    @ConfigEditorBoolean
    public boolean highlightGodrolls = true;
}
