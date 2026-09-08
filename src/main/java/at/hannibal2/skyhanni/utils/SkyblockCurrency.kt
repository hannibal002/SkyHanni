package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.BitsApi
import at.hannibal2.skyhanni.data.CurrencyApi.getFromStorage
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PurseApi
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.data.ChocolateAmount
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

/**
 * Currencies that can appear in an item cost lore.
 *
 * Entries written as an amount followed by the name, like "5,000 Bits", need this enum to be
 * read at all, since [ItemUtils.readItemAmount] cannot separate the amount from the name there.
 * Entries with the amount behind the name, like "Gold medal §8x2", are already read by the
 * regular item path and are listed here only so that features can refer to them.
 *
 * [internalName] has to be the id of the real repo item whenever the name resolves to one, even
 * if that item has no price, as is the case for [NeuInternalName.SKYBLOCK_COPPER].
 * [displayName] is written without color, use [coloredName] where the color is wanted.
 * [coinValue] is the worth of a single unit in coins, or null when unknown.
 * [loreNames] are the names as written in the lore, lowercase and without color codes.
 */
enum class SkyblockCurrency(
    val internalName: NeuInternalName,
    val displayName: String,
    val color: LorenzColor,
    val coinValue: Double? = null,
    private val loreNames: Set<String>,
    /** Set when [displayName] is the plural, as with "Bits". */
    private val singularName: String? = null,
    /**
     * Set when [displayName] is the singular and its plural is not an s away, as with "Chocolate".
     * Never set together with [singularName], [displayName] always covers the other form.
     */
    private val pluralName: String? = null,
    /** Set when the lore name alone is ambiguous and only unique on one island. */
    private val island: IslandType? = null,
    /** True when the amount belongs to the account instead of the current profile. */
    val accountWide: Boolean = false,
    /**
     * How much of this currency the player owns, or null when SkyHanni does not track it.
     * There is no default on purpose, every currency has to state where its amount comes from.
     */
    private val ownedAmount: SkyblockCurrency.() -> Long?,
) {
    // Universal
    COINS(
        NeuInternalName.SKYBLOCK_COIN,
        "Coins",
        GOLD,
        coinValue = 1.0,
        loreNames = setOf("coin", "coins", "skyblock coin", "skyblock coins", "skyblock_coin", "skyblock_coins"),
        singularName = "Coin",
        ownedAmount = { PurseApi.currentPurse.toLong() },
    ),

    // Bits Shop from Elisabeth
    BITS(
        "BITS".toInternalName(), "Bits", AQUA, loreNames = setOf("bit", "bits"),
        singularName = "Bit",
        accountWide = true,
        ownedAmount = { BitsApi.bits.toLong() },
    ),

    // Pesthunter's Wares in Garden
    PESTS(
        "PESTS".toInternalName(), "Pests", DARK_GREEN, loreNames = setOf("pest", "pests"),
        singularName = "Pest",
        ownedAmount = { getFromStorage() },
    ),

    // Chocolate Factory
    CHOCOLATE(
        "CHOCOLATE".toInternalName(),
        "Chocolate",
        GOLD,
        loreNames = setOf("chocolate"),
        pluralName = "Chocolate",
        ownedAmount = { ChocolateAmount.CURRENT.chocolate() + ChocolateAmount.chocolateSinceUpdate() },
    ),

    // SkyMart in Garden
    COPPER(
        NeuInternalName.SKYBLOCK_COPPER, "Copper", RED, loreNames = setOf("copper"),
        pluralName = "Copper",
        ownedAmount = { getFromStorage() },
    ),

    // Anita in Garden
    GOLD_MEDAL(
        NeuInternalName.SKYBLOCK_GOLD_MEDAL, "Gold medal", GOLD, loreNames = setOf("gold medal", "gold medals"),
        ownedAmount = { getFromStorage() },
    ),
    SILVER_MEDAL(
        NeuInternalName.SKYBLOCK_SILVER_MEDAL, "Silver medal", WHITE, loreNames = setOf("silver medal", "silver medals"),
        ownedAmount = { getFromStorage() },
    ),
    BRONZE_MEDAL(
        NeuInternalName.SKYBLOCK_BRONZE_MEDAL, "Bronze medal", RED, loreNames = setOf("bronze medal", "bronze medals"),
        ownedAmount = { getFromStorage() },
    ),

    // Tony's Shop in the Farming Islands
    PELTS(
        "PELTS".toInternalName(), "Pelts", DARK_PURPLE, loreNames = setOf("pelt", "pelts"),
        singularName = "Pelt",
        ownedAmount = { getFromStorage() },
    ),

    // Cosmetics in various shops
    GEMS(
        "GEMS".toInternalName(), "Gems", GREEN, loreNames = setOf("gem", "gems"),
        singularName = "Gem",
        accountWide = true,
        ownedAmount = { getFromStorage() },
    ),

    // no shop sells for sowdust yet, this only tracks the amount
    SOWDUST(
        "SOWDUST".toInternalName(), "Sowdust", DARK_GREEN, loreNames = setOf("sowdust"),
        pluralName = "Sowdust",
        ownedAmount = { getFromStorage() },
    ),

    // Rift shops
    MOTES(
        NeuInternalName.SKYBLOCK_MOTE, "Mote", LIGHT_PURPLE, loreNames = setOf("mote", "motes"),
        ownedAmount = { getFromStorage() },
    ),

    // the lore only writes "Tokens", the island is what makes it unambiguous
    KUUDRA_TOKEN(
        "KUUDRA_TOKEN".toInternalName(), "Tokens", DARK_PURPLE, loreNames = setOf("token", "tokens"),
        singularName = "Token",
        island = KUUDRA_ARENA,
        ownedAmount = { getFromStorage() },
    ),

    // Ticket Exchange from the Safari Manager, a ticket starts one Critter Safari
    // and the lower tiers are spent to upgrade into the higher ones
    SAFARI_TICKET_BASIC(
        "SAFARI_TICKET_BASIC".toInternalName(), "Basic Safari Ticket", DARK_GREEN,
        loreNames = setOf("basic safari ticket", "basic safari tickets"),
        ownedAmount = { getFromStorage() },
    ),
    SAFARI_TICKET_ECONOMY(
        "SAFARI_TICKET_ECONOMY".toInternalName(), "Economy Safari Ticket", BLUE,
        loreNames = setOf("economy safari ticket", "economy safari tickets"),
        ownedAmount = { getFromStorage() },
    ),
    SAFARI_TICKET_PREMIUM(
        "SAFARI_TICKET_PREMIUM".toInternalName(), "Premium Safari Ticket", DARK_PURPLE,
        loreNames = setOf("premium safari ticket", "premium safari tickets"),
        ownedAmount = { getFromStorage() },
    ),
    SAFARI_TICKET_FIRST_CLASS(
        "SAFARI_TICKET_FIRST_CLASS".toInternalName(), "First-Class Safari Ticket", GOLD,
        loreNames = setOf("first-class safari ticket", "first-class safari tickets"),
        ownedAmount = { getFromStorage() },
    ),

    // Carnival upgrades in the Hub
    CARNIVAL_TOKEN(
        "SKYBLOCK_CARNIVAL_POINT".toInternalName(), "Carnival Token", YELLOW,
        loreNames = setOf("carnival token", "carnival tokens"),
        ownedAmount = { getFromStorage() },
    ),

    // Grand Bakery from Feast Baker Scott in the Hub, earned by donating Seasonings during a Grand Feast
    KERNEL(
        NeuInternalName.SKYBLOCK_KERNEL, "Kernel", GOLD, loreNames = setOf("kernel", "kernels"),
        ownedAmount = { getFromStorage() },
    ),

    // TODO add these currencies, each one needs a real cost line from its shop first
    //  - North Stars, waiting on the winter event
    //  - Bingo Points, waiting on the bingo event
    ;

    val coloredName: String = color.getChatColor() + displayName

    /**
     * Reads the amount from a text that writes it in front of the name, like "5,000 Bits".
     * Returns null for currencies written the other way around, like "Gold medal §8x2".
     */
    fun readAmountOrNull(text: String): Long? = readCurrencyOrNull(text)?.takeIf { it.first == this }?.second

    fun getOwnedAmountOrNull(): Long? = ownedAmount.invoke(this)

    /** False when this currency belongs to an island the player is not on. */
    private fun isAvailable(): Boolean = island?.isInIsland() ?: true

    /** Formats an amount the way it appears in a cost lore, for example "§b5,000 Bits". */
    fun formatAmount(amount: Long): String {
        // displayName is the plural when a singular is stated, otherwise pluralName, and null appends the s
        val plural = if (singularName == null) pluralName else displayName
        return color.getChatColor() + StringUtils.pluralize(amount, singularName ?: displayName, plural, withNumber = true)
    }

    @SkyHanniModule
    companion object {
        /**
         * REGEX-TEST: 5,000 Bits
         * REGEX-TEST: 40 Pests
         * REGEX-TEST: 250 Copper
         * REGEX-TEST: 1,940,000 Coins
         * REGEX-TEST: 29.1 Coins
         * REGEX-TEST: 46,559,892,200 Chocolate
         * REGEX-TEST: 400 Gems
         * REGEX-TEST: 75 Pelts
         */
        private val amountPattern by RepoPattern.pattern(
            "utils.currency.amount",
            "(?<amount>[\\d,.]+) (?<name>[\\w' ]+)",
        )

        fun readCurrencyOrNull(text: String): Pair<SkyblockCurrency, Long>? = amountPattern.matchMatcher(text.removeColor()) {
            val currency = getByLoreNameOrNull(group("name")) ?: return@matchMatcher null
            currency to group("amount").formatLong()
        }

        private val byInternalName by lazy { entries.associateBy { it.internalName } }

        fun getByInternalNameOrNull(internalName: NeuInternalName): SkyblockCurrency? = byInternalName[internalName]

        fun getByLoreNameOrNull(name: String): SkyblockCurrency? {
            val clean = name.removeColor().lowercase()
            return entries.firstOrNull { it.isAvailable() && clean in it.loreNames }
        }

        /**
         * When the display name resolves to a repo item, [ItemNameResolver] answers before this
         * enum is ever reached, so the entry has to use that item's id. An invented id fails
         * silently instead: every comparison against it never matches.
         */
        @HandleEvent
        private fun onNeuRepoReload() {
            // The item name lookup needs NeuItems.allItemsCache, which another handler of this event rebuilds
            // on the next tick. Running at the end of that tick puts this check after the rebuild.
            DelayedRun.runNextTickEnd {
                val conflicts = entries.mapNotNull { currency ->
                    val id = currency.internalName
                    val resolved = ItemNameResolver.getInternalNameOrNull(currency.displayName)
                    when {
                        resolved != null && resolved != id -> "${currency.name} should use ${resolved.asString()}"
                        resolved == null && id.getItemStackOrNull() != null ->
                            "${currency.name} uses ${id.asString()}, which is a different repo item"

                        else -> null
                    }
                }
                if (conflicts.isEmpty()) return@runNextTickEnd

                ErrorManager.logErrorStateWithData(
                    "A SkyHanni currency uses a wrong id, please report this in discord",
                    "SkyblockCurrency entries do not match the repo item behind their name",
                    "conflicts" to conflicts,
                )
            }
        }
    }
}
