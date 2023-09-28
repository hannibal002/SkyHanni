package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.SlayerChangeEvent
import at.hannibal2.skyhanni.events.SlayerProgressChangeEvent
import at.hannibal2.skyhanni.events.SlayerQuestCompleteEvent
import at.hannibal2.skyhanni.features.slayer.SlayerType
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.nextAfter
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NEUItems.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.common.cache.CacheBuilder
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.concurrent.TimeUnit

object SlayerAPI {

    private var nameCache =
        CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES)
            .build<Pair<NEUInternalName, Int>, Pair<String, Double>>()

    var questStartTime = 0L
    var isInSlayerArea = false
    var latestSlayerCategory = ""
    private var latestProgressChangeTime = 0L
    var latestWrongAreaWarning = 0L
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
        val displayName = getNameWithEnchantmentFor(internalName)

        val price = internalName.getPrice()
        val npcPrice = internalName.getNpcPriceOrNull() ?: 0.0
        val maxPrice = npcPrice.coerceAtLeast(price)
        val totalPrice = maxPrice * amount

        val format = NumberUtil.format(totalPrice)
        val priceFormat = " §7(§6$format coins§7)"

        val result = "$amountFormat$displayName$priceFormat" to totalPrice
        nameCache.put(key, result)
        return result
    }

    fun getNameWithEnchantmentFor(internalName: NEUInternalName): String {
        if (internalName.asString() == "WISP_POTION") {
            return "§fWisp's Ice-Flavored Water"
        }
        return internalName.getItemStack().nameWithEnchantment ?: error("Could not find name for $internalName")
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (event.message.contains("§r§5§lSLAYER QUEST STARTED!")) {
            questStartTime = System.currentTimeMillis()
        }

        if (event.message == "  §r§a§lSLAYER QUEST COMPLETE!") {
            SlayerQuestCompleteEvent().postAndCatch()
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return

        // wait with sending SlayerChangeEvent until profile is detected
        if (ProfileStorageData.profileSpecific == null) return

        val slayerQuest = ScoreboardData.sidebarLinesFormatted.nextAfter("Slayer Quest") ?: ""
        if (slayerQuest != latestSlayerCategory) {
            val old = latestSlayerCategory
            latestSlayerCategory = slayerQuest
            SlayerChangeEvent(old, latestSlayerCategory).postAndCatch()
        }

        val slayerProgress = ScoreboardData.sidebarLinesFormatted.nextAfter("Slayer Quest", 2) ?: ""
        if (latestSlayerProgress != slayerProgress) {
            SlayerProgressChangeEvent(latestSlayerProgress, slayerProgress).postAndCatch()
            latestSlayerProgress = slayerProgress
            latestProgressChangeTime = System.currentTimeMillis()
        }

        if (event.isMod(5)) {
            isInSlayerArea = if (LorenzUtils.isStrandedProfile) {
                true
            } else {
                SlayerType.getByArea(LorenzUtils.skyBlockArea) != null
            }
        }
    }
}