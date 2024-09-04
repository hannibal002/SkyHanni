package at.hannibal2.skyhanni.data.model

enum class GraphNodeCategory(val internalName: String?, val displayName: String, val description: String) {
//     NO_CATEGORY(null, "<without category>", "Nodes without category"),
    DEV("dev", "Dev", "Intentionally marked as dev."), // E.g. Spawn points, todos, etc

    // Everywhere
    NPC("npc", "§eNPC", "A NPC entity."), // also take from neu repo
    AREA("area", "§aArea", "A SkyBlock Area."),
    POI("poi", "PoI", "Point of interest."),
    LAUNCH_PAD("launch", "Launch Pad", "Slime blocks sending you to another server."),

    // on multiple islands
    ROMEO("romeo", "Romeo & Juliette Quest", "Blocks related to the Romeo and Juliette/Ring of Love quest line."),
    RACE("race", "Race Start/Stop", "A race start or stop point."),
    SLAYER("slayer", "Slayer", "A Slayer area"),
    // hoppity

    // Hub
    HUB_12_STARTER("starter_npc", "Starter NPC", "One of the 12 starter NPC's you need to talk to."),
    // diana

    // Farming Islands: Pelts
    FARMING_CROP("farming_crop", "Farming Crop", "A spot where you can break crops on farming islands."),

    // Rift
    RIFT_ENIGMA("rift_enigma", "§5Enigma Soul", "Enigma Souls in the rift."),
    RIFT_EYE("rift_eye", "§4Eye", "An Eye in the rift to teleport to."),

    // Spider's Den
    SPIDER_RELIC("SPIDER_RELIC", "§5Relic", "An relic in the Spider's Den."),

    // Dwarven Mines
    MINES_EMISSARY("mines_emissary", "§6Emissary", "A Emissary from the king."),
    // commission areas

    ;

    companion object {
        fun byId(internalName: String?): GraphNodeCategory? = values().firstOrNull { it.internalName == internalName }
    }
}
