package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.SlayerQuestCompleteEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.slayer.SlayerChangeEvent
import at.hannibal2.skyhanni.events.slayer.SlayerProgressChangeEvent
import at.hannibal2.skyhanni.features.misc.IslandAreas
import at.hannibal2.skyhanni.features.slayer.SlayerType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeLimitedCache
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.nextAfter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SlayerApi {

    private val trackerConfig get() = SkyHanniMod.feature.slayer.itemProfitTracker
    private val nameCache = TimeLimitedCache<Pair<NeuInternalName, Int>, Pair<String, Double>>(1.minutes)

    var questStartTime = SimpleTimeMark.farPast()
    var isInCorrectArea = false
    var isInAnyArea = false
    var latestSlayerCategory = ""
    var latestWrongAreaWarning = SimpleTimeMark.farPast()
    var latestSlayerProgress = ""

    val currentAreaType by RecalculatingValue(500.milliseconds) {
        checkSlayerTypeForCurrentArea()
    }

    fun hasActiveSlayerQuest() = latestSlayerCategory != ""

    fun getItemNameAndPrice(internalName: NeuInternalName, amount: Int): Pair<String, Double> =
        nameCache.getOrPut(internalName to amount) {
            val amountFormat = if (amount != 1) "§7${amount}x §r" else ""
            val displayName = internalName.repoItemName

            val price = internalName.getPrice()
            val npcPrice = internalName.getNpcPriceOrNull() ?: 0.0
            val maxPrice = npcPrice.coerceAtLeast(price)
            val totalPrice = maxPrice * amount

            val format = totalPrice.shortFormat()

            if (internalName == NeuInternalName.SKYBLOCK_COIN) {
                "§6$format coins" to totalPrice
            } else {
                val priceFormat = " §7(§6$format coins§7)"
                "$amountFormat$displayName$priceFormat" to totalPrice
            }
        }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Slayer")

        if (!hasActiveSlayerQuest()) {
            event.addIrrelevant("no active slayer quest")
            return
        }

        event.addData {
            add("activeSlayer: $activeSlayer")
            add("isInCorrectArea: $isInCorrectArea")
            add("isInAnyArea: $isInAnyArea")
            add("latestSlayerProgress: ${latestSlayerProgress.removeColor()}")
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent) {
        if (event.message.contains("§r§5§lSLAYER QUEST STARTED!")) {
            questStartTime = SimpleTimeMark.now()
        }

        if (event.message == "  §r§a§lSLAYER QUEST COMPLETE!") {
            SlayerQuestCompleteEvent.post()
        }
    }

    val activeSlayer by RecalculatingValue(1.seconds) {
        grabActiveSlayer()
    }

    private fun grabActiveSlayer(): SlayerType? {
        for (line in ScoreboardData.sidebarLinesFormatted) {
            SlayerType.getByName(line)?.let {
                return it
            }
        }

        return null
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(event: SkyHanniTickEvent) {
        // wait with sending SlayerChangeEvent until profile is detected
        if (ProfileStorageData.profileSpecific == null) return

        val slayerQuest = ScoreboardData.sidebarLinesFormatted.nextAfter("Slayer Quest").orEmpty()
        if (slayerQuest != latestSlayerCategory) {
            val old = latestSlayerCategory
            latestSlayerCategory = slayerQuest
            SlayerChangeEvent(old, latestSlayerCategory).post()
        }

        val slayerProgress = ScoreboardData.sidebarLinesFormatted.nextAfter("Slayer Quest", 2).orEmpty()
        if (latestSlayerProgress != slayerProgress) {
            SlayerProgressChangeEvent(latestSlayerProgress, slayerProgress).post()
            latestSlayerProgress = slayerProgress
        }

        if (event.isMod(5)) {
            if (LorenzUtils.isStrandedProfile) {
                isInAnyArea = true
                isInCorrectArea = true
            } else {
                val area = currentAreaType
                isInAnyArea = area != null
                isInCorrectArea = area == activeSlayer && area != null
            }
        }
    }

    // TODO USE SH-REPO
    private fun checkSlayerTypeForCurrentArea() = when (IslandAreas.currentAreaName) {
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

        in listOf(
            "The End",
            "Void Sepulture",
            "Zealot Bruiser Hideout",
        ).let {
            if (trackerConfig.voidgloomInNest) it + "Dragon's Nest" else it
        },
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
