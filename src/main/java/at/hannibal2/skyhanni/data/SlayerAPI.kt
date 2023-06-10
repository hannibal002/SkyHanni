package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.SlayerChangeEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.slayer.SlayerType
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.nextAfter
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.common.cache.CacheBuilder
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.concurrent.TimeUnit

object SlayerAPI {

    var tick = 0

    private var nameCache =
        CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build<Pair<String, Int>, Pair<String, Double>>()

    var questStartTime = 0L
    var isInSlayerArea = false
    private var latestSlayerCategory = ""
    private var latestProgressChangeTime = 0L
    private var latestSlayerProgress = ""

    fun hasActiveSlayerQuest() = latestSlayerCategory != ""

    fun getLatestProgressChangeTime() = if (latestSlayerProgress == "§eSlay the boss!") {
        System.currentTimeMillis()
    } else latestProgressChangeTime


    // TODO use repo
    fun ignoreSlayerDrop(name: String) = when (name.removeColor()) {
        // maybe everywhere?
        "Stone" -> true
        "Head" -> true

        // Spider
        "Cobweb" -> true
        "String" -> true
        "Spider Eye" -> true
        "Bone" -> true

        // Blaze
        "Water Bottle" -> true

        else -> false
    }

    fun getItemNameAndPrice(stack: ItemStack): Pair<String, Double> {
        val internalName = stack.getInternalName()
        val amount = stack.stackSize
        val key = internalName to amount
        nameCache.getIfPresent(key)?.let {
            return it
        }

        val amountFormat = if (amount != 1) "§7${amount}x §r" else ""
        val displayName = NEUItems.getItemStack(internalName).nameWithEnchantment

        val price = NEUItems.getPrice(internalName)
        val npcPrice = BazaarApi.getBazaarDataByInternalName(internalName)?.npcPrice ?: 0.0
        val maxPrice = npcPrice.coerceAtLeast(price)
        val totalPrice = maxPrice * amount

        val format = NumberUtil.format(totalPrice)
        val priceFormat = " §7(§6$format coins§7)"

        val result = "$amountFormat$displayName$priceFormat" to totalPrice
        nameCache.put(key, result)
        return result
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (event.message.contains("§r§5§lSLAYER QUEST STARTED!")) {
            questStartTime = System.currentTimeMillis()
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (!LorenzUtils.inSkyBlock) return

        val slayerQuest = ScoreboardData.sidebarLinesFormatted.nextAfter("Slayer Quest") ?: ""
        if (slayerQuest != latestSlayerCategory) {
            SlayerChangeEvent(latestSlayerCategory, slayerQuest).postAndCatch()
            latestSlayerCategory = slayerQuest
        }

        val slayerProgress = ScoreboardData.sidebarLinesFormatted.nextAfter("Slayer Quest", 2) ?: ""
        if (latestSlayerProgress != slayerProgress) {
            latestSlayerProgress = slayerProgress
            latestProgressChangeTime = System.currentTimeMillis()
        }

        if (tick++ % 5 == 0) {
            isInSlayerArea = SlayerType.getByArea(LorenzUtils.skyBlockArea) != null
        }
    }
}