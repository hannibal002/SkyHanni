package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.data.jsonobjects.local.MayorJson

enum class Mayors(val mayorName: String, val color: String, val perks: MutableList<Perks>) {
    AATROX("Aatrox", "§3", mutableListOf()),
    COLE("Cole", "§e", mutableListOf()),
    DIANA("Diana", "§2", mutableListOf()),
    DIAZ("Diaz", "§6", mutableListOf()),
    FINNEGAN("Finnegan", "§c", mutableListOf()),
    FOXY("Foxy", "§d", mutableListOf()),
    MARINA("Marina", "§b", mutableListOf()),
    PAUL("Paul", "§c", mutableListOf()),

    SCORPIUS("Scorpius", "§d", mutableListOf()),
    JERRY("Jerry", "§d", mutableListOf()),
    DERPY("Derpy", "§d", mutableListOf()),

    UNKNOWN("Unknown", "§c", mutableListOf()),
    ;


    companion object {
        fun getMayorFromName(name: String) = entries.firstOrNull { it.mayorName == name } ?: UNKNOWN

        fun getMayorFromName(name: String, perks: ArrayList<MayorJson.Perk>): Mayors {
            val mayor = getMayorFromName(name)
            perks.forEach {
                mayor.perks.add(Perks.valueOf(it.name))
            }
            return mayor
        }
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

    fun isActive() = MayorAPI.currentMayor?.perks?.contains(this) ?: false
}
