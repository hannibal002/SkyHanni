package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SlayerChangeEvent
import at.hannibal2.skyhanni.events.SlayerQuestCompleteEvent
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.jsonobjects.SlayerProfitTrackerItemsJson
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.Expose
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object SlayerProfitTracker {
    private val config get() = SkyHanniMod.feature.slayer.itemProfitTracker

    private var itemLogCategory = ""
    private var baseSlayerType = ""
    private val logger = LorenzLogger("slayer/profit_tracker")
    private val trackers = mutableMapOf<String, SkyHanniItemTracker<Data>>()

    class Data : ItemTrackerData() {
        override fun resetItems() {
            slayerSpawnCost = 0
            slayerCompletedCount = 0
        }

        @Expose
        var slayerSpawnCost: Long = 0

        @Expose
        var slayerCompletedCount = 0

        override fun getDescription(timesDropped: Long): List<String> {
            val percentage = timesDropped.toDouble() / slayerCompletedCount
            val perBoss = LorenzUtils.formatPercentage(percentage.coerceAtMost(1.0))

            return listOf(
                "§7Dropped §e${timesDropped.addSeparators()} §7times.",
                "§7Your drop rate: §c$perBoss",
            )
        }

        override fun getCoinFormat(item: TrackedItem, numberColor: String): Pair<String, List<String>> {
            val mobKillCoinsFormat = NumberUtil.format(item.totalAmount)
            val gained = item.timesGained
            val text = " $numberColor${gained}x §6Mob Kill Coins§7: §6$mobKillCoinsFormat"
            val lore = listOf(
                "§7Killing mobs gives you coins (more with scavenger)",
                "§7You got §6$mobKillCoinsFormat coins §7in total this way"
            )
            return text to lore
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
            logger.log("Coins gained for killing mobs: ${coins.addSeparators()}")
            addMobKillCoins(coins.toInt())
        }
        if (event.reason == PurseChangeCause.LOSE_SLAYER_QUEST_STARTED) {
            logger.log("Coins paid for starting slayer quest: ${coins.addSeparators()}")
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

    private fun addMobKillCoins(coins: Int) {
        getTracker()?.addCoins(coins)
    }

    private fun addItemPickup(internalName: NEUInternalName, stackSize: Int) {
        getTracker()?.addItem(internalName, stackSize)
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
            LorenzUtils.debug("Ignored non-slayer item pickup: '$internalName' '$itemLogCategory'")
            return
        }

        val (itemName, price) = SlayerAPI.getItemNameAndPrice(internalName, amount)
        addItemPickup(internalName, amount)
        logger.log("Coins gained for picking up an item ($itemName) ${price.addSeparators()}")
        if (config.priceInChat && price > config.minimumPrice) {
            LorenzUtils.chat("§a+Slayer Drop§7: §r$itemName")
        }
        if (config.titleWarning && price > config.minimumPriceWarning) {
            LorenzUtils.sendTitle("§a+ $itemName", 5.seconds)
        }
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

        val profitFormat = NumberUtil.format(profit)
        val profitPrefix = if (profit < 0) "§c" else "§6"

        val profitPerBoss = profit / itemLog.slayerCompletedCount
        val profitPerBossFormat = NumberUtil.format(profitPerBoss)

        val text = "§eTotal Profit: $profitPrefix$profitFormat coins"
        addAsSingletonList(Renderable.hoverTips(text, listOf("§7Profit per boss: $profitPrefix$profitPerBossFormat")))

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
        event.move(10, "#profile.slayerProfitData", "#profile.slayerProfitData") { old ->
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
            LorenzUtils.userError(
                "No current slayer data found! " +
                    "§eGo to a slayer area and start the specific slayer type you want to reset the data of.",
            )
            return
        }

        getTracker()?.resetCommand(args, "shclearslayerprofits")
    }
}
