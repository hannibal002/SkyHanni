package at.hannibal2.skyhanni.config.features.combat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnderNodeConfig {
    @Expose
    @ConfigOption(
        name = "Enabled",
        desc = "Tracks all of your drops from mining Ender Nodes in the End.\n" +
            "Also tracks drops from Endermen."
    )
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(
        name = "Text Format",
        desc = "Drag text to change the appearance of the overlay."
    )
    @ConfigEditorDraggableList(
        exampleText = {
            "§5§lEnder Node Tracker",
            "§d1,303 Ender Nodes Mined",
            "§615.3M Coins Made",
            " ",
            "§b123 §cEndermite Nest",
            "§b832 §aEnchanted End Stone",
            "§b230 §aEnchanted Obsidian",
            "§b1630 §aEnchanted Ender Pearl",
            "§b85 §aGrand Experience Bottle",
            "§b4 §9Titanic Experience Bottle",
            "§b15 §9End Stone Shulker",
            "§b53 §9End Stone Geode",
            "§b10 §d◆ Magical Rune I",
            "§b24 §5Ender Gauntlet",
            "§b357 §5Mite Gel",
            "§b2 §cShrimp The Fish",
            " ",
            "§b200 §5Ender Armor",
            "§b24 §5Ender Helmet",
            "§b24 §5Ender Chestplate",
            "§b24 §5Ender Leggings",
            "§b24 §5Ender Boots",
            "§b24 §5Ender Necklace",
            "§f10§7-§a8§7-§93§7-§52§7-§61 §fEnderman Pet",
            " "
        }
    )
    public Property<List<Integer>> textFormat = Property.of(new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 12, 14, 15, 16, 17, 23)));

    @Expose
    public Position position = new Position(10, 80, false, true);
}
