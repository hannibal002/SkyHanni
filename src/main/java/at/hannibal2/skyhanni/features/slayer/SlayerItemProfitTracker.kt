package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.Storage.ProfileSpecific.SlayerProfitList
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.events.SlayerChangeEvent
import at.hannibal2.skyhanni.events.SlayerQuestCompleteEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.ProfitListWrapper
import at.hannibal2.skyhanni.utils.ProfitTracker
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
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
    private var display = emptyList<List<Any>>()
    private val logger = LorenzLogger("slayer/item_profit_tracker")
    private var currentSessionData = mutableMapOf<String, SlayerProfitList>()

    private fun addSlayerCosts(price: Int) {
        val itemLog = currentLog() ?: return
        itemLog.modify {
            it.slayerSpawnCost += price
        }
        updateDisplay()
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
        updateDisplay()
    }

    private fun addMobKillCoins(coins: Int) {
        val itemLog = currentLog() ?: return

        itemLog.modify {
            it.mobKillCoins += coins
        }
        updateDisplay()
    }

    private fun addItemPickup(internalName: NEUInternalName, stackSize: Int) {
        val itemLog = currentLog() ?: return

        itemLog.modify {
            val slayerItemProfit = it.items.getOrPut(internalName) { SlayerProfitList.ProfitTrackerItem() }

            slayerItemProfit.timesDropped++
            slayerItemProfit.totalAmount += stackSize
        }

        updateDisplay()
    }

    class SlayerWrapper<T : SlayerProfitList>(val list: T) : ProfitListWrapper<T>() {
        override fun reset() {
            list.items.clear()
            list.mobKillCoins = 0
            list.slayerSpawnCost = 0
            list.slayerCompletedCount = 0
        }

        override fun getList(): Map<NEUInternalName, SlayerProfitList.ProfitTrackerItem> {
            return list.items
        }

        override fun getType(): T = list

    }

    class AbstractSlayerProfitList(
        total: SlayerWrapper<SlayerProfitList>,
        currentSession: SlayerWrapper<SlayerProfitList>,
        update: () -> Unit,
    ) : ProfitTracker.AbstractProfitList<SlayerProfitList>(total, currentSession, update)

    private fun currentLog(): AbstractSlayerProfitList? {
        if (itemLogCategory == "") return null

        val profileSpecific = ProfileStorageData.profileSpecific ?: return null

        val a = profileSpecific.slayerProfitData.getOrPut(itemLogCategory) { SlayerProfitList() }
        val b = currentSessionData.getOrPut(itemLogCategory) { SlayerProfitList() }
        val update = { updateDisplay() }

        return AbstractSlayerProfitList(SlayerWrapper(a), SlayerWrapper(b), update)
    }

    @SubscribeEvent
    fun onQuestComplete(event: SlayerQuestCompleteEvent) {
        val itemLog = currentLog() ?: return

        itemLog.modify {
            it.slayerCompletedCount++
        }

        updateDisplay()
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        if (!isEnabled()) return
        if (!SlayerAPI.isInSlayerArea) return
        if (!SlayerAPI.hasActiveSlayerQuest()) return

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
        val internalName = itemStack.getInternalNameOrNull() ?: return

        val (itemName, price) = SlayerAPI.getItemNameAndPrice(itemStack)
        addItemPickup(internalName, itemStack.stackSize)
        logger.log("Coins gained for picking up an item ($itemName) ${price.addSeparators()}")
        if (config.priceInChat) {
            if (price > config.minimumPrice) {
                LorenzUtils.chat("§e[SkyHanni] §a+Slayer Drop§7: §r$itemName")
            }
        }
        if (config.titleWarning) {
            if (price > config.minimumPriceWarning) {
                TitleUtils.sendTitle("§a+ $itemName", 5.seconds)
            }
        }
    }

    fun updateDisplay() {
        val currentLog = currentLog() ?: return
        display = currentLog.drawDisplay("§e§l${itemLogCategory} Profit Tracker")
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (!SlayerAPI.isInSlayerArea) return

        val currentlyOpen = Minecraft.getMinecraft().currentScreen is GuiInventory
        if (inventoryOpen != currentlyOpen) {
            inventoryOpen = currentlyOpen
            updateDisplay()
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

        if (args.size == 1) {
            if (args[0].lowercase() == "confirm") {
                resetData(DisplayMode.TOTAL)
                updateDisplay()
                LorenzUtils.chat("§e[SkyHanni] You reset your $itemLogCategory slayer data!")
                return
            }
        }

        LorenzUtils.clickableChat(
                "§e[SkyHanni] Are you sure you want to reset all your $itemLogCategory slayer data? Click here to confirm.",
                "shclearslayerprofits confirm"
        )
    }
}
