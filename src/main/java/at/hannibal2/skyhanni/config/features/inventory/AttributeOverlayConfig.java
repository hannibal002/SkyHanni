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
    public List<AttributeAPI.AttributeType> attributesList = new ArrayList<>(Arrays.asList(
        AttributeAPI.AttributeType.ARACHNO,
        AttributeAPI.AttributeType.ATTACK_SPEED,
        AttributeAPI.AttributeType.BLAZING,
        AttributeAPI.AttributeType.COMBO,
        AttributeAPI.AttributeType.ELITE,
        AttributeAPI.AttributeType.ENDER,
        AttributeAPI.AttributeType.IGNITION,
        AttributeAPI.AttributeType.LIFE_RECOVERY,
        AttributeAPI.AttributeType.MANA_STEAL,
        AttributeAPI.AttributeType.MIDAS_TOUCH,
        AttributeAPI.AttributeType.UNDEAD,
        AttributeAPI.AttributeType.WARRIOR,
        AttributeAPI.AttributeType.DEADEYE,
        AttributeAPI.AttributeType.ARACHNO_RESISTANCE,
        AttributeAPI.AttributeType.BLAZING_RESISTANCE,
        AttributeAPI.AttributeType.BREEZE,
        AttributeAPI.AttributeType.DOMINANCE,
        AttributeAPI.AttributeType.ENDER_RESISTANCE,
        AttributeAPI.AttributeType.EXPERIENCE,
        AttributeAPI.AttributeType.FORTITUDE,
        AttributeAPI.AttributeType.LIFE_REGENERATION,
        AttributeAPI.AttributeType.LIFELINE,
        AttributeAPI.AttributeType.MAGIC_FIND,
        AttributeAPI.AttributeType.MANA_POOL,
        AttributeAPI.AttributeType.MANA_REGENERATION,
        AttributeAPI.AttributeType.VITALITY,
        AttributeAPI.AttributeType.SPEED,
        AttributeAPI.AttributeType.UNDEAD_RESISTANCE,
        AttributeAPI.AttributeType.VETERAN,
        AttributeAPI.AttributeType.BLAZING_FORTUNE,
        AttributeAPI.AttributeType.FISHING_EXPERIENCE,
        AttributeAPI.AttributeType.INFECTION,
        AttributeAPI.AttributeType.DOUBLE_HOOK,
        AttributeAPI.AttributeType.FISHERMAN,
        AttributeAPI.AttributeType.FISHING_SPEED,
        AttributeAPI.AttributeType.HUNTER,
        AttributeAPI.AttributeType.TROPHY_HUNTER
    ));

    @Expose
    @ConfigOption(
        name = "Min Level",
        desc = "Minimum level to show the attributes of.\n" +
            "(Overridden by Highlight Good Rolls)"
    )
    @ConfigEditorSlider(minValue = 0, maxValue = 10, minStep = 1)
    public int minimumLevel = 0;

    @Expose
    @ConfigOption(
        name = "Highlight Good Rolls",
        desc = "Highlights Good attribute combinations.\n" +
            "§cNote: These are subjective and ever changing. If you\n" +
            "§c want to suggest changes, please do so in the discord."
    )
    @ConfigEditorBoolean
    public boolean highlightGodrolls = true;

    @Expose
    @ConfigOption(name = "Hide non Good Rolls", desc = "Hides attributes that are not considered good rolls.")
    @ConfigEditorBoolean
    public boolean hideNonGoodRolls = false;
}
