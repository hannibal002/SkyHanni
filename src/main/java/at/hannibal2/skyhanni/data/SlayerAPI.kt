package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.SlayerChangeEvent
import at.hannibal2.skyhanni.events.SlayerProgressChangeEvent
import at.hannibal2.skyhanni.events.SlayerQuestCompleteEvent
import at.hannibal2.skyhanni.features.slayer.SlayerType
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.nextAfter
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NEUItems.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RecalculatingValue
import com.google.common.cache.CacheBuilder
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

object SlayerAPI {

    private var nameCache =
        CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES)
            .build<Pair<NEUInternalName, Int>, Pair<String, Double>>()

    var questStartTime = 0L
    var isInCorrectArea = false
    var isInAnyArea = false
    var latestSlayerCategory = ""
    private var latestProgressChangeTime = 0L
    var latestWrongAreaWarning = 0L
    private var latestSlayerProgress = ""

    fun hasActiveSlayerQuest() = latestSlayerCategory != ""

    fun getLatestProgressChangeTime() = if (latestSlayerProgress == "§eSlay the boss!") {
        System.currentTimeMillis()
    } else latestProgressChangeTime

    fun getItemNameAndPrice(internalName: NEUInternalName, amount: Int): Pair<String, Double> {
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

    fun getActiveSlayer() = activeSlayer.getValue()

    private val activeSlayer = RecalculatingValue(1.seconds) {
        grabActiveSlayer()
    }

    private fun grabActiveSlayer(): SlayerType? {
        for (line in ScoreboardData.sidebarLinesFormatted) {
            for (type in SlayerType.entries) {
                if (line.contains(type.displayName)) {
                    return type
                }
            }
        }

        return null
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
            isInCorrectArea = if (LorenzUtils.isStrandedProfile) {
                isInAnyArea = true
                true
            } else {
                val slayerTypeForCurrentArea = getSlayerTypeForCurrentArea()
                isInAnyArea = slayerTypeForCurrentArea != null
                slayerTypeForCurrentArea == getActiveSlayer()
            }
        }
    }

    // TODO USE SH-REPO
    fun getSlayerTypeForCurrentArea() = when (LorenzUtils.skyBlockArea) {
        "Graveyard",
        "Coal Mine",
        -> SlayerType.REVENANT

        "Spider Mound",
        "Arachne's Burrow",
        "Arachne's Sanctuary",
        "Burning Desert",
        -> SlayerType.TARANTULA

        "Ruins",
        "Howling Cave",
        -> SlayerType.SVEN

        "The End",
        "Void Sepulture",
        "Zealot Bruiser Hideout",
        -> SlayerType.VOID

        "Stronghold",
        "The Wasteland",
        "Smoldering Tomb",
        -> SlayerType.INFERNO

        "Stillgore Château",
        "Oubliette",
        -> SlayerType.VAMPIRE

        else -> null
    }
}
