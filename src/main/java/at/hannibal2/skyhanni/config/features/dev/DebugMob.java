package at.hannibal2.skyhanni.config.features.dev;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
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

    @Expose
    @ConfigOption(name = "Mob Hit Detection", desc = "Debugs for the ways to Hit a Mob")
    @Accordion
    public MobHitDetecion mobHitDetecion = new MobHitDetecion();

    public static class MobHitDetecion {

        @Expose
        @ConfigOption(name = "Hit Highlight", desc = "Highlight each entity that is in the HitList")
        @ConfigEditorBoolean
        public boolean skyblockMobHitHighlight = false;

        @Expose
        @ConfigOption(name = "Highlight Ray Hit", desc = "Highlights the SkyblockMob that is directly in front of the camera")
        @ConfigEditorBoolean
        public boolean skyblockMobHighlightRayHit = false;

        @Expose
        @ConfigOption(name = "Kill Message", desc = "Shows a Kill Message in Chat")
        @ConfigEditorBoolean
        public boolean ShowNameOfKilledMob = false;

        @Expose
        @ConfigOption(name = "Log HitList", desc = "Logs the complete HitList each Tick")
        @ConfigEditorBoolean
        public boolean LogMobHitList = false;

        @Expose
        @ConfigOption(name = "HitList Log as ID", desc = "The HitList will be log only as Mob ID instead of the Mob Name")
        @ConfigEditorBoolean
        public boolean LogMobHitListId = false;

        @Expose
        @ConfigOption(name = "Debug Arrow", desc = "Enables all Debug functions for the Arrow Detection")
        @ConfigEditorBoolean
        public boolean arrowDebug = false;

        @Expose
        @ConfigOption(name = "Bow Strength", desc = "Adjust default strength of Bows")
        @ConfigEditorSlider(minValue = 1.0f, maxValue = 10.0f, minStep = 0.1f)
        public double bowStrength = 3.0;
        @Expose
        @ConfigOption(name = "Arrow Gravity", desc = "Adjust default Gravity of Arrows")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.0001f)
        public double arrowGravity = 0.025;
        @Expose
        @ConfigOption(name = "Arrow Drag", desc = "Adjust default Drag of Arrows")
        @ConfigEditorSlider(minValue = 0.0f, maxValue = 1.0f, minStep = 0.001f)
        public double arrowDrag = 0.99;

        @Expose
        @ConfigOption(name = "Debug Cleave", desc = "Logs Cleave Hits and shows range")
        @ConfigEditorBoolean
        public boolean cleaveDebug = false;
    }

}
