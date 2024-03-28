package at.hannibal2.skyhanni.config.features.combat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig;
import at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostCounterConfig;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.Category;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CombatConfig {

    @Expose
    @Category(name = "Damage Indicator", desc = "Damage Indicator settings")
    public DamageIndicatorConfig damageIndicator = new DamageIndicatorConfig();

    @Expose
    @Category(name = "Ghost Counter", desc = "Ghost counter settings")
    public GhostCounterConfig ghostCounter = new GhostCounterConfig();

    @Expose
    @ConfigOption(name = "Summonings", desc = "")
    @Accordion
    public SummoningsConfig summonings = new SummoningsConfig();

    @Expose
    @ConfigOption(name = "Mobs", desc = "")
    @Accordion
    public MobsConfig mobs = new MobsConfig();

    @Expose
    @ConfigOption(name = "Bestiary", desc = "")
    @Accordion
    public BestiaryConfig bestiary = new BestiaryConfig();

    @Expose
    @ConfigOption(name = "Ender Node Tracker", desc = "")
    @Accordion
    public EnderNodeConfig enderNodeTracker = new EnderNodeConfig();

    @Expose
    @ConfigOption(name = "Dragon Features", desc = "")
    @Accordion
    public DragonConfig dragon = new DragonConfig();

    @Expose
    @ConfigOption(name = "Weight Endstone Protector", desc = "Shows your Endstone Protector weight in chat after the it died")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean endstoneProtectorChat = true;


    @Expose
    @ConfigOption(name = "Hide Damage Splash", desc = "Hide all damage splashes anywhere in SkyBlock.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideDamageSplash = false;
}
