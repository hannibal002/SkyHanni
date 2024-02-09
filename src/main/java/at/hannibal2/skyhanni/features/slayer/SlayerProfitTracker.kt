package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.data.jsonobjects.repo.SlayerProfitTrackerItemsJson
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SlayerChangeEvent
import at.hannibal2.skyhanni.events.SlayerQuestCompleteEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.Expose
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object SlayerProfitTracker {

    private val config get() = SkyHanniMod.feature.slayer.itemProfitTracker

    private var itemLogCategory = ""
    private var baseSlayerType = ""
    private val trackers = mutableMapOf<String, SkyHanniItemTracker<Data>>()

    class Data : ItemTrackerData() {

        override fun resetItems() {
            slayerSpawnCost = 0
            slayerCompletedCount = 0
        }

        @Expose
        var slayerSpawnCost: Long = 0

        @Expose
        var slayerCompletedCount = 0L

        override fun getDescription(timesDropped: Long): List<String> {
            val percentage = timesDropped.toDouble() / slayerCompletedCount
            val perBoss = LorenzUtils.formatPercentage(percentage.coerceAtMost(1.0))

            return listOf(
                "§7Dropped §e${timesDropped.addSeparators()} §7times.",
                "§7Your drop rate: §c$perBoss",
            )
        }

        override fun getCoinName(item: TrackedItem) = "§6Mob Kill Coins"

        override fun getCoinDescription(item: TrackedItem): List<String> {
            val mobKillCoinsFormat = NumberUtil.format(item.totalAmount)
            return listOf(
                "§7Killing mobs gives you coins (more with scavenger).",
                "§7You got §6$mobKillCoinsFormat coins §7that way."
            )
        }
    }

    private val ItemTrackerData.TrackedItem.timesDropped get() = timesGained

    private fun addSlayerCosts(price: Int) {
        getTracker()?.modify {
            it.slayerSpawnCost += price
        }
    }

    private var allowedItems = mapOf<String, List<NEUInternalName>>()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        allowedItems = event.getConstant<SlayerProfitTrackerItemsJson>("SlayerProfitTrackerItems").slayers
    }

    @SubscribeEvent
    fun onPurseChange(event: PurseChangeEvent) {
        if (!isEnabled()) return
        val coins = event.coins
        if (event.reason == PurseChangeCause.GAIN_MOB_KILL && SlayerAPI.isInCorrectArea) {
            getTracker()?.addCoins(coins.toInt())
        }
        if (event.reason == PurseChangeCause.LOSE_SLAYER_QUEST_STARTED) {
            addSlayerCosts(coins.toInt())
        }
    }

    @SubscribeEvent
    fun onSlayerChange(event: SlayerChangeEvent) {
        val newSlayer = event.newSlayer
        itemLogCategory = newSlayer.removeColor()
        baseSlayerType = itemLogCategory.substringBeforeLast(" ")
        getTracker()?.update()
    }

    private fun getTracker(): SkyHanniItemTracker<Data>? {
        if (itemLogCategory == "") return null

        return trackers.getOrPut(itemLogCategory) {
            val getStorage: (Storage.ProfileSpecific) -> Data = {
                it.slayerProfitData.getOrPut(
                    itemLogCategory
                ) { Data() }
            }
            SkyHanniItemTracker("$itemLogCategory Profit Tracker", { Data() }, getStorage) { drawDisplay(it) }
        }
    }

    @SubscribeEvent
    fun onQuestComplete(event: SlayerQuestCompleteEvent) {
        getTracker()?.modify {
            it.slayerCompletedCount++
        }
    }

    @SubscribeEvent
    fun onItemAdd(event: ItemAddEvent) {
        if (!isEnabled()) return
        if (!SlayerAPI.isInCorrectArea) return
        if (!SlayerAPI.hasActiveSlayerQuest()) return

        val internalName = event.internalName
        val amount = event.amount

        if (!isAllowedItem(internalName)) {
            ChatUtils.debug("Ignored non-slayer item pickup: '$internalName' '$itemLogCategory'")
            return
        }

        getTracker()?.addItem(internalName, amount)
    }

    private fun isAllowedItem(internalName: NEUInternalName): Boolean {
        val allowedList = allowedItems[baseSlayerType] ?: return false
        return internalName in allowedList
    }

    private fun drawDisplay(itemLog: Data) = buildList<List<Any>> {
        val tracker = getTracker() ?: return@buildList
        addAsSingletonList("§e§l$itemLogCategory Profit Tracker")

        var profit = tracker.drawItems(itemLog, { true }, this)
        val slayerSpawnCost = itemLog.slayerSpawnCost
        if (slayerSpawnCost != 0L) {
            val mobKillCoinsFormat = NumberUtil.format(slayerSpawnCost)
            addAsSingletonList(
                Renderable.hoverTips(
                    " §7Slayer Spawn Costs: §c$mobKillCoinsFormat",
                    listOf("§7You paid §c$mobKillCoinsFormat §7in total", "§7for starting the slayer quests.")
                )
            )
            profit += slayerSpawnCost
        }

        val slayerCompletedCount = itemLog.slayerCompletedCount
        addAsSingletonList(
            Renderable.hoverTips(
                "§7Bosses killed: §e${slayerCompletedCount.addSeparators()}",
                listOf("§7You killed the $itemLogCategory boss", "§e${slayerCompletedCount.addSeparators()} §7times.")
            )
        )

        addAsSingletonList(tracker.addTotalProfit(profit, itemLog.slayerCompletedCount, "boss"))

        tracker.addPriceFromButton(this)
    }

    val coinFormat: (ItemTrackerData.TrackedItem) -> Pair<String, List<String>> = { item ->
        val mobKillCoinsFormat = NumberUtil.format(item.totalAmount)
        val text = " §6Mob kill coins§7: §6$mobKillCoinsFormat"
        val lore = listOf(
            "§7Killing mobs gives you coins (more with scavenger)",
            "§7You got §e$mobKillCoinsFormat §7coins in total this way"
        )

        text to lore
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (!SlayerAPI.isInCorrectArea) return

        getTracker()?.renderDisplay(config.pos)
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(10, "#profile.slayerProfitData") { old ->
            for (data in old.asJsonObject.entrySet().map { it.value.asJsonObject }) {
                val items = data.get("items").asJsonObject
                for (item in items.entrySet().map { it.value.asJsonObject }) {
                    val oldValue = item.get("timesDropped")
                    item.add("timesGained", oldValue)
                }

                val coinAmount = data.get("mobKillCoins")
                val coins = JsonObject()
                coins.add("internalName", JsonPrimitive("SKYBLOCK_COIN"))
                coins.add("timesDropped", JsonPrimitive(1))
                coins.add("totalAmount", coinAmount)
                items.add("SKYBLOCK_COIN", coins)
            }

            old
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled

    fun clearProfitCommand(args: Array<String>) {
        if (itemLogCategory == "") {
            ChatUtils.userError(
                "No current slayer data found! " +
                    "§eGo to a slayer area and start the specific slayer type you want to reset the data of.",
            )
            return
        }

        getTracker()?.resetCommand(args, "shclearslayerprofits")
    }
}
