package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.data.jsonobjects.local.MayorJson

enum class Mayor(
    val mayorName: String,
    val color: String,
    private vararg val perks: Perk,
) {
    AATROX("Aatrox", "§3", Perk.SLASHED_PRICING, Perk.SLAYER_XP_BUFF, Perk.PATHFINDER),
    COLE("Cole", "§e", Perk.PROSPECTION, Perk.MINING_XP_BUFF, Perk.MINING_FIESTA),
    DIANA("Diana", "§2", Perk.LUCKY, Perk.MYTHOLOGICAL_RITUAL, Perk.PET_XP_BUFF),
    DIAZ("Diaz", "§6", Perk.BARRIER_STREET, Perk.SHOPPING_SPREE),
    FINNEGAN("Finnegan", "§c", Perk.FARMING_SIMULATOR, Perk.PELT_POCALYPSE, Perk.GOATED),
    FOXY("Foxy", "§d", Perk.SWEET_TOOTH, Perk.BENEVOLENCE, Perk.EXTRA_EVENT),
    MARINA("Marina", "§b", Perk.FISHING_XP_BUFF, Perk.LUCK_OF_THE_SEA, Perk.FISHING_FESTIVAL),
    PAUL("Paul", "§c", Perk.MARAUDER, Perk.EZPZ, Perk.BENEDICTION),

    SCORPIUS("Scorpius", "§d", Perk.BRIBE, Perk.DARKER_AUCTIONS),
    JERRY("Jerry", "§d", Perk.PERKPOCALYPSE, Perk.STATSPOCALYPSE, Perk.JERRYPOCALYPSE),
    DERPY("Derpy", "§d", Perk.TURBO_MINIONS, Perk.AH_CLOSED, Perk.DOUBLE_MOBS_HP, Perk.MOAR_SKILLZ),

    UNKNOWN("Unknown", "§c"),
    ;

    val activePerks: MutableList<Perk> = mutableListOf()

    companion object {
        fun getMayorFromName(name: String) = entries.firstOrNull { it.mayorName == name } ?: UNKNOWN

        fun setMayorWithActivePerks(name: String, perks: ArrayList<MayorJson.Perk>): Mayor {
            val mayor = getMayorFromName(name)

            mayor.perks.forEach { it.isActive = false }
            mayor.activePerks.clear()
            perks.mapNotNull { perk -> Perk.entries.firstOrNull { it.perkName == perk.name } }
                .filter { mayor.perks.contains(it) }.forEach {
                    it.isActive = true
                    mayor.activePerks.add(it)
                }

            return mayor
        }
    }
}

enum class Perk(val perkName: String) {
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

    var isActive = false
}
