package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.events.SlayerChangeEvent
import at.hannibal2.skyhanni.events.SlayerQuestCompleteEvent
import at.hannibal2.skyhanni.events.entity.ItemAddInInventoryEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi.Companion.getBazaarData
import at.hannibal2.skyhanni.test.PriceSource
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addSelector
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getPriceOrNull
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.jsonobjects.SlayerProfitTrackerItemsJson
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object SlayerProfitTracker {
    private val config get() = SkyHanniMod.feature.slayer.itemProfitTracker

    private val diceRollChatPattern =
        "§eYour §r§(5|6High Class )Archfiend Dice §r§erolled a §r§.(?<number>.)§r§e! Bonus: §r§.(?<hearts>.*)❤".toPattern()

    private val ARCHFIEND_DICE = "ARCHFIEND_DICE".asInternalName()
    private val HIGH_CLASS_ARCHFIEND_DICE = "HIGH_CLASS_ARCHFIEND_DICE".asInternalName()

    private var itemLogCategory = ""
    private var baseSlayerType = ""
    private val logger = LorenzLogger("slayer/profit_tracker")
    private var lastClickDelay = 0L
    private val trackers = mutableMapOf<String, SkyHanniTracker<Data>>()

    class Data : TrackerData() {
        override fun reset() {
            items.clear()
            mobKillCoins = 0
            slayerSpawnCost = 0
            slayerCompletedCount = 0
        }

        @Expose
        var items: MutableMap<NEUInternalName, SlayerItem> = HashMap()

        @Expose
        var mobKillCoins: Long = 0

        @Expose
        var slayerSpawnCost: Long = 0

        @Expose
        var slayerCompletedCount = 0

        class SlayerItem {
            @Expose
            var internalName: NEUInternalName? = null

            @Expose
            var timesDropped: Long = 0

            @Expose
            var totalAmount: Long = 0

            @Expose
            var hidden = false

            override fun toString() = "SlayerItem{" +
                "internalName='" + internalName + '\'' +
                ", timesDropped=" + timesDropped +
                ", totalAmount=" + totalAmount +
                ", hidden=" + hidden +
                '}'
        }

        override fun toString() = "SlayerProfitTracker.Data{" +
            "items=" + items +
            ", mobKillCoins=" + mobKillCoins +
            ", slayerSpawnCost=" + slayerSpawnCost +
            ", slayerCompletedCount=" + slayerCompletedCount +
            '}'
    }

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
        getTracker()?.modify {
            it.mobKillCoins += coins
        }
    }

    private fun addItemPickup(internalName: NEUInternalName, stackSize: Int) {
        getTracker()?.modify {
            val slayerItem = it.items.getOrPut(internalName) { Data.SlayerItem() }

            slayerItem.timesDropped++
            slayerItem.totalAmount += stackSize
        }
    }

    private fun getTracker(): SkyHanniTracker<Data>? {
        if (itemLogCategory == "") return null

        return trackers.getOrPut(itemLogCategory) {
            val getStorage: (Storage.ProfileSpecific) -> Data = {
                it.slayerProfitData.getOrPut(
                    itemLogCategory
                ) { Data() }
            }
            SkyHanniTracker("$itemLogCategory Profit Tracker", { Data() }, getStorage) { drawDisplay(it) }
        }
    }

    @SubscribeEvent
    fun onQuestComplete(event: SlayerQuestCompleteEvent) {
        getTracker()?.modify {
            it.slayerCompletedCount++
        }
    }

    @SubscribeEvent
    fun onSackChange(event: SackChangeEvent) {
        if (!isEnabled()) return
        if (!SlayerAPI.isInCorrectArea) return
        if (!SlayerAPI.hasActiveSlayerQuest()) return

        for (sackChange in event.sackChanges) {
            val change = sackChange.delta
            if (change > 0) {
                val internalName = sackChange.internalName
                addItem(internalName, change)
            }
        }
    }

    @SubscribeEvent
    fun onItemAdd(event: ItemAddInInventoryEvent) {
        if (!isEnabled()) return
        if (!SlayerAPI.isInCorrectArea) return
        if (!SlayerAPI.hasActiveSlayerQuest()) return

        val internalName = event.internalName
        if (internalName == ARCHFIEND_DICE || internalName == HIGH_CLASS_ARCHFIEND_DICE) {
            if (lastDiceRoll.passedSince() < 500.milliseconds) {
                return
            }
        }

        addItem(internalName, event.amount)
    }

    private var lastDiceRoll = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (diceRollChatPattern.matches(event.message)) {
            lastDiceRoll = SimpleTimeMark.now()
        }
    }

    private fun addItem(internalName: NEUInternalName, amount: Int) {
        if (!isAllowedItem(internalName)) {
            LorenzUtils.debug("Ignored non-slayer item pickup: '$internalName' '$itemLogCategory'")
            return
        }

        val (itemName, price) = SlayerAPI.getItemNameAndPrice(internalName, amount)
        addItemPickup(internalName, amount)
        logger.log("Coins gained for picking up an item ($itemName) ${price.addSeparators()}")
        if (config.priceInChat && price > config.minimumPrice) {
            LorenzUtils.chat("§e[SkyHanni] §a+Slayer Drop§7: §r$itemName")
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

        var profit = 0.0
        val map = mutableMapOf<Renderable, Long>()
        for ((internalName, itemProfit) in itemLog.items) {
            val amount = itemProfit.totalAmount

            val price = (getPrice(internalName) * amount).toLong()

            val cleanName = SlayerAPI.getNameWithEnchantmentFor(internalName)
            var name = cleanName
            val priceFormat = NumberUtil.format(price)
            val hidden = itemProfit.hidden
            if (hidden) {
                while (name.startsWith("§f")) {
                    name = name.substring(2)
                }
                name = StringUtils.addFormat(name, "§m")
            }
            val text = " §7${amount.addSeparators()}x $name§7: §6$priceFormat"

            val timesDropped = itemProfit.timesDropped
            val percentage = timesDropped.toDouble() / itemLog.slayerCompletedCount
            val perBoss = LorenzUtils.formatPercentage(percentage.coerceAtMost(1.0))

            val renderable = if (tracker.isInventoryOpen()) Renderable.clickAndHover(
                text, listOf(
                    "§7Dropped §e${timesDropped.addSeparators()} §7times.",
                    "§7Your drop rate: §c$perBoss",
                    "",
                    "§eClick to " + (if (hidden) "show" else "hide") + "!",
                    "§eControl + Click to remove this item!",
                )
            ) {
                if (System.currentTimeMillis() > lastClickDelay + 150) {

                    if (KeyboardManager.isControlKeyDown()) {
                        itemLog.items.remove(internalName)
                        LorenzUtils.chat("§e[SkyHanni] Removed $cleanName §efrom slayer profit display.")
                        lastClickDelay = System.currentTimeMillis() + 500
                    } else {
                        itemProfit.hidden = !hidden
                        lastClickDelay = System.currentTimeMillis()
                    }
                    tracker.update()
                }
            } else Renderable.string(text)
            if (tracker.isInventoryOpen() || !hidden) {
                map[renderable] = price
            }
            profit += price
        }
        val mobKillCoins = itemLog.mobKillCoins
        if (mobKillCoins != 0L) {
            val mobKillCoinsFormat = NumberUtil.format(mobKillCoins)
            map[Renderable.hoverTips(
                " §7Mob kill coins: §6$mobKillCoinsFormat",
                listOf(
                    "§7Killing mobs gives you coins (more with scavenger)",
                    "§7You got §e$mobKillCoinsFormat §7coins in total this way"
                )
            )] = mobKillCoins
            profit += mobKillCoins
        }
        val slayerSpawnCost = itemLog.slayerSpawnCost
        if (slayerSpawnCost != 0L) {
            val mobKillCoinsFormat = NumberUtil.format(slayerSpawnCost)
            map[Renderable.hoverTips(
                " §7Slayer Spawn Costs: §c$mobKillCoinsFormat",
                listOf("§7You paid §c$mobKillCoinsFormat §7in total", "§7for starting the slayer quests.")
            )] = slayerSpawnCost
            profit += slayerSpawnCost
        }

        for (text in map.sortedDesc().keys) {
            addAsSingletonList(text)
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

        val text = "§eTotal Profit: $profitPrefix$profitFormat"
        addAsSingletonList(Renderable.hoverTips(text, listOf("§7Profit per boss: $profitPrefix$profitPerBossFormat")))

        if (tracker.isInventoryOpen()) {
            addSelector<PriceSource>(
                "",
                getName = { type -> type.displayName },
                isCurrent = { it.ordinal == config.priceFrom },
                onChange = {
                    config.priceFrom = it.ordinal
                    tracker.update()
                }
            )
        }
    }

    private fun getPrice(internalName: NEUInternalName) = when (config.priceFrom) {
        0 -> internalName.getBazaarData()?.sellPrice ?: internalName.getPriceOrNull() ?: 0.0
        1 -> internalName.getBazaarData()?.buyPrice ?: internalName.getPriceOrNull() ?: 0.0

        else -> internalName.getNpcPriceOrNull() ?: 0.0
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (!SlayerAPI.isInCorrectArea) return

        getTracker()?.renderDisplay(config.pos)
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled

    fun clearProfitCommand(args: Array<String>) {
        if (itemLogCategory == "") {
            LorenzUtils.chat(
                "§c[SkyHanni] No current slayer data found. " +
                    "Go to a slayer area and start the specific slayer type you want to reset the data of."
            )
            return
        }

        getTracker()?.resetCommand(args, "shclearslayerprofits")
    }
}
