package at.hannibal2.skyhanni.features.misc.discordrpc

import com.google.common.collect.Sets
import java.util.*

enum class LocationRPC {
    ISLAND("Your Island"),  // No RPC
    GUEST_ISLAND("'s Island", "island"),  // No RPC

    // Hub
    AUCTION_HOUSE("Auction House"), BANK("Bank"), BAZAAR("Bazaar Alley"),  // No RPC
    CANVAS_ROOM("Canvas Room"), COAL_MINE("Coal Mine"), COLOSSEUM("Colosseum"), COLOSSEUM_ARENA(
        "Colosseum Arena",
        "colosseum"
    ),
    DUEL_ZONE(
        "Duel Zone",
        "colosseum"
    ),
    ELECTION_ROOM("Election Room"), FARM("Farm"), FASHION_SHOP("Fashion Shop"), FISHERMANS_HUT("Fisherman's Hut"), FLOWER_HOUSE(
        "Flower House"
    ),
    FOREST("Forest"), GRAVEYARD("Graveyard"), HIGH_LEVEL("High Level"), LIBRARY("Library"), MOUNTAIN("Mountain"), RUINS(
        "Ruins"
    ),
    TAVERN("Tavern"), VILLAGE("Village"), WILDERNESS("Wilderness"), WIZARD_TOWER("Wizard Tower"), CATACOMBS_ENTRANCE("Catacombs Entrance"),  // The Park
    BIRCH_PARK("Birch Park"), SPRUCE_WOODS("Spruce Woods"), SAVANNA_WOODLAND("Savanna Woodland"), MELODYS_PLATEAU("Melody's Plateau"), DARK_THICKET(
        "Dark Thicket"
    ),
    JUNGLE_ISLAND("Jungle Island"), HOWLING_CAVE("Howling Cave"), GOLD_MINE("Gold Mine"),  // Deep Caverns
    DEEP_CAVERNS("Deep Caverns"),  // No RPC
    GUNPOWDER_MINES("Gunpowder Mines"),  // No RPC
    LAPIS_QUARRY("Lapis Quarry"),  // No RPC
    PIGMAN_DEN("Pigmen's Den"),  // No RPC
    SLIMEHILL("Slimehill"), DIAMOND_RESERVE("Diamond Reserve"), OBSIDIAN_SANCTUARY("Obsidian Sanctuary"),  // Dwarven mines
    DWARVEN_MINES("Dwarven Mines"), DWARVEN_VILLAGE("Dwarven Village"), GATES_TO_THE_MINES("Gates to the Mines"), THE_LIFT(
        "The Lift"
    ),
    THE_FORGE("The Forge"), FORGE_BASIN("Forge Basin"), LAVA_SPRINGS("Lava Springs"), PALACE_BRIDGE("Palace Bridge"), ROYAL_PALACE(
        "Royal Palace"
    ),
    ARISTOCRAT_PASSAGE("Aristocrat Passage"), HANGING_TERRACE("Hanging Terrace"), CLIFFSIDE_VEINS("Cliffside Veins"), RAMPARTS_QUARRY(
        "Rampart's Quarry"
    ),
    DIVANS_GATEWAY("Divan's Gateway"), FAR_RESERVE("Far Reserve"), GOBLIN_BURROWS("Goblin Burrows"), UPPER_MINES("Upper Mines"), ROYAL_MINES(
        "Royal Mines"
    ),
    MINERS_GUILD("Miner's Guild"), GREAT_ICE_WALL("Great Ice Wall"), THE_MIST("The Mist"), CC_MINECARTS_CO("C&C Minecarts Co."), GRAND_LIBRARY(
        "Grand Library"
    ),
    HANGING_COURT("Hanging Court"),  // Crystal Hollows
    CRYSTAL_HOLLOWS("Crystal Hollows"), CRYSTAL_NUCLEUS("Crystal Nucleus"), MAGMA_FIELDS("Magma Fields"), JUNGLE("Jungle"), MITHRIL_DEPOSITS(
        "Mithril Deposits"
    ),
    GOBLIN_HOLDOUT("Goblin Holdout"), PRECURSOR_REMNANT("Precursor Remnants"), FAIRY_GROTTO("Fairy Grotto"), KHAZAD_DUM(
        "Khazad-dûm"
    ),  //These are the random gen places in each biome
    JUNGLE_TEMPLE("Jungle Temple"), MINES_OF_DIVAN("Mines of Divan"), GOBLIN_QUEEN_DEN("Goblin Queens Den"), LOST_PRECURSOR_CITY(
        "Lost Precursor City"
    ),
    THE_BARN("The Barn"),  // Mushroom Island
    MUSHROOM_DESERT("Mushroom Desert"), DESERT_SETTLEMENT("Desert Settlement"), TREASURE_HUNTER_CAMP("Treasure Hunter Camp"), OASIS(
        "Oasis"
    ),
    MUSHROOM_GORGE("Mushroom Gorge"), GLOWING_MUSHROOM_CAVE("Glowing Mushroom Cave"), OVERGROWN_MUSHROOM_CAVE("Overgrown Mushroom Cave"), JAKES_HOUSE(
        "Jake's House"
    ),
    SHEPHERDS_KEEP("Shepherds Keep"), TRAPPERS_DEN("Trappers Den"), SPIDERS_DEN("Spider's Den"), BLAZING_FORTRESS("Blazing Fortress"),  // The End
    THE_END("The End"), DRAGONS_NEST("Dragon's Nest"), VOID_SEPULTURE("Void Sepulture"), ZEALOT_BRUISER_HIDEOUT("Zealot Bruiser Hideout"), VOID_SLATE(
        "Void Slate"
    ),  // Jerry's workshop
    JERRY_POND("Jerry Pond"),  // No RPC
    JERRYS_WORKSHOP("Jerry's Workshop"),  // No RPC

