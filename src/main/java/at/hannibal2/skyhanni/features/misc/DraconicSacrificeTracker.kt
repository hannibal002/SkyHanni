package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe.NeuRecipeType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoinWithBrackets
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.JsonObject
import com.google.gson.annotations.Expose
import net.minecraft.world.phys.AABB

@SkyHanniModule
object DraconicSacrificeTracker {

    private val config get() = SkyHanniMod.feature.combat.endIsland.draconicSacrificeTracker
    private val patternGroup = RepoPattern.group("misc.draconicsacrifice")

    /**
     * REGEX-TEST: SACRIFICE! You turned Ender Boots into 3 Dragon Essence!
     * REGEX-TEST: SACRIFICE! You turned Ender Helmet into 3 Dragon Essence!
     * REGEX-TEST: SACRIFICE! You turned Old Dragon Helmet into 25 Dragon Essence!
     * REGEX-TEST: SACRIFICE! You turned Wise Dragon Helmet into 25 Dragon Essence!
     */
    private val sacrificeLoot by patternGroup.pattern(
        "sacrifice.colorless",
        "SACRIFICE! You turned (?<item>.*) into (?<amount>\\d+) Dragon Essence!",
    )

    /**
     * REGEX-TEST: BONUS LOOT! You also received 17x Wise Dragon Fragment from your sacrifice!
     */
    private val bonusLoot by patternGroup.pattern(
        "bonus.colorless",
        "BONUS LOOT! You also received (?:\\w(?<amount>\\d+)?x)? ?(?<item>.*) from your sacrifice!",
    )

    private val tracker =
        SkyHanniItemTracker(
            "Draconic Sacrifice Profit Tracker",
            ::Data,
            { it.draconicSacrificeTracker },
            trackerConfig = { config.perTrackerConfig }
        ) {
            drawDisplay(it)
        }

    private val altarArea = AABB(-601.0, 4.0, -282.0, -586.0, 15.0, -269.0)
    private val ESSENCE_DRAGON = "ESSENCE_DRAGON".toInternalName()

    data class Data(
        @Expose var itemsSacrificed: Long = 0L,
        @Expose var sacrificedItemsMap: MutableMap<NeuInternalName, Long> = mutableMapOf(),
    ) : ItemTrackerData<SessionUptime.Normal>(SessionUptime.Normal::class) {
        override fun getDescription(timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / itemsSacrificed
            val dropRate = percentage.coerceAtMost(1.0).formatPercentage()
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop rate: §c$dropRate.",
            )
        }

        override fun getCoinName(item: TrackedItem) = "§dDragon Essence"

        override fun getCoinDescription(item: TrackedItem): List<String> {
            val essences = item.totalAmount.addSeparators()
            return listOf(
                "§7Sacrificed items give you dragon essence.",
                "§7You got §6$essences essence §7that way.",
            )
        }
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§5§lDraconic Sacrifice Profit Tracker")
        val profit = tracker.drawItems(data, { true }, this)

        val sacrificedItems = data.sacrificedItemsMap.map { (item, amount) ->
            val price = item.getAdjustedPrice(config.perTrackerConfig.trackerConfig.priceSource)
            Triple(item, amount, -amount * price)
        }.sortedBy { it.third }
        val sacrificedCost = sacrificedItems.sumOf { (_, _, totalCost) -> totalCost }

        add(
            Renderable.hoverTips(
                "§b${data.itemsSacrificed.addSeparators()} §6Items Sacrificed",
                sacrificedItems.map { (item, amount, _) ->
                    "${item.repoItemName}: §b${amount.addSeparators()}"
                }
            ).toSearchable()
        )
        add(
            Renderable.hoverTips(
                "§6Total Sacrifice Cost: ${sacrificedCost.formatCoinWithBrackets()}",
                sacrificedItems.map { (item, amount, totalCost) ->
                    "${item.repoItemName}: §b${amount.addSeparators()} ${totalCost.formatCoinWithBrackets()}"
                }
            ).toSearchable()
        )

        val duration = data.getTotalUptime()
        addAll(
            tracker.addTotalProfit(
                profit + sacrificedCost,
                data.itemsSacrificed,
                "sacrifice",
                duration,
                "Sacrifices"
            )
        )

        tracker.addPriceFromButton(this)
    }

    private fun NeuInternalName.getAdjustedPrice(priceSource: ItemPriceSource): Double {
        if (!config.fragmentPrice) return getPrice()
        val ingredients = NeuItems.getRecipes(this)
            .firstOrNull { it.recipeType == NeuRecipeType.CRAFTING }
            ?.ingredients
            ?: return getPrice(priceSource)

        val uniqueIngredients = ingredients
            .groupBy { it.internalName }
            .mapValues { (_, list) -> list.sumOf { it.count } }

        // Recipe consists solely of one fragment type
        if (uniqueIngredients.size == 1) {
            val (ingredient, amount) = uniqueIngredients.entries.first()
            if ("FRAGMENT" in ingredient.asString()) {
                return ingredient.getPrice(priceSource) * amount
            }
        }

        return getPrice(priceSource)
    }

    @HandleEvent
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        val msg = event.cleanMessage
        sacrificeLoot.matchMatcher(msg) {
            val amount = group("amount").toInt()
            val item = group("item")
            tracker.addItem(ESSENCE_DRAGON, amount, command = false)
            val internalName = NeuInternalName.fromItemNameOrNull(item) ?: return
            tracker.modify {
                it.itemsSacrificed += 1
                it.sacrificedItemsMap.addOrPut(internalName, 1)
            }
        }

        bonusLoot.matchMatcher(msg) {
            val item = group("item")
            val amount = groupOrNull("amount")?.toInt() ?: 1
            val internalName = NeuInternalName.fromItemNameOrNull(item) ?: return
            tracker.addItem(internalName, amount, command = false)
        }
        tracker.update()
    }

    init {
        tracker.initRenderer({ config.position }) { shouldShowDisplay() }
    }

    private fun shouldShowDisplay(): Boolean {
        if (!isEnabled()) return false
        if (config.onlyInVoidSlate && !altarArea.isPlayerInside()) return false

        return true
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetdraconicsacrificetracker") {
            description = "Resets the Draconic Sacrifice Tracker."
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(141, "#profile.draconicSacrificeTracker.sacrificedItemsMap") { element ->
            val oldMap = element.asJsonObject
            val newMap = JsonObject()
            for ((displayName, value) in oldMap.entrySet()) {
                val internalName = NeuInternalName.fromItemNameOrNull(displayName.removeColor()) ?: continue
                newMap.addProperty(internalName.asString(), value.asLong)
            }
            newMap
        }
    }

    private fun isEnabled() = IslandType.THE_END.isInIsland() && config.enabled
}
