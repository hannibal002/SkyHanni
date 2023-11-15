package at.hannibal2.skyhanni.config.features.combat;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.features.combat.damageindicator.DamageIndicatorConfig;
import at.hannibal2.skyhanni.config.features.combat.ghostcounter.GhostCounterConfig;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class CombatConfig {

    @Expose
    @ConfigOption(name = "Damage Indicator", desc = "")
    @Accordion
    public DamageIndicatorConfig damageIndicator = new DamageIndicatorConfig();

    @Expose
    @ConfigOption(name = "Ghost Counter", desc = "")
    @Accordion
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
    @ConfigOption(name = "Hide Damage Splash", desc = "Hide all damage splashes anywhere in SkyBlock.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean hideDamageSplash = false;
}
