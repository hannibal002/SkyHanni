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
    val lavaMazeConfig: LavaMazeConfig = LavaMazeConfig()

    @ConfigOption(name = "Crafting Room", desc = "")
    @Accordion
    @Expose
    val craftingRoom: CraftingRoomConfig = CraftingRoomConfig()

    @ConfigOption(name = "Upside-Down Parkour", desc = "")
    @Accordion
    @Expose
    val upsideDownParkour: UpsideDownParkourConfig = UpsideDownParkourConfig()

    // Red-Green Puzzle
    @ConfigOption(name = "Dance Room Helper", desc = "")
    @Accordion
    @Expose
    val danceRoomHelper: DanceRoomHelperConfig = DanceRoomHelperConfig()

    @ConfigOption(name = "Tubulator", desc = "")
    @Accordion
    @Expose
    val tubulatorConfig: TubulatorConfig = TubulatorConfig()
}
