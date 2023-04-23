package at.hannibal2.skyhanni.config.features;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorKeybind;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import org.lwjgl.input.Keyboard;

public class Diana {

    @Expose
    @ConfigOption(name = "Soopy Guess", desc = "Uses §eSoopy's Guess Logic §7to find the next burrow. Does not require SoopyV2 or ChatTriggers to be installed.")
    @ConfigEditorBoolean
    public boolean burrowsSoopyGuess = false;

    @Expose
    @ConfigOption(name = "Nearby Detection", desc = "Show burrows near you.")
    @ConfigEditorBoolean
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

}
