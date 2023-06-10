package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.bazaar.BazaarData
import at.hannibal2.skyhanni.test.PriceSource
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addSelector
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import com.google.common.cache.CacheBuilder
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.entity.item.EntityItem
import net.minecraft.network.play.server.S0DPacketCollectItem
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.concurrent.TimeUnit

class SlayerItemProfitTracker {
    private val config get() = SkyHanniMod.feature.slayer.itemProfitTracker
    private var collectedCache = CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.SECONDS).build<Int, Unit>()

    private var itemLogCategory = ""
    private var display = listOf<List<Any>>()
    private val logger = LorenzLogger("slayer/item_profit_tracker")
    private var inventoryOpen = false
    private var lastClickDelay = 0L

    private fun addSlayerCosts(price: Int) {
        val itemLog = currentLog() ?: return
        itemLog.slayerSpawnCost += price
        update()
    }

    @SubscribeEvent
    fun onPurseChange(event: PurseChangeEvent) {
        if (!isEnabled()) return
        val coins = event.coins
        if (event.reason == PurseChangeCause.GAIN_MOB_KILL) {
            if (SlayerAPI.isInSlayerArea) {
                logger.log("Coins gained for killing mobs: ${coins.addSeparators()}")
                addMobKillCoins(coins.toInt())
            }
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
        update()
    }

    private fun addMobKillCoins(coins: Int) {
        val itemLog = currentLog() ?: return

        itemLog.mobKillCoins += coins
        update()
    }

    private fun addItemPickup(internalName: String, stackSize: Int) {
        val itemLog = currentLog() ?: return

        val slayerItemProfit =
            itemLog.items.getOrPut(internalName) { Storage.ProfileSpecific.SlayerProfitList.SlayerItemProfit() }

        slayerItemProfit.timesDropped++
        slayerItemProfit.totalAmount += stackSize
        update()
    }

    private fun currentLog(): Storage.ProfileSpecific.SlayerProfitList? {
        if (itemLogCategory == "") return null

        val profileSpecific = ProfileStorageData.profileSpecific ?: return null

        return profileSpecific.slayerProfitData.getOrPut(itemLogCategory) {
            Storage.ProfileSpecific.SlayerProfitList()
        }
    }

    @SubscribeEvent
    fun onQuestComplete(event: SlayerQuestCompleteEvent) {
        val itemLog = currentLog() ?: return

        itemLog.slayerCompletedCount++
        update()
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        if (!isEnabled()) return
        if (!SlayerAPI.isInSlayerArea) return

        val packet = event.packet
        if (packet !is S0DPacketCollectItem) return

        val entityID = packet.collectedItemEntityID
        val item = Minecraft.getMinecraft().theWorld.getEntityByID(entityID) ?: return
        if (item !is EntityItem) return

        if (collectedCache.getIfPresent(entityID) != null) return
        collectedCache.put(entityID, Unit)

        val itemStack = item.entityItem
        val name = itemStack.name ?: return
        if (SlayerAPI.ignoreSlayerDrop(name)) return
        val internalName = itemStack.getInternalName()
        if (internalName == "") return

        val (itemName, price) = SlayerAPI.getItemNameAndPrice(itemStack)
        addItemPickup(internalName, itemStack.stackSize)
        logger.log("Coins gained for picking up an item ($itemName) ${price.addSeparators()}")
        if (config.priceInChat) {
            if (config.minimumPrice < price) {
                LorenzUtils.chat("§e[SkyHanni] §a+Slayer Drop§7: §r$itemName")
            }
        }
    }

    fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList<List<Any>> {
        val itemLog = currentLog() ?: return@buildList

        addAsSingletonList("§e§l$itemLogCategory Profit Tracker")
        var profit = 0.0
        val map = mutableMapOf<Renderable, Long>()
        for ((internalName, itemProfit) in itemLog.items) {
            val amount = itemProfit.totalAmount

            val price = (getPrice(internalName) * amount).toLong()

            var name = SlayerAPI.getNameWithEnchantmentFor(internalName) ?: internalName
            val priceFormat = NumberUtil.format(price)
            val hidden = itemProfit.hidden
            if (hidden) {
                name = StringUtils.addFormat(name, "§m")
            }
            val text = " §7${amount.addSeparators()}x $name§7: §6$priceFormat"

            val timesDropped = itemProfit.timesDropped
            val percentage = timesDropped.toDouble() / itemLog.slayerCompletedCount
            val perBoss = LorenzUtils.formatPercentage(percentage.coerceAtMost(1.0))


            val renderable = if (inventoryOpen) Renderable.clickAndHover(
                text, listOf(
                    "§7Dropped §e$timesDropped §7times.",
                    "§7Your drop rate: §c$perBoss",
                    "",
                    "§eClick to " + (if (hidden) "show" else "hide") + "!"
                )
            ) {
                if (System.currentTimeMillis() > lastClickDelay + 150) {
                    lastClickDelay = System.currentTimeMillis()
                    itemProfit.hidden = !hidden
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
                "§7Bosses killed: §e$slayerCompletedCount",
                listOf("§7You killed the $itemLogCategory boss", "§e$slayerCompletedCount §7times.")
            )
        )

        profit += mobKillCoins
        val profitFormat = NumberUtil.format(profit)
        val profitPrefix = if (profit < 0) "§c" else "§6"

        val profitPerBoss = profit / itemLog.slayerCompletedCount
        val profitPerBossFormat = NumberUtil.format(profitPerBoss)

        val text = "§eTotal Profit: $profitPrefix$profitFormat"
        addAsSingletonList(Renderable.hoverTips(text, listOf("§7Profit per boss: $profitPrefix$profitPerBossFormat")))

        if (inventoryOpen) {
            addSelector(" ", PriceSource.values(),
                getName = { type -> type.displayName },
                isCurrent = { it.ordinal == config.priceFrom },
                onChange = {
                    config.priceFrom = it.ordinal
                    update()
                })
        }
    }

    private fun getPrice(internalName: String): Double {
        val bazaarData = BazaarApi.getBazaarDataByInternalName(internalName)
        return bazaarData?.let { getPrice(it) } ?: NEUItems.getPrice(internalName)
    }

    private fun getPrice(bazaarData: BazaarData): Double {
        return when (config.priceFrom) {
            0 -> bazaarData.sellPrice
            1 -> bazaarData.buyPrice

            else -> bazaarData.npcPrice
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (!SlayerAPI.isInSlayerArea) return

        val currentlyOpen = Minecraft.getMinecraft().currentScreen is GuiInventory
        if (inventoryOpen != currentlyOpen) {
            inventoryOpen = currentlyOpen
            update()
        }


        config.pos.renderStringsAndItems(display, posLabel = "Slayer Item Profit Tracker")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
