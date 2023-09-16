package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigAccordionId;
import io.github.moulberry.moulconfig.annotations.ConfigEditorAccordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class SummoningsConfig {

    @Expose
    @ConfigOption(name = "Summoning Soul Display", desc = "Show the name of dropped Summoning Souls laying on the ground. " +
            "§cNot working in dungeons if Skytils' 'Hide Non-Starred Mobs Nametags' feature is enabled!")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean summoningSoulDisplay = false;

    @Expose
    @ConfigOption(name = "Summoning Mob", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean summoningMob = false;

    @Expose
    @ConfigOption(name = "Summoning Mob Display", desc = "Show the health of your spawned summons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    @FeatureToggle
    public boolean summoningMobDisplay = false;

    @Expose
    public Position summoningMobDisplayPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Summoning Mob Nametag", desc = "Hide the nametag of your spawned summons.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    @FeatureToggle
    public boolean summoningMobHideNametag = false;

    @Expose
    @ConfigOption(name = "Summoning Mob Color", desc = "Marks own summons green.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    @FeatureToggle
    public boolean summoningMobColored = false;
}
