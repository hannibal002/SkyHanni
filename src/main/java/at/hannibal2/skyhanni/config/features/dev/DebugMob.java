package at.hannibal2.skyhanni.config.features.dev;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class DebugMob {

    @Expose
    @ConfigOption(name = "Force Reset", desc = "Resets all Mobs, turn off to enable Mob Detetcion again")
    @ConfigEditorBoolean
    public boolean forceReset = false;

    @Expose
    @ConfigOption(name = "Mob Detection", desc = "Debug feature to check the Mob Detection")
    @Accordion
    public MobDetection mobDetection = new MobDetection();

    public static class MobDetection {

        @Expose
        @ConfigOption(name = "SkyblockMob Highlight", desc = "Highlight each entity that is a valid SkyblockMob in green")
        @ConfigEditorBoolean
        public boolean skyblockMobHighlight = false;

        @Expose
        @ConfigOption(name = "DisplayNPC Highlight", desc = "Highlight each entity that is a valid DisplayNPC in red")
        @ConfigEditorBoolean
        public boolean displayNPCHighlight = false;

        @Expose
        @ConfigOption(name = "Player Highlight", desc = "Highlight each entity that is a real Player in blue. (Yourself is also include in the list but won't be Highlighted for obvious reason)")
        @ConfigEditorBoolean
        public boolean realPlayerHighlight = false;

        @Expose
        @ConfigOption(name = "Summon Highlight", desc = "Highlight each entity that is a valid Summon in yellow")
        @ConfigEditorBoolean
        public boolean summonHighlight = false;

        @Expose
        @ConfigOption(name = "SkyblockMob Show Name", desc = "Addes Hologram to each SkyblockMob that shows there name")
        @ConfigEditorBoolean
        public boolean skyblockMobShowName = false;

        @Expose
        @ConfigOption(name = "DisplayNPC Show Name", desc = "Addes Hologram to each DisplayNPC that shows there name")
        @ConfigEditorBoolean
        public boolean displayNPCShowName = false;
    }
}
