package at.hannibal2.skyhanni.config.features.combat.damageindicator

import at.hannibal2.skyhanni.config.FeatureToggle
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDraggableList
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class DamageIndicatorConfig {

    // TODO rename to showNameAndHealth
    @Expose
    @ConfigOption(name = "Damage Indicator Enabled", desc = "Show name and health of selected mobs big.")
    @ConfigEditorBoolean
    @FeatureToggle
    var enabled: Boolean = false

    @Expose
    @ConfigOption(name = "Boss Name", desc = "Change how boss names are displayed.")
    @ConfigEditorDropdown
    var bossName: NameVisibility = NameVisibility.FULL_NAME

    enum class NameVisibility(private val displayName: String) {
        HIDDEN("Hidden"),
        FULL_NAME("Full Name"),
        SHORT_NAME("Short Name"),
        ;

        override fun toString() = displayName
    }

    // TODO only show currently working and tested features
    @Expose
    @ConfigOption(name = "Select Boss", desc = "Change what bosses the damage indicator should be enabled for.")
    @ConfigEditorDraggableList
    val bossesToShow: MutableList<BossCategory> = mutableListOf(
        BossCategory.NETHER_MINI_BOSSES,
        BossCategory.VANQUISHER,
        BossCategory.REVENANT_HORROR,
        BossCategory.TARANTULA_BROODFATHER,
        BossCategory.SVEN_PACKMASTER,
        BossCategory.VOIDGLOOM_SERAPH,
        BossCategory.INFERNO_DEMONLORD,
        BossCategory.DIANA_MOBS,
        BossCategory.SEA_CREATURES,
        BossCategory.ARACHNE,
        BossCategory.BROODMOTHER,
        BossCategory.THE_RIFT_BOSSES,
        BossCategory.RIFTSTALKER_BLOODFIEND,
        BossCategory.REINDRAKE,
        BossCategory.GARDEN_PESTS,
    )

    enum class BossCategory(private val displayName: String) {
        NETHER_MINI_BOSSES("§bNether Mini Bosses"),
        VANQUISHER("§bVanquisher"),
        ENDERSTONE_PROTECTOR("§bEndstone Protector"),
        ENDER_DRAGON("§bEnder Dragon"),
        REVENANT_HORROR("§bRevenant Horror"),
        TARANTULA_BROODFATHER("§bTarantula Broodfather"),
        SVEN_PACKMASTER("§bSven Packmaster"),
        VOIDGLOOM_SERAPH("§bVoidgloom Seraph"),
        INFERNO_DEMONLORD("§bInferno Demonlord"),
        HEADLESS_HORSEMAN("§bHeadless Horseman (bugged)"),
        DUNGEON_FLOOR_1("§bDungeon Floor 1"),
        DUNGEON_FLOOR_2("§bDungeon Floor 2"),
        DUNGEON_FLOOR_3("§bDungeon Floor 3"),
        DUNGEON_FLOOR_4("§bDungeon Floor 4"),
        DUNGEON_FLOOR_5("§bDungeon Floor 5"),
        DUNGEON_FLOOR_6("§bDungeon Floor 6"),
        DUNGEON_FLOOR_7("§bDungeon Floor 7"),
        DIANA_MOBS("§bDiana Mobs"),
        SEA_CREATURES("§bSea Creatures"),
        DUMMY("Dummy"),
        ARACHNE("§bArachne"),
        THE_RIFT_BOSSES("§bThe Rift Bosses"),
        RIFTSTALKER_BLOODFIEND("§bRiftstalker Bloodfiend"),
        REINDRAKE("§6Reindrake"),
        GARDEN_PESTS("§aGarden Pests"),
        BROODMOTHER("§bBroodmother");

        override fun toString() = displayName
    }

    @Expose
    @ConfigOption(name = "Hide Damage Splash", desc = "Hide damage splashes near selected mobs.")
    @ConfigEditorBoolean
    var hideDamageSplash: Boolean = false // remain dependent on dmg indicator

    @Expose
    @ConfigOption(name = "Damage Over Time", desc = "Show damage and health over time below the damage indicator.")
    @ConfigEditorBoolean
    var showDamageOverTime: Boolean = false // remain dependent on dmg indicator

    @Expose
    @ConfigOption(name = "Hide Nametag", desc = "Hide the vanilla nametag of bosses with damage indicator enabled.")
    @ConfigEditorBoolean
    var hideVanillaNametag: Boolean = false // remain dependent on dmg indicator

    @Expose
    @ConfigOption(name = "Shuriken Indicator", desc = "Indicate if an Extremely Real Shuriken has been used.")
    @ConfigEditorBoolean
    @FeatureToggle
    var shurikenIndicator: Boolean = true

    @Expose
    @ConfigOption(name = "Twilight Indicator", desc = "Indicate if Twilight Arrow Poison has been used.")
    @ConfigEditorBoolean
    @FeatureToggle
    var twilightIndicator: Boolean = true

    @Expose
    @ConfigOption(
        name = "Compact Status Effects",
        desc = "Use the icons of Extremely Real Shuriken and Twilight Arrow Poison " +
            "instead of their names for the indicator.",
    )
    @ConfigEditorBoolean
    var compactStatusEffects: Boolean = false

    @Expose
    @ConfigOption(
        name = "Time to Kill",
        desc = "Show the time it takes to kill the slayer boss.",
    )
    @ConfigEditorBoolean
    @FeatureToggle
    var timeToKillSlayer: Boolean = true

    @Expose
    @ConfigOption(name = "Show Bacte Phase", desc = "Show the current phase of Bacte in the Rift.")
    @ConfigEditorBoolean
    @FeatureToggle
    var showBactePhase: Boolean = true

    @Expose
    @ConfigOption(name = "Spider Slayer", desc = "")
    @Accordion
    val spiderSlayer: SpiderSlayerConfig = SpiderSlayerConfig()

    @Expose
    @ConfigOption(name = "Ender Slayer", desc = "")
    @Accordion
    val enderSlayer: EnderSlayerConfig = EnderSlayerConfig()

    @Expose
    @ConfigOption(name = "Vampire Slayer", desc = "")
    @Accordion
    val vampireSlayer: VampireSlayerConfig = VampireSlayerConfig()
}
