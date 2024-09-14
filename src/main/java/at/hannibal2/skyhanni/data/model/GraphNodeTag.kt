package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.LorenzColor

enum class GraphNodeTag(
    val internalName: String,
    val color: LorenzColor,
    val cleanName: String,
    val description: String,
    val onlyIsland: IslandType? = null,
) {
    DEV("dev", LorenzColor.WHITE, "Dev", "Intentionally marked as dev."), // E.g. Spawn points, todos, etc

    // Everywhere
    NPC("npc", LorenzColor.YELLOW, "NPC", "A NPC entity."), // also take from neu repo
    AREA("area", LorenzColor.DARK_GREEN, "Area", "A big SkyBlock area."),
    SMALL_AREA("small_area", LorenzColor.GREEN, "Small Area", "A small SkyBlock area, e.g. a house."),
    POI("poi", LorenzColor.WHITE, "PoI", "Point of interest."),
    LAUNCH_PAD("launch", LorenzColor.WHITE, "Launch Pad", "Slime blocks sending you to another server."),

    // on multiple islands
    ROMEO("romeo", LorenzColor.WHITE, "Romeo & Juliette Quest", "Blocks related to the Romeo and Juliette/Ring of Love quest line."),
    RACE("race", LorenzColor.WHITE, "Race Start/Stop", "A race start or stop point."),
    SLAYER("slayer", LorenzColor.WHITE, "Slayer", "A Slayer area"),
    HOPPITY("hoppity", LorenzColor.AQUA, "Hoppity Egg", "An egg location in Hoppity's Hunt."),
    // hoppity

    // Hub
    HUB_12_STARTER(
        "starter_npc", LorenzColor.WHITE, "Starter NPC", "One of the 12 starter NPC's you need to talk to.",
        onlyIsland = IslandType.HUB,
    ),
    // diana

    // Farming Islands: Pelts
//     FARMING_CROP("farming_crop", LorenzColor.WHITE, "Farming Crop", "A spot where you can break crops on farming islands."),

    // Rift
    RIFT_ENIGMA("rift_enigma", LorenzColor.DARK_PURPLE, "Enigma Soul", "Enigma Souls in the rift.", onlyIsland = IslandType.THE_RIFT),
    RIFT_EYE("rift_eye", LorenzColor.DARK_RED, "Rift Eye", "An Eye in the rift to teleport to.", onlyIsland = IslandType.THE_RIFT),
    RIFT_MONTEZUMA("rift_montezuma", LorenzColor.GRAY, "Montezuma Soul Piece", "A piece of the Montezuma Soul.", onlyIsland = IslandType.THE_RIFT),
    RIFT_EFFIGY("rift_effigy", LorenzColor.RED, "Blood Effigies", "Locations of the Blood Effigies.", onlyIsland = IslandType.THE_RIFT),

    // Spider's Den
    SPIDER_RELIC("SPIDER_RELIC", LorenzColor.DARK_PURPLE, "Spider's Relic", "An relic in the Spider's Den.", onlyIsland = IslandType.SPIDER_DEN),

    // Dwarven Mines
    MINES_EMISSARY("mines_emissary", LorenzColor.GOLD, "Mines Emissary", "A Emissary from the king.", onlyIsland = IslandType.DWARVEN_MINES),
    // commission areas

    // Crimson Isles
    CRIMSON_MINIBOSS("crimson_miniboss", LorenzColor.RED, "Crimson Miniboss", "Mini bosses in Crimson Isle.", onlyIsland = IslandType.CRIMSON_ISLE),

    ;

    val displayName: String = color.getChatColor() + cleanName

    companion object {
        fun byId(internalName: String?): GraphNodeTag? = entries.firstOrNull { it.internalName == internalName }
    }
}
