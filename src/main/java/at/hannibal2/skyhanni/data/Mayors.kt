package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.data.MayorAPI.foxyExtraEventPattern
import at.hannibal2.skyhanni.data.jsonobjects.other.MayorPerk
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher

enum class Mayor(
    val mayorName: String,
    val color: String,
    vararg val perks: Perk,
) {
    AATROX("Aatrox", "§3", Perk.SLASHED_PRICING, Perk.SLAYER_XP_BUFF, Perk.PATHFINDER),
    COLE("Cole", "§e", Perk.PROSPECTION, Perk.MINING_XP_BUFF, Perk.MINING_FIESTA),
    DIANA("Diana", "§2", Perk.LUCKY, Perk.MYTHOLOGICAL_RITUAL, Perk.PET_XP_BUFF),
    DIAZ("Diaz", "§6", Perk.BARRIER_STREET, Perk.SHOPPING_SPREE),
    FINNEGAN("Finnegan", "§c", Perk.FARMING_SIMULATOR, Perk.PELT_POCALYPSE, Perk.GOATED),
    FOXY(
        "Foxy",
        "§d",
        Perk.SWEET_TOOTH,
        Perk.BENEVOLENCE,
        Perk.EXTRA_EVENT_MINING,
        Perk.EXTRA_EVENT_FISHING,
        Perk.EXTRA_EVENT_SPOOKY
    ),
    MARINA("Marina", "§b", Perk.FISHING_XP_BUFF, Perk.LUCK_OF_THE_SEA, Perk.FISHING_FESTIVAL),
    PAUL("Paul", "§c", Perk.MARAUDER, Perk.EZPZ, Perk.BENEDICTION),

    SCORPIUS("Scorpius", "§d", Perk.BRIBE, Perk.DARKER_AUCTIONS),
    JERRY("Jerry", "§d", Perk.PERKPOCALYPSE, Perk.STATSPOCALYPSE, Perk.JERRYPOCALYPSE),
    DERPY("Derpy", "§d", Perk.TURBO_MINIONS, Perk.AH_CLOSED, Perk.DOUBLE_MOBS_HP, Perk.MOAR_SKILLZ),

    UNKNOWN("Unknown", "§c"),
    DISABLED("§cDisabled", "§7"),
    ;

    val activePerks: MutableList<Perk> = mutableListOf()

    override fun toString() = mayorName

    companion object {

        fun getMayorFromName(name: String): Mayor? = entries.firstOrNull { it.mayorName == name }

        fun setAssumeMayorJson(name: String, perksJson: List<MayorPerk>): Mayor? {
            val mayor = getMayorFromName(name)
            if (mayor == null) {
                ErrorManager.logErrorStateWithData(
                    "Unknown mayor found",
                    "mayor name not in Mayor enum",
                    "name" to name,
                    "perksJson" to perksJson,
                    betaOnly = true
                )
                return null
            }
            val perks = perksJson.mapNotNull { perk ->
                Perk.entries.firstOrNull { it.perkName == perk.renameIfFoxyExtraEventPerkFound() }
            }

            mayor.setAssumeMayor(perks)
            return mayor
        }

        fun Mayor.setAssumeMayor(perks: List<Perk>) {
            perks.forEach { it.isActive = false }
            activePerks.clear()
            perks.filter { perks.contains(it) }.forEach {
                it.isActive = true
                activePerks.add(it)
            }
        }

        private fun MayorPerk.renameIfFoxyExtraEventPerkFound(): String? {
            val foxyExtraEventPairs = mapOf(
                "Spooky Festival" to "Extra Event (Spooky)",
                "Mining Fiesta" to "Extra Event (Mining)",
                "Fishing Festival" to "Extra Event (Fishing)"
            )

            foxyExtraEventPattern.matchMatcher(this.description) {
                return foxyExtraEventPairs.entries.firstOrNull { it.key == group("event") }?.value
            }
            return this.name
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
    EXTRA_EVENT_MINING("Extra Event (Mining)"),
    EXTRA_EVENT_FISHING("Extra Event (Fishing)"),
    EXTRA_EVENT_SPOOKY("Extra Event (Spooky)"),

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
