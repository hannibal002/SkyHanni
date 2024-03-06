package at.hannibal2.skyhanni.config.features.rift.area.mirrorverse;

import at.hannibal2.skyhanni.config.features.rift.area.mirrorverse.danceroomhelper.DanceRoomHelperConfig;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class MirrorVerseConfig {

    @ConfigOption(name = "Lava Maze", desc = "")
    @Accordion
    @Expose
    public LavaMazeConfig lavaMazeConfig = new LavaMazeConfig();

    @ConfigOption(name = "Upside Down Parkour", desc = "")
    @Accordion
    @Expose
    public UpsideDownParkourConfig upsideDownParkour = new UpsideDownParkourConfig();

    @ConfigOption(name = "Dance Room Helper", desc = "")
    @Accordion
    @Expose
    public DanceRoomHelperConfig danceRoomHelper = new DanceRoomHelperConfig();

    @ConfigOption(name = "Tubulator", desc = "")
    @Accordion
    @Expose
    public TubulatorConfig tubulatorConfig = new TubulatorConfig();
}
