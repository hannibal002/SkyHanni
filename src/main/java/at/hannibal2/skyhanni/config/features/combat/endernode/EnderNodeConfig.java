package at.hannibal2.skyhanni.config.features.combat.endernode;

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

import static at.hannibal2.skyhanni.config.features.combat.endernode.EnderNodeDisplayEntry.COINS_MADE;
import static at.hannibal2.skyhanni.config.features.combat.endernode.EnderNodeDisplayEntry.ENCHANTED_ENDER_PEARL;
import static at.hannibal2.skyhanni.config.features.combat.endernode.EnderNodeDisplayEntry.ENCHANTED_END_STONE;
import static at.hannibal2.skyhanni.config.features.combat.endernode.EnderNodeDisplayEntry.ENCHANTED_OBSIDIAN;
import static at.hannibal2.skyhanni.config.features.combat.endernode.EnderNodeDisplayEntry.ENDERMAN_PET;
import static at.hannibal2.skyhanni.config.features.combat.endernode.EnderNodeDisplayEntry.ENDERMITE_NEST;
import static at.hannibal2.skyhanni.config.features.combat.endernode.EnderNodeDisplayEntry.ENDER_ARMOR;
import static at.hannibal2.skyhanni.config.features.combat.endernode.EnderNodeDisplayEntry.GRAND_XP_BOTTLE;
import static at.hannibal2.skyhanni.config.features.combat.endernode.EnderNodeDisplayEntry.MAGICAL_RUNE_I;
import static at.hannibal2.skyhanni.config.features.combat.endernode.EnderNodeDisplayEntry.MITE_GEL;
import static at.hannibal2.skyhanni.config.features.combat.endernode.EnderNodeDisplayEntry.NODES_MINED;
import static at.hannibal2.skyhanni.config.features.combat.endernode.EnderNodeDisplayEntry.SHRIMP_THE_FISH;
import static at.hannibal2.skyhanni.config.features.combat.endernode.EnderNodeDisplayEntry.SPACER_1;
import static at.hannibal2.skyhanni.config.features.combat.endernode.EnderNodeDisplayEntry.SPACER_2;
import static at.hannibal2.skyhanni.config.features.combat.endernode.EnderNodeDisplayEntry.TITANIC_XP_BOTTLE;
import static at.hannibal2.skyhanni.config.features.combat.endernode.EnderNodeDisplayEntry.TITLE;

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
    @ConfigEditorDraggableList()
    public Property<List<EnderNodeDisplayEntry>> textFormat = Property.of(new ArrayList<>(Arrays.asList(
        TITLE,
        NODES_MINED,
        COINS_MADE,
        SPACER_1,
        ENDERMITE_NEST,
        ENCHANTED_END_STONE,
        ENCHANTED_OBSIDIAN,
        ENCHANTED_ENDER_PEARL,
        GRAND_XP_BOTTLE,
        TITANIC_XP_BOTTLE,
        MAGICAL_RUNE_I,
        MITE_GEL,
        SHRIMP_THE_FISH,
        SPACER_2,
        ENDER_ARMOR,
        ENDERMAN_PET)
    ));

    @Expose
    public Position position = new Position(10, 80, false, true);
}
