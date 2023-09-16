package at.hannibal2.skyhanni.config.features;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.*;
import org.lwjgl.input.Keyboard;

public class DianaConfig {

    @Expose
    @ConfigOption(name = "Soopy Guess", desc = "Uses §eSoopy's Guess Logic §7to find the next burrow. Does not require SoopyV2 or ChatTriggers to be installed.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean burrowsSoopyGuess = false;

    @Expose
    @ConfigOption(name = "Nearby Detection", desc = "Show burrows near you.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean burrowsNearbyDetection = false;

    @Expose
    @ConfigOption(name = "Smooth Transition", desc = "Show the way from one burrow to another smoothly.")
    @ConfigEditorBoolean
    public boolean burrowSmoothTransition = false;

    @Expose
    @ConfigOption(name = "Nearest Warp", desc = "Warps to the nearest warp point on the hub, if closer to the next burrow.")
    @ConfigEditorBoolean
    public boolean burrowNearestWarp = false;

    @Expose
    @ConfigOption(name = "Warp Key", desc = "Press this key to warp to nearest burrow waypoint.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int keyBindWarp = Keyboard.KEY_NONE;

    @Expose
    @ConfigOption(name = "Ignored Warps", desc = "")
    @Accordion
    public IgnoredWarpsConfig ignoredWarps = new IgnoredWarpsConfig();

    public static class IgnoredWarpsConfig {

        @Expose
        @ConfigOption(name = "Crypt", desc = "Ignore the Crypt warp point (Because it takes a long time to leave).")
        @ConfigEditorBoolean
        public boolean crypt = false;

        @Expose
        @ConfigOption(name = "Wizard", desc = "Ignore the Wizard Tower warp point (Because it is easy to fall into the rift).")
        @ConfigEditorBoolean
        public boolean wizard = false;

    }

    @Expose
    @ConfigOption(name = "Inquisitor Waypoint Sharing", desc = "")
    @Accordion
    @ConfigAccordionId(id = 9)
    public InquisitorSharing inquisitorSharing = new InquisitorSharing();

    public static class InquisitorSharing {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Shares your Inquisitor and receiving other Inquisitors via Party Chat.")
        @ConfigEditorBoolean
        @FeatureToggle
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Focus", desc = "Hide other waypoints when your Party finds a Inquisitor.")
        @ConfigEditorBoolean
        public boolean focusInquisitor = false;

        @Expose
        @ConfigOption(name = "Instant Share", desc = "Share the waypoint as soon as you find a Inquisitor. As alternative, you can share it only via key press")
        @ConfigEditorBoolean
        public boolean instantShare = true;

        @Expose
        @ConfigOption(name = "Share Key", desc = "Press this key to share your Inquisitor Waypoint.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_Y)
        public int keyBindShare = Keyboard.KEY_Y;

        @Expose
        @ConfigOption(name = "Show Despawn Time", desc = "Show the time until the shared Inquisitor will despawn.")
        @ConfigEditorBoolean
        public boolean showDespawnTime = true;
    }

    @Expose
    @ConfigOption(name = "Griffin Pet Warning", desc = "Warn when holding an Ancestral Spade if a Griffin pet is not equipped.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean petWarning = true;
}
