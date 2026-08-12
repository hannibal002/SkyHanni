package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

/**
 * Currencies that can appear in an item cost lore, but are not real items in the NEU repo.
 *
 * [coinValue] is the worth of a single unit in coins, or null when unknown.
 * [loreNames] are the names as written in the lore, lowercase and without color codes.
 */
enum class SkyblockCurrency(
    val internalName: NeuInternalName,
    val displayName: String,
    val coinValue: Double? = null,
    private val loreNames: Set<String>,
) {
    // Universal
    COINS(
        NeuInternalName.SKYBLOCK_COIN,
        "§6Coins",
        coinValue = 1.0,
        loreNames = setOf("coin", "coins", "skyblock coin", "skyblock coins", "skyblock_coin", "skyblock_coins"),
    ),

    // Bits Shop from Elisabeth
    BITS("BITS".toInternalName(), "§bBits", loreNames = setOf("bit", "bits")),

    // Pesthunter's Wares in Garden
    PESTS("PESTS".toInternalName(), "§2Pests", loreNames = setOf("pest", "pests")),

    // Chocolate Factory
    CHOCOLATE("CHOCOLATE".toInternalName(), "§6Chocolate", loreNames = setOf("chocolate")),

    // Anita and SkyMart in Garden
    COPPER("COPPER".toInternalName(), "§cCopper", loreNames = setOf("copper")),
    GOLD_MEDAL("GOLD_MEDAL".toInternalName(), "§6Gold medal", loreNames = setOf("gold medal", "gold medals")),
    SILVER_MEDAL("SILVER_MEDAL".toInternalName(), "§fSilver medal", loreNames = setOf("silver medal", "silver medals")),
    BRONZE_MEDAL("BRONZE_MEDAL".toInternalName(), "§cBronze medal", loreNames = setOf("bronze medal", "bronze medals")),
    ;

    @SkyHanniModule
    companion object {

        private val byInternalName by lazy { entries.associateBy { it.internalName } }

        fun getByInternalNameOrNull(internalName: NeuInternalName): SkyblockCurrency? = byInternalName[internalName]

        fun getByLoreNameOrNull(name: String): SkyblockCurrency? {
            val clean = name.removeColor().lowercase()
            return entries.firstOrNull { clean in it.loreNames }
        }

        /**
         * A currency id that is also a real repo item would lose its price,
         * since [ItemPriceUtils.getPriceOrNull] answers from this enum first.
         */
        @HandleEvent
        private fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
            val conflicts = entries.filter { it.internalName.getItemStackOrNull() != null }
            if (conflicts.isEmpty()) return

            ErrorManager.logErrorStateWithData(
                "A SkyHanni currency id is also a real item, please report this in discord",
                "SkyblockCurrency internal names collide with NEU repo items",
                "conflicts" to conflicts.map { it.internalName.asString() },
            )
        }
    }
}
