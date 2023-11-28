package at.hannibal2.skyhanni.config.features.combat.damageindicator;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorBossEntry.ARACHNE;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorBossEntry.DIANA_MOBS;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorBossEntry.DUNGEON_ALL;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorBossEntry.GARDEN_PESTS;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorBossEntry.INFERNO_DEMONLORD;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorBossEntry.NETHER_MINI_BOSSES;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorBossEntry.REINDRAKE;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorBossEntry.REVENANT_HORROR;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorBossEntry.RIFTSTALKER_BLOODFIEND;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorBossEntry.SEA_CREATURES;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorBossEntry.SVEN_PACKMASTER;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorBossEntry.TARANTULA_BROODFATHER;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorBossEntry.THE_RIFT_BOSSES;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorBossEntry.VANQUISHER;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorBossEntry.VOIDGLOOM_SERAPH;

public class DamageIndicatorConfig {

    @Expose
    @ConfigOption(name = "Damage Indicator Enabled", desc = "Show the boss' remaining health.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    @ConfigOption(name = "Healing Chat Message", desc = "Sends a chat message when a boss heals themself.")
    @ConfigEditorBoolean
    public boolean healingMessage = false;

    @Expose
    @ConfigOption(
        name = "Boss Name",
        desc = "Change how the boss name should be displayed.")
    @ConfigEditorDropdown(values = {"Hidden", "Full Name", "Short Name"})
    public int bossName = 1;

    @Expose
    @ConfigOption(
        name = "Select Boss",
        desc = "Change what type of boss you want the damage indicator be enabled for."
    )
    @ConfigEditorDraggableList()
    //TODO only show currently working and tested features
    public List<DamageIndicatorBossEntry> bossesToShow = new ArrayList<>(Arrays.asList(
        DUNGEON_ALL,
        NETHER_MINI_BOSSES,
        VANQUISHER,
        REVENANT_HORROR,
        TARANTULA_BROODFATHER,
        SVEN_PACKMASTER,
        VOIDGLOOM_SERAPH,
        INFERNO_DEMONLORD,
        DIANA_MOBS,
        SEA_CREATURES,
        ARACHNE,
        THE_RIFT_BOSSES,
        RIFTSTALKER_BLOODFIEND,
        REINDRAKE,
        GARDEN_PESTS

    ));

    @Expose
    @ConfigOption(name = "Hide Damage Splash", desc = "Hiding damage splashes near the damage indicator.")
    @ConfigEditorBoolean
    public boolean hideDamageSplash = false;

    @Expose
    @ConfigOption(name = "Damage Over Time", desc = "Show damage and health over time below the damage indicator.")
    @ConfigEditorBoolean
    public boolean showDamageOverTime = false;

    @Expose
    @ConfigOption(name = "Hide Nametag", desc = "Hide the vanilla nametag of damage indicator bosses.")
    @ConfigEditorBoolean
    public boolean hideVanillaNametag = false;

    @Expose
    @ConfigOption(name = "Time to Kill", desc = "Show the time it takes to kill the slayer boss.")
    @ConfigEditorBoolean
    public boolean timeToKillSlayer = true;


    @Expose
    @ConfigOption(name = "Ender Slayer", desc = "")
    @Accordion
    public EnderSlayerConfig enderSlayer = new EnderSlayerConfig();

    @Expose
    @ConfigOption(name = "Vampire Slayer", desc = "")
    @Accordion
    public VampireSlayerConfig vampireSlayer = new VampireSlayerConfig();
}
