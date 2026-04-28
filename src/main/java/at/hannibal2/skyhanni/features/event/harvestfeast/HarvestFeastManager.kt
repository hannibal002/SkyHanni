package at.hannibal2.skyhanni.features.event.harvestfeast

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.SkyHanniMod.launchCoroutine
import at.hannibal2.skyhanni.api.EliteDevApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.ElectionApi
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteFeastData
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteFeastJson
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlinx.coroutines.sync.Mutex
import net.minecraft.world.item.ItemStack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object HarvestFeastManager {
    private val patternGroup = RepoPattern.group("event.harvestfeast")
    private val profileStorage get() = SkyHanniMod.feature.storage
    private val config get() = SkyHanniMod.feature.event.feast

    private val CURRENT_CROPS_SLOTS = listOf(13, 14, 15)
    private val ALL_CROPS_SLOTS = 27..44
    private val isCurrentOutdated get() = isOutdated(currentFeastData) && isDataAvailable()

    private val feastInventoryDetector by lazy { InventoryDetector(feastInventoryPattern) }
    private val allCropsInventoryDetector by lazy { InventoryDetector(allCropsInventoryPattern) }

    private var currentFeastData: EliteFeastData? = null
    private var fetchedFromElite = false
    private var lastFetched = SimpleTimeMark.farPast()

    private val fetchingFeastDataMutex = Mutex()
    private val sendingFeastDataMutex = Mutex()

    /**
     * REGEX-TEST: Harvest Feast
     * REGEX-TEST: Grand Feast
     */
    private val feastInventoryPattern by patternGroup.pattern(
        "main.menu",
        "(?:Harvest|(?<grand>Grand)) Feast",
    )

    /**
     * REGEX-TEST: All Crops
     */
    private val allCropsInventoryPattern by patternGroup.pattern(
        "main.allcrops",
        "All Crops",
    )

    /**
     * REGEX-TEST: Out of season until the next Feast!
     */
    private val outOfSeasonPattern by patternGroup.pattern(
        "crop.outofseason",
        "Out of season until the next Feast!",
    )

    /**
     * REGEX-TEST: Will be in-season in 20h!
     * REGEX-TEST: Will be in-season in 13m!
     */
    private val willBeInSeasonPattern by patternGroup.pattern(
        "crop.willbe",
        "Will be in-season in (?<time>.+)!",
    )

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Harvest Feast Data")

        if (currentFeastData == null) {
            val now = SkyBlockTime.now()
            event.addIrrelevant("Harvest Feast data is null for year ${now.year} and month ${now.month}.")
        }

        // TODO: Add more debug
    }

    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        if (feastInventoryDetector.isInside()) return
        fetch()
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!allCropsInventoryDetector.isInside()) return
        readAllCrops(event.inventoryItems)
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        currentFeastData = profileStorage.storedHarvestFeastData.takeUnless { isOutdated(it) }
    }

    private fun readAllCrops(items: Map<Int, ItemStack>) {
        val current = readCurrentActiveCrops(items)//.takeIf { it.size == 3 } ?: return
        val next = readCropTimestamps(items, current)

        val sendData = EliteFeastJson(
            current = current.map { it.cropName },
            next = next.map { it.key.cropName to it.value }.toMap(),
            isGrandFeast = assumeGrandFeast(),
        )
        println(ConfigManager.gson.toJson(sendData))
    }

    private fun readCurrentActiveCrops(stacks: Map<Int, ItemStack>): List<CropType> {
        val stacks = stacks.filterKeys { it in CURRENT_CROPS_SLOTS }
        val current = stacks.mapNotNull { CropType.getByNameOrNull(it.value.hoverName.string.removeColor() ) }

        if (current.size != 3) {
            ErrorManager.logErrorStateWithData(
                "Error reading current Harvest Feast crops.",
                "current harvest feast crops not 3",
                "current crops" to current,
            )
        }

        return current
    }

    private fun readCropTimestamps(items: Map<Int, ItemStack>, inSeason: List<CropType?>): Map<CropType, Long?> {
        val outputMap = CropType.entries.associateWith { null }.toMutableMap<CropType, Long?>()

        items.filterKeys { it in ALL_CROPS_SLOTS }.mapNotNull { entry ->
            val crop = CropType.getByNameOrNull(entry.value.hoverName.string.removeColor())
            println("Got crop $crop")
            val lore = entry.value.getLoreComponent().map { it.string.removeColor() }
            willBeInSeasonPattern.firstMatcher(lore) {
                groupOrNull("time")?.let { timeStr ->
                    println("Got time for $crop ($timeStr)")
                    val time = TimeUtils.getDurationOrNull(timeStr)
                    if (crop != null && time != null) {
                        outputMap[crop] = getTimeStamp(time)
                    }
                }
            }
            outOfSeasonPattern.firstMatcher(lore) {
                println("Got no feast time")
                if (crop != null) {
                    outputMap[crop] = null
                }
            }
        }

        val now = SkyBlockTime.now()
        val stamp = SkyBlockTime.SKYBLOCK_EPOCH_START_MILLIS + SkyBlockTime.SKYBLOCK_YEAR_MILLIS * now.year + SkyBlockTime.SKYBLOCK_MONTH_MILLIS * now.month
        inSeason.filterNotNull().forEach {
            outputMap[it] = stamp
        }

        println(outputMap)

        return outputMap
    }

    private fun assumeGrandFeast(): Boolean {
        val mayorGrandFeast = ElectionApi.currentMayor?.let { Perk.GRAND_FEAST in it.perks } ?: false
        val ministerGrandFeast = ElectionApi.currentMinister?.let { Perk.GRAND_FEAST in it.perks } ?: false
        val timeBasedGrandFeast = currentFeastData?.let { it.month !in 7..9 && it.year == SkyBlockTime.now().year && it.current.isNotEmpty() } ?: false
        return mayorGrandFeast || ministerGrandFeast || timeBasedGrandFeast
    }

    private fun getTimeStamp(time: Duration): Long {
        val starting = SkyBlockTime.fromTimeMark(SimpleTimeMark.now() + time)
        return SkyBlockTime.SKYBLOCK_EPOCH_START_MILLIS +
            SkyBlockTime.SKYBLOCK_YEAR_MILLIS * starting.year +
            (SkyBlockTime.SKYBLOCK_MONTH_MILLIS * (starting.month - if (starting.day < 21) 1 else 0))
    }

    private fun fetch() {
        if (!config.fetchAutomatically) return
        if (!isCurrentOutdated) return
        if (lastFetched.passedSince() < 10.minutes) return

        CoroutineSettings("harvest feast data fetch").withIOContext().withMutex(fetchingFeastDataMutex).launchCoroutine {
            val data = EliteDevApi.fetchHarvestFeastData()
            println(ConfigManager.gson.toJson(data))
            currentFeastData = data.takeIf { it.complete && !isOutdated(it) }
            handleFeastData()
            lastFetched = SimpleTimeMark.now()
        }
    }

    private fun handleFeastData() {
        if (isCurrentOutdated) {
            ChatUtils.chat("Current Harvest Feast Data could not be loaded.")
        } else {
            ChatUtils.debug("Loaded Harvest Feast Data for year ${currentFeastData?.year}, month ${currentFeastData?.month}.")
            fetchedFromElite = true
            saveDataToStorage()
        }
    }

    private fun saveDataToStorage() {
        profileStorage.storedHarvestFeastData = currentFeastData
    }

    private fun isOutdated(data: EliteFeastData?): Boolean {
        val data = data ?: return true
        val now = SkyBlockTime.now()
        return data.year < now.year ||
            data.month < now.month ||
            data.current.isEmpty()
    }

    private fun isDataAvailable(): Boolean {
        val now = SkyBlockTime.now()
        return now.month in 7..9 || assumeGrandFeast()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shprintcurrentfeastdata") {
            description = "Prints the current feast data to chat"
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback {

                val builder = StringBuilder()
                builder.append("profile feast data(outdated: ${isOutdated(profileStorage.storedHarvestFeastData)}): ")
                builder.appendLine(ConfigManager.gson.toJson(profileStorage.storedHarvestFeastData))
                builder.append("current feast data (outdated: $isCurrentOutdated): ")
                builder.appendLine(ConfigManager.gson.toJson(currentFeastData))
                builder.appendLine("fetched from elite: $fetchedFromElite")
                builder.appendLine("last fetched: ${lastFetched.passedSince()}")

                ChatUtils.clickableChat("Click to copy data to clipboard.", onClick = {
                    CoroutineSettings("copy feast data to clipboard").withIOContext().launchCoroutine {
                        ClipboardUtils.copyToClipboardAsync(builder.toString()).await()
                    }
                })
            }
        }
    }
}
