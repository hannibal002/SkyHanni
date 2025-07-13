package at.hannibal2.skyhanni.features.event.yearofthepig

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.yearofthepig.ShinyOrbChargedEvent
import at.hannibal2.skyhanni.events.yearofthepig.ShinyOrbLootedEvent
import at.hannibal2.skyhanni.events.yearofthepig.ShinyOrbUsedEvent
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerUtils.addSkillXpInfo
import com.google.gson.annotations.Expose

@SkyHanniModule
object ShinyOrbTracker {

    private val config get() = SkyHanniMod.feature.event.yearOfThePig.shinyOrbTracker
    private val SHINY_ORB_ITEM = "SHINY_ORB".toInternalName()
    private val SHINY_ROD_ITEM = "SHINY_ROD".toInternalName()
    private val tracker = SkyHanniItemTracker(
        "Shiny Orb Tracker",
        { ShinyOrbData() },
        { it.shinyOrbTracker },
    ) { drawDisplay(it) }

    private fun passesHoldingItem() = !config.holdingItems || InventoryUtils.getItemInHand()?.let {
        it.getInternalNameOrNull() in setOf(SHINY_ORB_ITEM, SHINY_ROD_ITEM)
    } == true

    init {
        tracker.initRenderer(
            { config.position },
        ) { config.enabled && IslandType.HUB.isCurrent() && passesHoldingItem() && PigFeaturesApi.isYearOfThePig() }
    }

    class ShinyOrbData : ItemTrackerData() {

        override fun resetItems() {
            orbsUsed = 0L
            orbsCompleted = 0L
            skillXpGained = enumMapOf()
        }

        override fun getDescription(timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / orbsCompleted
            val perOrb = percentage.coerceAtMost(1.0).formatPercentage()

            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop chance per §6Shiny Orb§7: §c$perOrb",
            )
        }

        override fun getCoinName(item: TrackedItem) = "§6Coins"

        override fun getCoinDescription(item: TrackedItem): List<String> {
            val coinsFormat = item.totalAmount.shortFormat()
            return listOf(
                "§6Shiny Orbs§7 occasionally drop coins as a reward.",
                "§7You got §6$coinsFormat coins §7that way.",
            )
        }

        @Expose
        var orbsUsed = 0L

        @Expose
        var orbsCompleted = 0L

        @Expose
        var skillXpGained: MutableMap<SkillType, Long> = enumMapOf()
    }

    @HandleEvent
    fun onShinyOrbUsed(event: ShinyOrbUsedEvent) {
        tracker.modify { it.orbsUsed++ }
    }

    @HandleEvent
    fun onShinyOrbCharged(event: ShinyOrbChargedEvent) {
        tracker.modify { it.orbsCompleted++ }
    }

    @HandleEvent
    fun onShinyOrbLooted(event: ShinyOrbLootedEvent) {
        when {
            event.loot != null -> {
                val (internalName, amount) = event.loot.first to event.loot.second
                tracker.addItem(internalName, amount, command = false)
            }

            event.coins != null -> tracker.addCoins(event.coins, command = false)
            event.skillXp != null -> tracker.modify { tracker ->
                val (skill, amount) = event.skillXp.first to event.skillXp.second
                tracker.skillXpGained.addOrPut(skill, amount)
            }
        }
    }

    private fun drawDisplay(data: ShinyOrbData): List<Searchable> = buildList {
        if (data.orbsUsed == 0L) return@buildList
        addSearchString("§6§lShiny Orb Profit Tracker")
        var profit = tracker.drawItems(data, { true }, this)

        val orbPrice = 5000.0
        val totalOrbPrice = data.orbsUsed * orbPrice
        profit -= totalOrbPrice
        add(StringRenderable("§7${data.orbsUsed}x §6Shiny Orb§7: §c-${totalOrbPrice.shortFormat()} coins").toSearchable())

        // Skill XP gains
        addSkillXpInfo(data.skillXpGained)

        add(tracker.addTotalProfit(profit, data.orbsCompleted, "orb used"))
    }
}
