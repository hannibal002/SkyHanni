package at.hannibal2.skyhanni.config.features;

import io.github.moulberry.moulconfig.annotations.*;
import org.lwjgl.input.Keyboard;

public class Diana {

    @ConfigOption(name = "Soopy Guess", desc = "Uses §eSoopy's Guess Logic §7to find the next burrow. Does not require SoopyV2 or ChatTriggers to be installed.")
    @ConfigEditorBoolean
    public boolean burrowsSoopyGuess = false;

    @ConfigOption(name = "Nearby Detection", desc = "Show burrows near you.")
    @ConfigEditorBoolean
    public boolean burrowsNearbyDetection = false;

    @ConfigOption(name = "Smooth Transition", desc = "Show the way from one burrow to another smoothly.")
    @ConfigEditorBoolean
    public boolean burrowSmoothTransition = false;

    @ConfigOption(name = "Nearest Warp", desc = "Warps to the nearest warp point on the hub, if closer to the next burrow.")
    @ConfigEditorBoolean
    public boolean burrowNearestWarp = false;

    @ConfigOption(name = "Warp Key", desc = "Press this key to warp to nearest burrow waypoint.")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_NONE)
    public int keyBindWarp = Keyboard.KEY_NONE;

    @ConfigOption(name = "Inquisitor Waypoint Sharing", desc = "")
    @Accordion
    @ConfigAccordionId(id = 9)
    public InquisitorSharing inquisitorSharing = new InquisitorSharing();

    public static class InquisitorSharing {

        @ConfigOption(name = "Enabled", desc = "Shares your Inquisitor and receiving other Inquisitors via Party Chat.")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @ConfigOption(name = "Focus", desc = "Hide other waypoints when your party finds a inquisitor.")
        @ConfigEditorBoolean
        public boolean focusInquisitor = false;

        @ConfigOption(name = "Instant Share", desc = "Share the waypoint as soon as you find a inquisitor. As alternative, you can share it only via key press")
        @ConfigEditorBoolean
        public boolean instantShare = true;

        @ConfigOption(name = "Share Key", desc = "Press this key to share your Inquisitor Waypoint.")
        @ConfigEditorKeybind(defaultKey = Keyboard.KEY_Y)
        public int keyBindShare = Keyboard.KEY_Y;
    }

}
