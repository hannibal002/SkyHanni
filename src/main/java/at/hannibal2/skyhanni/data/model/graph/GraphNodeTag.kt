package at.hannibal2.skyhanni.data.model.graph

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.utils.LorenzColor

enum class GraphNodeTag(
    val internalName: String,
    val color: LorenzColor,
    val cleanName: String,
    val description: String,
    val onlyIsland: IslandType? = null,
    // only show the node tag in the graph editor when on those islands. this is just a debug visual thing, no user impact
    val onlyIslands: IslandTypeTag? = null,
    val onlySkyblock: Boolean? = true,
) {
    DEV("dev", LorenzColor.WHITE, "Dev", "Intentionally marked as dev.", onlySkyblock = null), // E.g. Spawn points, todos, etc

    // Everywhere
    NPC("npc", LorenzColor.YELLOW, "NPC", "An NPC to talk to.", onlySkyblock = null),
    AREA("area", LorenzColor.DARK_GREEN, "Area", "A big SkyBlock area.", onlySkyblock = null),
    SMALL_AREA("small_area", LorenzColor.GREEN, "Small Area", "A small SkyBlock area, e.g. a house.", onlySkyblock = null),
    POI("poi", LorenzColor.WHITE, "Point of Interest", "A relevant spot or a landmark on the map.", onlySkyblock = null),

    // TODO delete once no graph data contains this anymore
    @Deprecated("gets split up into WARP, JUMP_PAD and TELEPORT_PAD")
    TELEPORT(
        "teleport",
        LorenzColor.BLUE,
        "Teleport",
        "A teleportation spot. Deprecated, split into warp, jump pad, and teleport pad.",
        onlySkyblock = null,
    ),

    // teleports from any island to this location on this island
    WARP("warp", LorenzColor.BLUE, "Teleport Destination", "A teleport command destination.", onlySkyblock = null),

    // stand there, jumps to another island, name is the other island (enum name, not display name)
    JUMP_PAD(
        "jump_pad",
        LorenzColor.BLUE,
        "Teleport To Other Island",
        "A jump pad, portal, or similar mechanic that teleports to another island.",
        onlySkyblock = null,
    ),

    // teleports around on the same island
    TELEPORT_PAD(
        "teleport_pad",
        LorenzColor.BLUE,
        "Teleport To Same Island",
        "A teleport pad that teleports within the same island.",
        onlySkyblock = null,
    ),

    // on multiple islands
    ROMEO("romeo", LorenzColor.WHITE, "Romeo & Juliette Quest", "Spots related to the Romeo and Juliette/Ring of Love quest line."),
    RACE("race", LorenzColor.WHITE, "Race Start/Stop", "A race start or stop point."),
    SLAYER("slayer", LorenzColor.RED, "Slayer", "A Slayer area."),
    HOPPITY("hoppity", LorenzColor.AQUA, "Hoppity Egg", "An egg location in Hoppity's Hunt."),
    GRIND_MOBS("grind_mobs", LorenzColor.RED, "Mob Spawn Area", "An area where mobs spawn that can be killed."),
    GRIND_ORES("grind_ores", LorenzColor.DARK_AQUA, "Ore Vein", "A regenerating ore vein that can be mined."),
    GRIND_CROPS("grind_crops", LorenzColor.DARK_GREEN, "Crop Area", "An area where crops grow that can be farmed."),
    // hoppity

    // diana

    // Farming Islands: Pelts
//     FARMING_CROP("farming_crop", LorenzColor.WHITE, "Farming Crop", "A spot where you can break crops on farming islands."),

    // Rift
    RIFT_ENIGMA("rift_enigma", LorenzColor.DARK_PURPLE, "Enigma Soul", "Enigma Souls in the Rift.", onlyIsland = IslandType.THE_RIFT),
    RIFT_BUTTONS_QUEST(
        "rift_buttons_quest",
        LorenzColor.LIGHT_PURPLE,
        "Wooden Buttons",
        "A spot to hit wooden buttons for the Dreadfarm Enigma Soul.",
        onlyIsland = IslandType.THE_RIFT,
    ),
    RIFT_EYE("rift_eye", LorenzColor.DARK_RED, "Rift Eye", "An Eye in the Rift to teleport to.", onlyIsland = IslandType.THE_RIFT),
    RIFT_MONTEZUMA(
        "rift_montezuma",
        LorenzColor.GRAY,
        "Montezuma Soul Piece",
        "A piece of the Montezuma Soul.",
        onlyIsland = IslandType.THE_RIFT,
    ),
    RIFT_EFFIGY("rift_effigy", LorenzColor.RED, "Blood Effigies", "Locations of the Blood Effigies.", onlyIsland = IslandType.THE_RIFT),

    // Spider's Den
    SPIDER_RELIC(
        "SPIDER_RELIC",
        LorenzColor.DARK_PURPLE,
        "Spider's Relic",
        "A relic in the Spider's Den.",
        onlyIsland = IslandType.SPIDER_DEN,
    ),

    // Dwarven Mines
    // always has to also have the npc tag added
    MINES_EMISSARY("mines_emissary", LorenzColor.GOLD, "Mines Emissary", "An Emissary to the king.", onlyIsland = IslandType.DWARVEN_MINES),
    // commission areas

    // Crimson Isles
    CRIMSON_MINIBOSS(
        "crimson_miniboss",
        LorenzColor.RED,
        "Crimson Miniboss",
        "A miniboss in the Crimson Isle.",
        onlyIsland = IslandType.CRIMSON_ISLE,
    ),

    // The End
    END_GOLEM("end_golem", LorenzColor.RED, "Golem Spawn", "A spot where the golem can spawn in the End.", onlyIsland = IslandType.THE_END),

    // lobby event waypoints
    HALLOWEEN_BASKET(
        "event_basket",
        LorenzColor.LIGHT_PURPLE,
        "Basket",
        "A Basket during the Halloween Event.",
        onlySkyblock = false,
    ),

    FISHING_HOTSPOT(
        "fishing_hotspot",
        LorenzColor.AQUA,
        "Fishing Hotspot",
        "A possible hotspot where you can fish.",
        onlyIslands = IslandTypeTag.FISHING_HOTSPOT,
    ),

    FAIRY_SOUL(
        "fairy_soul",
        LorenzColor.DARK_PURPLE,
        "Fairy Soul",
        "A Fairy Soul.",
    ),

    HIDEONLEAF(
        "hideonleaf",
        LorenzColor.YELLOW,
        "Hideonleaf",
        "The green Shulkers.",
        onlyIsland = IslandType.GALATEA,
    ),

    ;

    val displayName: String = color.getChatColor() + cleanName

    companion object {
        fun byId(internalName: String?): GraphNodeTag? = entries.firstOrNull { it.internalName == internalName }
    }
}
