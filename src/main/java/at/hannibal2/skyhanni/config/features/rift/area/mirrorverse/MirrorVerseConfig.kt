package at.hannibal2.skyhanni.config.features.rift.area.mirrorverse

import at.hannibal2.skyhanni.config.features.rift.area.mirrorverse.danceroomhelper.DanceRoomHelperConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MirrorVerseConfig {
    // Four Levers
    @ConfigOption(name = "Lava Maze", desc = "")
    @Accordion
    @Expose
    var lavaMazeConfig: LavaMazeConfig = LavaMazeConfig()

    @ConfigOption(name = "Crafting Room", desc = "")
    @Accordion
    @Expose
    var craftingRoom: CraftingRoomConfig = CraftingRoomConfig()

    @ConfigOption(name = "Upside-Down Parkour", desc = "")
    @Accordion
    @Expose
    var upsideDownParkour: UpsideDownParkourConfig = UpsideDownParkourConfig()

    // Red-Green Puzzle
    @ConfigOption(name = "Dance Room Helper", desc = "")
    @Accordion
    @Expose
    var danceRoomHelper: DanceRoomHelperConfig = DanceRoomHelperConfig()

    @ConfigOption(name = "Tubulator", desc = "")
    @Accordion
    @Expose
    var tubulatorConfig: TubulatorConfig = TubulatorConfig()
}