    // Dungeons
    THE_CATACOMBS("The Catacombs"),  // No RPC
    DUNGEON_HUB("Dungeon Hub"),  // No RPC

    // Crimson Isle
    CRIMSON_ISLE("Crimson Isle"), CRIMSON_FIELDS("Crimson Fields"), CATHEDRAL("Cathedral"), BARBARIAN_OUTPOST("Barbarian Outpost"), MAGE_OUTPOST(
        "Mage Outpost"
    ),
    THE_BASTION("The Bastion"), BLAZING_VOLCANO("Blazing Volcano"), BURNING_DESERT("Burning Desert"), DOJO("Dojo"), DRAGONTAIL(
        "Dragontail"
    ),
    DRAGONTAIL_TOWNSQUARE("Dragontail Townsquare"), DRAGONTAIL_AUCTION_HOUS("Dragontail AH"), DRAGONTAIL_BAZAAR("Dragontail BZ"), DRAGONTAIL_BANK(
        "Dragontail Bank"
    ),
    DRAGONTAIL_BLACKSMITH("Dragontail Blacksmith"), MINION_SHOP("Minion Shop"), CHIEFS_HUT("Chief's Hut"), FORGOTTEN_SKULL(
        "Forgotten Skull"
    ),
    KUUDRAS_END("Kuudra's End"), MAGMA_CHAMBER("Magma Chamber"), MYSTIC_MARSH("Mystic Marsh"), ODGERS_HUT("Odger's Hut"), RUINS_OF_ASHFANG(
        "Ruins of Ashfang"
    ),
    SCARLETON("Scarleton"), SCARLETON_PLAZA("Scarleton Plaza"), SCARLETON_AUCTION_HOUS("Scarleton AH"), SCARLETON_BAZAAR(
        "Scarleton BZ"
    ),
    SCARLETON_BANK("Scarleton Bank"), SCARLETON_BLACKSMITH("Scarleton Blacksmith"), SCARLETON_MINION_SHOP("Scarleton Minion Shop"), STRONGHOLD(
        "Stronghold"
    ),
    THE_WASTELAND("The Wasteland"), MATRIARCHS_LAIR("Matriarch's Lair"), BELLY_OF_THE_BEAST("Belly of the Beast"), AURAS_LAB(
        "Aura's Lab"
    ),
    COURTYARD("Courtyard"), IGRUPANS_CHICKEN_COOP("Igrupan's Chicken Coop"), THRONE_ROOM("Throne Room"), MAGE_COUNCIL("Mage Council"), PLHLEGBLAST_POOL(
        "Plhlegblast Pool"
    ),
    SMOLDERING_TOMB("Smoldering Tomb"),  /*
    Out of Bounds
    This is a valid location in Skyblock, it isn't a placeholder or a made up location.
    It actually displays when the player is out of bounds.
     */
    NONE("None"),  // This is used when the mod is unable to retrieve the player's location from the sidebar.
    UNKNOWN("Unknown");

