package at.hannibal2.skyhanni.config.features.combat.damageindicator;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.HasLegacyId;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig.DamageIndicatorBossEntry.ARACHNE;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig.DamageIndicatorBossEntry.DIANA_MOBS;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig.DamageIndicatorBossEntry.DUNGEON_ALL;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig.DamageIndicatorBossEntry.GARDEN_PESTS;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig.DamageIndicatorBossEntry.INFERNO_DEMONLORD;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig.DamageIndicatorBossEntry.NETHER_MINI_BOSSES;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig.DamageIndicatorBossEntry.REINDRAKE;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig.DamageIndicatorBossEntry.REVENANT_HORROR;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig.DamageIndicatorBossEntry.RIFTSTALKER_BLOODFIEND;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig.DamageIndicatorBossEntry.SEA_CREATURES;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig.DamageIndicatorBossEntry.SVEN_PACKMASTER;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig.DamageIndicatorBossEntry.TARANTULA_BROODFATHER;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig.DamageIndicatorBossEntry.THE_RIFT_BOSSES;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig.DamageIndicatorBossEntry.VANQUISHER;
import static at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig.DamageIndicatorBossEntry.VOIDGLOOM_SERAPH;

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

    public enum DamageIndicatorBossEntry implements HasLegacyId {
        DUNGEON_ALL("§bDungeon All", 0),
        NETHER_MINI_BOSSES("§bNether Mini Bosses", 1),
        VANQUISHER("§bVanquisher", 2),
        ENDERSTONE_PROTECTOR("§bEndstone Protector (not tested)", 3),
        ENDER_DRAGON("§bEnder Dragon (not finished)", 4),
        REVENANT_HORROR("§bRevenant Horror", 5),
        TARANTULA_BROODFATHER("§bTarantula Broodfather", 6),
        SVEN_PACKMASTER("§bSven Packmaster", 7),
        VOIDGLOOM_SERAPH("§bVoidgloom Seraph", 8),
        INFERNO_DEMONLORD("§bInferno Demonlord", 9),
        HEADLESS_HORSEMAN("§bHeadless Horseman (bugged)", 10),
        DUNGEON_FLOOR_1("§bDungeon Floor 1", 11),
        DUNGEON_FLOOR_2("§bDungeon Floor 2", 12),
        DUNGEON_FLOOR_3("§bDungeon Floor 3", 13),
        DUNGEON_FLOOR_4("§bDungeon Floor 4", 14),
        DUNGEON_FLOOR_5("§bDungeon Floor 5", 15),
        DUNGEON_FLOOR_6("§bDungeon Floor 6", 16),
        DUNGEON_FLOOR_7("§bDungeon Floor 7", 17),
        DIANA_MOBS("§bDiana Mobs", 18),
        SEA_CREATURES("§bSea Creatures", 19),
        DUMMY("Dummy", 20),
        ARACHNE("§bArachne", 21),
        THE_RIFT_BOSSES("§bThe Rift Bosses", 22),
        RIFTSTALKER_BLOODFIEND("§bRiftstalker Bloodfiend", 23),
        REINDRAKE("§6Reindrake", 24),
        GARDEN_PESTS("§aGarden Pests", 25),
        ;

        private final String str;
        private final int legacyId;

        DamageIndicatorBossEntry(String str, int legacyId) {
            this.str = str;
            this.legacyId = legacyId;
        }

        // Constructor if new enum elements are added post-migration
        DamageIndicatorBossEntry(String str) {
            this(str, -1);
        }

        @Override
        public int getLegacyId() {
            return legacyId;
        }

        @Override
        public String toString() {
            return str;
        }
    }
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
