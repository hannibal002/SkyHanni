package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.core.config.Position;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigAccordionId;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorAccordion;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigEditorBoolean;
import at.hannibal2.skyhanni.config.core.config.annotations.ConfigOption;
import com.google.gson.annotations.Expose;

public class DevData {

    @Expose
    @ConfigOption(name = "Repo Auto Update", desc = "Update the repository on every startup.")
    @ConfigEditorBoolean
    public boolean repoAutoUpdate = true;

    @Expose
    @ConfigOption(name = "Debug", desc = "")
    @ConfigEditorAccordion(id = 0)
    public boolean debugDO_NOT_USE = false;

    @Expose
    @ConfigOption(name = "Enable Debug", desc = "Enable Test logic")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean debugEnabled = false;

    @Expose
//    @ConfigOption(name = "Debug Location", desc = "")
//    @ConfigEditorButton(runnableId = "debugPos", buttonText = "Edit")
//    @ConfigAccordionId(id = 0)
    public Position debugPos = new Position(10, 10, false, true);

    @Expose
    @ConfigOption(name = "Minecraft Console", desc = "")
    @ConfigEditorAccordion(id = 1)
    public boolean minecraftConsole = false;

    @Expose
    @ConfigOption(name = "Unfiltered Debug", desc = "Print the debug information for unfiltered console messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean printUnfilteredDebugs = false;

    @Expose
    @ConfigOption(name = "Unfiltered Debug File", desc = "Print the debug information into log files instead of into the console for unfiltered console messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean logUnfilteredFile = false;

    @Expose
    @ConfigOption(
            name = "Outside SkyBlock",
            desc = "Print the debug information for unfiltered console messages outside SkyBlock too."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean printUnfilteredDebugsOutsideSkyBlock = false;

    @Expose
    @ConfigOption(
            name = "Log Filtered",
            desc = "Log the filtered messages into the console."
    )
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 1)
    public boolean printFilteredReason = false;

    @Expose
    @ConfigOption(name = "Console Filters", desc = "")
    @ConfigAccordionId(id = 1)
    @ConfigEditorAccordion(id = 2)
    public boolean consoleFilters = false;

    @Expose
    @ConfigOption(name = "Filter Chat", desc = "Filter chat messages.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterChat = false;

    @Expose
    @ConfigOption(name = "Filter Grow Buffer", desc = "Filter 'Needed to grow BufferBuilder buffer:'")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterGrowBuffer = true;

    @Expose
    @ConfigOption(name = "Filter Sound Error", desc = "Filter 'Unable to play unknown soundEvent'.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterUnknownSound = true;

    @Expose
    @ConfigOption(name = "Filter Scoreboard Errors", desc = "Filter error messages with Scoreboard: removeTeam, createTeam, " +
            "removeObjective and 'scoreboard team already exists'.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterScoreboardErrors = true;

    @Expose
    @ConfigOption(name = "Filter Particle", desc = "Filter message 'Could not spawn particle effect VILLAGER_HAPPY'.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterParticleVillagerHappy = true;

    @Expose
    @ConfigOption(name = "Filter OptiFine", desc = "Filter OptiFine messages CustomItems and ConnectedTextures during loading.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterOptiFine = true;

    @Expose
    @ConfigOption(name = "Filter AsmHelper Transformer", desc = "Filter messages when AsmHelper is Transforming a class during loading.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterAmsHelperTransformer = true;

    @Expose
    @ConfigOption(name = "Filter Applying AsmWriter", desc = "Filter messages when AsmHelper is applying AsmWriter ModifyWriter.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterAsmHelperApplying = true;

    @Expose
    @ConfigOption(name = "Filter Biome ID Bounds", desc = "Filter message 'Biome ID is out of bounds'.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 2)
    public boolean filterBiomeIdBounds = true;

    @Expose
    @ConfigOption(name = "Print Event Timings", desc = "Prints every 10 seconds the timings of all SkyHanni events in the chat.")
    @ConfigEditorBoolean
    public boolean printEventTimings = false;
}
