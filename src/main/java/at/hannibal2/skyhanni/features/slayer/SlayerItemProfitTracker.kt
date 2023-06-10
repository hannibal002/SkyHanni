package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.common.cache.CacheBuilder
import net.minecraft.client.Minecraft
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
    private var itemLogs = mapOf<String, ItemLog>()
    private val logger = LorenzLogger("slayer_item_profit_tracker")

    private fun addSlayerCosts(price: Double) {
        val itemLog = currentLog() ?: return

        val name = "Slayer Spawn Costs"
        itemLog.items = itemLog.items.editCopy {
            val (oldCoins, oldAmount) = getOrDefault(name, 0.0 to 0)
            this[name] = oldCoins + price to oldAmount + 1
        }
        update()
    }

    @SubscribeEvent
    fun onPurseChange(event: PurseChangeEvent) {
        if (!isEnabled()) return
        val coins = event.coins
        if (event.reason == PurseChangeCause.GAIN_MOB_KILL) {
            if (SlayerAPI.isInSlayerArea) {
                logger.log("Coins gained for killing mobs: ${coins.addSeparators()}")
                addMobKillCoins(coins)
            }
        }
        if (event.reason == PurseChangeCause.LOSE_SLAYER_QUEST_STARTED) {
            logger.log("Coins paid for starting slayer quest: ${coins.addSeparators()}")
            addSlayerCosts(coins)
        }
    }

    @SubscribeEvent
    fun onSlayerChange(event: SlayerChangeEvent) {
        val newSlayer = event.newSlayer
        itemLogCategory = if (newSlayer == "") {
            ""
        } else {
            newSlayer.split(" ").dropLast(1).joinToString(" ")
        }
        update()
    }

    private fun addMobKillCoins(coins: Double) {
        val itemLog = currentLog() ?: return

        itemLog.mobKillCoins += coins
        update()
    }

    private fun addItemPickup(totalPrice: Double, displayName: String, stackSize: Int) {
        val itemLog = currentLog() ?: return

        itemLog.items = itemLog.items.editCopy {
            val (oldCoins, oldAmount) = getOrDefault(displayName, 0.0 to 0)
            this[displayName] = oldCoins + totalPrice to oldAmount + stackSize
        }
        update()
    }

    private fun currentLog(): ItemLog? {
        if (itemLogCategory == "") return null

        itemLogs[itemLogCategory]?.let {
            return it
        }

        val itemLog = ItemLog(itemLogCategory)
        itemLogs = itemLogs.editCopy { this[itemLogCategory] = itemLog }

        return itemLog
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
        val displayName = NEUItems.getItemStack(internalName).nameWithEnchantment ?: "internalName"
        addItemPickup(price, displayName, itemStack.stackSize)
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

        val displayName = itemLog.displayName.removeColor()
        addAsSingletonList("§e§l$displayName Profit Tracker")
        var profit = 0.0
        val map = mutableMapOf<String, Double>()
        for ((name, value) in itemLog.items) {
            val (price, amount) = value
            val profitPrefix = if (price < 0) "§c" else "§6"
            val priceFormat = NumberUtil.format(price)
            map["§7${amount.addSeparators()}x $name§7: $profitPrefix$priceFormat"] = price
            profit += price
        }
        val mobKillCoins = itemLog.mobKillCoins
        if (mobKillCoins != 0.0) {
            val mobKillCoinsFormat = NumberUtil.format(mobKillCoins)
            map["§7Mob kill coins: §6$mobKillCoinsFormat"] = mobKillCoins
        }

        for (text in map.sortedDesc().keys) {
            addAsSingletonList(" $text")
        }

        profit += mobKillCoins
        val profitFormat = NumberUtil.format(profit)
        val profitPrefix = if (profit < 0) "§c" else "§6"
        addAsSingletonList("§eTotal Profit: $profitPrefix$profitFormat")
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (!SlayerAPI.isInSlayerArea) return

        config.pos.renderStringsAndItems(display, posLabel = "Slayer Item Profit Tracker")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled

    class ItemLog(val displayName: String) {
        // display name -> (totalCoins, amount)
        var items = mapOf<String, Pair<Double, Int>>()
        var mobKillCoins = 0.0
    }
}
