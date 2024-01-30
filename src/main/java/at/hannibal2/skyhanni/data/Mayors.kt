package at.hannibal2.skyhanni.data

enum class Mayors(val mayorName: String, val color: String, val perk: List<Perks>) {
    AATROX("Aatrox", "§3", listOf(Perks.SLASHED_PRICING, Perks.SLAYER_XP_BUFF, Perks.PATHFINDER)),
    COLE("Cole", "§e", listOf(Perks.PROSPECTION, Perks.MINING_XP_BUFF, Perks.MINING_FIESTA)),
    DIANA("Diana", "§2", listOf(Perks.LUCKY, Perks.MYTHOLOGICAL_RITUAL, Perks.PET_XP_BUFF)),
    DIAZ("Diaz", "§6", listOf(Perks.BARRIER_STREET, Perks.SHOPPING_SPREE)),
    FINNEGAN("Finnegan", "§c", listOf(Perks.FARMING_SIMULATOR, Perks.PELT_POCALYPSE, Perks.GOATED)),
    FOXY("Foxy", "§d", listOf(Perks.SWEET_TOOTH, Perks.BENEVOLENCE, Perks.EXTRA_EVENT)),
    MARINA("Marina", "§b", listOf(Perks.FISHING_XP_BUFF, Perks.LUCK_OF_THE_SEA, Perks.FISHING_FESTIVAL)),
    PAUL("Paul", "§c", listOf(Perks.MARAUDER, Perks.EZPZ, Perks.BENEDICTION)),

    SCORPIUS("Scorpius", "§d", listOf(Perks.BRIBE, Perks.DARKER_AUCTIONS)),
    JERRY("Jerry", "§d", listOf(Perks.PERKPOCALYPSE, Perks.STATSPOCALYPSE, Perks.JERRYPOCALYPSE)),
    DERPY("Derpy", "§d", listOf(Perks.TURBO_MINIONS, Perks.AH_CLOSED, Perks.DOUBLE_MOBS_HP, Perks.MOAR_SKILLZ)),

    UNKNOWN("Unknown", "§c", listOf()),
    ;


    companion object {
        fun getMayorFromName(name: String) = entries.firstOrNull { it.mayorName == name } ?: UNKNOWN
    }
}

enum class Perks(val perkName: String) {
    // Aatrox
    SLASHED_PRICING("SLASHED Pricing"),
    SLAYER_XP_BUFF("Slayer XP Buff"),
    PATHFINDER("Pathfinder"),

    // Cole
    PROSPECTION("Prospection"),
    MINING_XP_BUFF("Mining XP Buff"),
    MINING_FIESTA("Mining Fiesta"),

    // Diana
    LUCKY("Lucky!"),
    MYTHOLOGICAL_RITUAL("Mythological Ritual"),
    PET_XP_BUFF("Pet XP Buff"),

    // Diaz
    BARRIER_STREET("Barrier Street"),
    SHOPPING_SPREE("Shopping Spree"),

    // Finnegan
    FARMING_SIMULATOR("Farming Simulator"),
    PELT_POCALYPSE("Pelt-pocalypse"),
    GOATED("GOATed"),

    // Foxy
    SWEET_TOOTH("Sweet Tooth"),
    BENEVOLENCE("Benevolence"),
    EXTRA_EVENT("Extra Event"),

    // Marina
    FISHING_XP_BUFF("Fishing XP Buff"),
    LUCK_OF_THE_SEA("Luck of the Sea 2.0"),
    FISHING_FESTIVAL("Fishing Festival"),

    // Paul
    MARAUDER("Marauder"),
    EZPZ("EZPZ"),
    BENEDICTION("Benediction"),


    // Scorpius
    BRIBE("Bribe"),
    DARKER_AUCTIONS("Darker Auctions"),

    // Jerry
    PERKPOCALYPSE("Perkpocalypse"),
    STATSPOCALYPSE("Statspocalypse"),
    JERRYPOCALYPSE("Jerrypocalypse"),

    // Derpy
    TURBO_MINIONS("TURBO MINIONS!!!"),
    AH_CLOSED("AH CLOSED!!!"),
    DOUBLE_MOBS_HP("DOUBLE MOBS HP!!!"),
    MOAR_SKILLZ("MOAR SKILLZ!!!"),
    ;
}
