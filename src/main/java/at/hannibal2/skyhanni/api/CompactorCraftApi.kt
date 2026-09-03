package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.SackApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventory
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import kotlin.time.Duration

/**
 * Single step crafts as the compactor does them: a fixed amount of one item crafts into another item.
 * Built once per repo reload from the NEU recipes.
 */
@SkyHanniModule
object CompactorCraftApi {

    /** Null until the first repo reload build finished. Replaced as a whole, never field by field. */
    @Volatile
    private var lookup: CraftLookup? = null

    private val repoReloadCoroutine = CoroutineSettings("compactor gfs single step crafts", Duration.INFINITE)

    /** A single step craft: [baseAmount] of [base] crafts into [result]. */
    data class Upgrade(val base: NeuInternalName, val baseAmount: Int, val result: NeuInternalName)

    /** What the craft data says about one base item. */
    sealed interface CraftState {
        /** The first build has not finished yet, so nothing is known about any item. */
        data object NotLoaded : CraftState

        /** The item has no single step craft. */
        data object NoCraft : CraftState

        /** Two or more crafts tie at the highest amount, so no single one can be picked. */
        data class Ambiguous(val options: List<Upgrade>) : CraftState

        /** The inventory already holds enough for one craft. */
        data class Enough(val upgrade: Upgrade) : CraftState

        /** [amount] more of the base item are needed for one craft. */
        data class Missing(val upgrade: Upgrade, val amount: Int) : CraftState
    }

    /** [inInventory] is passed in when the caller already counted, to save one inventory scan per item. */
    fun getCraftState(base: NeuInternalName, inInventory: Int = base.getAmountInInventory()): CraftState {
        val lookup = lookup ?: return CraftState.NotLoaded
        lookup.ambiguous[base]?.let { return CraftState.Ambiguous(it) }
        val upgrade = lookup.upgrades[base] ?: return CraftState.NoCraft

        val missing = upgrade.baseAmount - inInventory
        return if (missing > 0) CraftState.Missing(upgrade, missing) else CraftState.Enough(upgrade)
    }

    @HandleEvent
    private fun onNeuRepoReload() {
        // The sack item list is only rebuilt after this event, so the build has to wait for it.
        DelayedRun.runNextTickEnd("compactor gfs single step crafts") {
            repoReloadCoroutine.launch {
                buildUpgrades()
            }
        }
    }

    private fun buildUpgrades() {
        val picked = mutableMapOf<NeuInternalName, Upgrade>()
        val unclear = mutableMapOf<NeuInternalName, List<Upgrade>>()
        for ((base, results) in createCandidates()) {
            // Assumed, not verified: the compactor takes the largest craft. Ice makes Packed Ice at 9
            // and Enchanted Ice at 160, and 160 is what the compactor produces.
            val highest = results.maxOf { it.baseAmount }
            val tied = results.filter { it.baseAmount == highest }
            tied.singleOrNull()?.let { picked[base] = it } ?: run { unclear[base] = tied }
        }
        lookup = CraftLookup(picked, unclear)
    }

    private fun createCandidates(): Map<NeuInternalName, List<Upgrade>> = NeuItems.allNeuRepoInternalNames()
        // The compactor puts its result into a sack, so anything that has no sack cannot be one.
        .filter { it.asString() in SackApi.sackListInternalNames }
        .flatMap { result -> upgradesFor(result) }
        .groupBy { it.base }

    private fun upgradesFor(result: NeuInternalName): List<Upgrade> = NeuItems.getRecipes(result)
        .filter { it.isCraftingRecipe() }
        .mapNotNull { recipe ->
            val (base, amount) =
                ItemUtils.neededItems(recipe).entries.singleOrNull() ?: return@mapNotNull null
            if (amount <= 1) return@mapNotNull null
            // The base item is what gets pulled with /gfs later, so it needs a sack of its own.
            if (base.asString() !in SackApi.sackListInternalNames) return@mapNotNull null

            Upgrade(base, amount, result)
        }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestcompactorcrafts") {
            description = "List all items with more than one single step craft at the same highest amount"
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback {
                printAmbiguous()
            }
        }
    }

    private fun printAmbiguous() {
        val ambiguous = lookup?.ambiguous ?: run {
            ChatUtils.userError("Recipe data is not loaded yet.")
            return
        }
        if (ambiguous.isEmpty()) {
            ChatUtils.chat("No ambiguous single step crafts found.")
            return
        }
        val lines = ambiguous.map { (base, results) ->
            "${base.asString()} -> ${results.joinToString(", ") { "x${it.baseAmount} into ${it.result.asString()}" }}"
        }.sorted()

        for (line in lines) {
            ChatUtils.consoleLog("ambiguous single step craft: $line")
        }
        ChatUtils.clickToClipboard("Found ${lines.size} ambiguous single step crafts.", lines)
    }

    /**
     * One finished build. [ambiguous] holds base items where two or more crafts tie at the highest
     * amount, so no single upgrade can be picked.
     */
    private data class CraftLookup(
        val upgrades: Map<NeuInternalName, Upgrade>,
        val ambiguous: Map<NeuInternalName, List<Upgrade>>,
    )
}
