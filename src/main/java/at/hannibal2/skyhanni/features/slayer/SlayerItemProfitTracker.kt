package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.Storage.ProfileSpecific.SlayerProfitList
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.events.SlayerChangeEvent
import at.hannibal2.skyhanni.events.SlayerQuestCompleteEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi.Companion.getBazaarData
import at.hannibal2.skyhanni.test.PriceSource
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addSelector
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getPriceOrNull
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.jsonobjects.SlayerProfitTrackerItemsJson
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.tracker.DisplayMode
import at.hannibal2.skyhanni.utils.tracker.TrackerUtils
import at.hannibal2.skyhanni.utils.tracker.TrackerUtils.addDisplayModeToggle
import at.hannibal2.skyhanni.utils.tracker.TrackerUtils.addSessionResetButton
import at.hannibal2.skyhanni.utils.tracker.TrackerWrapper
import com.google.common.cache.CacheBuilder
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.entity.item.EntityItem
import net.minecraft.network.play.server.S0DPacketCollectItem
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

object SlayerItemProfitTracker {
    private val config get() = SkyHanniMod.feature.slayer.itemProfitTracker
    private var collectedCache = CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.SECONDS).build<Int, Unit>()

    private var itemLogCategory = ""
    private var baseSlayerType = ""
    private var display = emptyList<List<Any>>()
    private val logger = LorenzLogger("slayer/item_profit_tracker")
    private var inventoryOpen = false
    private var lastClickDelay = 0L
    private var currentSessionData = mutableMapOf<String, SlayerProfitList>()

    private fun addSlayerCosts(price: Int) {
        val itemLog = currentLog() ?: return
        itemLog.modify {
            it.slayerSpawnCost += price
        }
        update()
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
        update()
    }

    private fun addMobKillCoins(coins: Int) {
        val itemLog = currentLog() ?: return

        itemLog.modify {
            it.mobKillCoins += coins
        }
        update()
    }

    private fun addItemPickup(internalName: NEUInternalName, stackSize: Int) {
        val itemLog = currentLog() ?: return

        itemLog.modify {
            val slayerItemProfit = it.items.getOrPut(internalName) { SlayerProfitList.SlayerItemProfit() }

            slayerItemProfit.timesDropped++
            slayerItemProfit.totalAmount += stackSize
        }

        update()
    }

    private fun currentDisplay() = currentLog()?.get(TrackerUtils.currentDisplayMode)

    private fun currentLog(): TrackerWrapper<SlayerProfitList>? {
        if (itemLogCategory == "") return null
        val profileSpecific = ProfileStorageData.profileSpecific ?: return null

        return TrackerWrapper(
            profileSpecific.slayerProfitData.getOrPut(itemLogCategory) { SlayerProfitList() },
            currentSessionData.getOrPut(itemLogCategory) { SlayerProfitList() }
        )
    }

    @SubscribeEvent
    fun onQuestComplete(event: SlayerQuestCompleteEvent) {
        val itemLog = currentLog() ?: return

        itemLog.modify {
            it.slayerCompletedCount++
        }

        update()
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

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        if (!isEnabled()) return
        if (!SlayerAPI.isInCorrectArea) return
        if (!SlayerAPI.hasActiveSlayerQuest()) return

        val packet = event.packet
        if (packet !is S0DPacketCollectItem) return

        val entityID = packet.collectedItemEntityID
        val item = EntityUtils.getEntityByID(entityID) ?: return
        if (item !is EntityItem) return

        if (collectedCache.getIfPresent(entityID) != null) return
        collectedCache.put(entityID, Unit)

        val itemStack = item.entityItem
        val name = itemStack.name ?: return
        if (SlayerAPI.ignoreSlayerDrop(name)) return
        val internalName = itemStack.getInternalNameOrNull() ?: return
        addItem(internalName, itemStack.stackSize)
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

    fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList<List<Any>> {
        val itemLog = currentDisplay() ?: return@buildList

        addAsSingletonList("§e§l$itemLogCategory Profit Tracker")
        if (inventoryOpen) {
            addDisplayModeToggle {
                update()
            }
        }

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

            val renderable = if (inventoryOpen) Renderable.clickAndHover(
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
                    update()
                }
            } else Renderable.string(text)
            if (inventoryOpen || !hidden) {
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

        if (inventoryOpen) {
            addSelector<PriceSource>(
                "",
                getName = { type -> type.displayName },
                isCurrent = { it.ordinal == config.priceFrom },
                onChange = {
                    config.priceFrom = it.ordinal
                    update()
                }
            )
        }
        if (inventoryOpen && TrackerUtils.currentDisplayMode == DisplayMode.CURRENT) {
            addSessionResetButton("$itemLogCategory Slayer", currentLog()) {
                update()
            }
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

        val currentlyOpen = Minecraft.getMinecraft().currentScreen is GuiInventory
        if (inventoryOpen != currentlyOpen) {
            inventoryOpen = currentlyOpen
            update()
        }

        config.pos.renderStringsAndItems(display, posLabel = "Slayer Item Profit Tracker")
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

        TrackerUtils.resetCommand("$itemLogCategory Slayer", "shclearslayerprofits", args, currentLog()) {
            update()
        }
    }
}
