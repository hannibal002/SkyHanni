package at.hannibal2.skyhanni.features.event.yearofthepig

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.yearofthepig.ShinyOrbLootedEvent
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RenderDisplayConfig
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerUtils.addSkillXpInfo
import at.hannibal2.skyhanni.utils.tracker.data.ItemTrackerData
import com.google.gson.annotations.Expose

@SkyHanniModule
object ShinyOrbTracker : SkyHanniItemTracker<ShinyOrbTracker.Data>("Shiny Orb Tracker") {

    override val config get() = SkyHanniMod.feature.event.yearOfThePig.shinyOrbTracker
    override val storageAccessor: (ProfileSpecificStorage) -> Data = { it.shinyOrbTracker }
    override val renderConfig = RenderDisplayConfig(
        outsideInventory = true,
        condition = { passesHoldingItem() && PigFeaturesApi.isYearOfThePig() },
        onlyOnIsland = IslandType.HUB,
    )
    private val SHINY_ORB_ITEM = "SHINY_ORB".toInternalName()
    private val SHINY_ROD_ITEM = "SHINY_ROD".toInternalName()
    private val SHINY_SHARD_ITEM = "SHINY_SHARD".toInternalName()

    private fun passesHoldingItem() = !config.holdingItems || InventoryUtils.getItemInHand()?.let {
        it.getInternalNameOrNull() in setOf(SHINY_ORB_ITEM, SHINY_ROD_ITEM)
    } == true

    data class Data(
        @Expose var orbsUsed: Long = 0L,
        @Expose var orbsCompleted: Long = 0L,
        @Expose var skillXpGained: MutableMap<SkillType, Long> = enumMapOf(),
    ) : ItemTrackerData<SessionUptime.Normal>() {
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
    }

    @HandleEvent
    fun onShinyOrbUsed() {
        modify { it.orbsUsed++ }
    }

    @HandleEvent
    fun onShinyOrbCharged() {
        modify { it.orbsCompleted++ }
    }

    @HandleEvent
    fun onShinyOrbLooted(event: ShinyOrbLootedEvent) {
        addItem(SHINY_SHARD_ITEM, 1, command = false)
        when {
            event.loot != null -> {
                val (internalName, amount) = event.loot.first to event.loot.second
                addItem(internalName, amount, command = false)
            }

            event.coins != null -> addCoins(event.coins, command = false)
            event.skillXp != null -> modify {
                val (skill, amount) = event.skillXp.first to event.skillXp.second
                it.skillXpGained.addOrPut(skill, amount)
            }
        }
    }

    override fun drawDisplayF(data: Data): List<Searchable> = buildList {
        if (data.orbsUsed == 0L) return@buildList
        addSearchString("§6§lShiny Orb Profit Tracker")
        var profit = drawItems(data, { true }, this)

        val orbPrice = 5000.0
        val totalOrbPrice = data.orbsUsed * orbPrice
        profit -= totalOrbPrice
        addSearchString("§7${data.orbsUsed}x §6Shiny Orb§7: §c-${totalOrbPrice.shortFormat()} coins")

        // Skill XP gains
        addSkillXpInfo(data.skillXpGained)

        val duration = data.getTotalUptime()
        addAll(addTotalProfit(profit, data.orbsCompleted, "orb used", duration, "Orbs used"))
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetshinyorbtracker") {
            description = "Resets the Shiny Orb Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { resetCommand() }
        }
    }
}