    /**
     * The name of this location as shown on the in-game scoreboard.
     */
    private val scoreboardName: String
    private val discordIconKey: String

    constructor(scoreboardName: String, discordIconKey: String) {
        this.scoreboardName = scoreboardName
        this.discordIconKey = discordIconKey
    }

    constructor(scoreboardName: String) {
        this.scoreboardName = scoreboardName
        val noDiscordRPC: Set<String> = Sets.newHashSet(
            "ISLAND",
            "BAZAAR",
            "DEEP_CAVERNS",
            "GUNPOWDER_MINES",
            "LAPIS_QUARRY",
            "PIGMAN_DEN",
            "JERRYS_WORKSHOP",
            "JERRY_POND",
            "DWARVEN_MINES",
            "DWARVEN_VILLAGE",
            "GATES_TO_THE_MINES",
            "THE_LIFT",
            "THE_FORGE",
            "FORGE_BASIN",
            "LAVA_SPRINGS",
            "PALACE_BRIDGE",
            "ROYAL_PALACE",
            "ARISTOCRAT_PASSAGE",
            "HANGING_TERRACE",
            "CLIFFSIDE_VEINS",
            "RAMPARTS_QUARRY",
            "DIVANS_GATEWAY",
            "FAR_RESERVE",
            "GOBLIN_BURROWs",
            "UPPER_MINES",
            "MINERS_GUILD",
            "GREAT_ICE_WALL",
            "THE_MIST",
            "CC_MINECARTS_CO",
            "GRAND_LIBRARY",
            "HANGING_COURT",
            "ROYAL_MINES",
            "DESERT_SETTLEMENT",
            "TREASURE_HUNTER_CAMP",
            "OASIS",
            "MUSHROOM_GORGE",
            "GLOWING_MUSHROOM_CAVE",
            "OVERGROWN_MUSHROOM_CAVE",
            "JAKES_HOUSE",
            "SHEPHERDS_KEEP",
            "TRAPPERS_DEN",
            "DOJO",
            "BURNING_DESERT",
            "BLAZING_VOLCANO",
            "THE_BASTION",
            "MAGE_OUTPOST",
            "BARBARIAN_OUTPOST",
            "CATHEDRAL",
            "CRIMSON_FIELDS",
            "CRIMSON_ISLE",
            "MINION_SHOP",
            "DRAGONTAIL_BLACKSMITH",
            "DRAGONTAIL_BANK",
            "DRAGONTAIL_BAZAAR",
            "DRAGONTAIL_AUCTION_HOUS",
            "DRAGONTAIL_TOWNSQUARE",
            "DRAGONTAIL",
            "RUINS_OF_ASHFANG",
            "ODGERS_HUT",
            "MAGMA_CHAMBER",
            "KUUDRAS_END",
            "FORGOTTEN_SKULL",
            "CHIEFS_HUT",
            "STRONGHOLD",
            "SCARLETON_MINION_SHOP",
            "SCARLETON_BLACKSMITH",
            "SCARLETON_BANK",
            "SCARLETON_BAZAAR",
            "SCARLETON_PLAZA",
            "SCARLETON",
            "PLHLEGBLAST_POOL",
            "MAGE_COUNCIL",
            "THRONE_ROOM",
            "IGRUPANS_CHICKEN_COOP",
            "COURTYARD",
            "AURAS_LAB",
            "BELLY_OF_THE_BEAST",
            "MATRIARCHS_LAIR",
            "SMOLDERING_TOMB",
            "MYSTIC_MARSH",
            "SCARLETON_AUCTION_HOUS",
            "THE_WASTELAND"
        )
        discordIconKey = if (noDiscordRPC.contains(name)) {
            "skyblock"
        } else {
            name.lowercase(Locale.getDefault()).replace("_", "-")
        }
    }

    companion object {
        fun getDiscordIconKey(scoreboardName: String): String {
            for (enum in values()) {
                if (enum.scoreboardName ==  scoreboardName) {
                    return enum.discordIconKey
                }
            }
            return UNKNOWN.discordIconKey
        }
    }
}