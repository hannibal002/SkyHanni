package at.hannibal2.skyhanni.features.event.harvestfeast

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.SkyHanniMod.launchCoroutine
import at.hannibal2.skyhanni.api.EliteDevApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.enums.SharePolicy
import at.hannibal2.skyhanni.data.ElectionApi
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteFeastData
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteFeastJson
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenApi.getItemStackCopy
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlinx.coroutines.sync.Mutex
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import java.awt.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object HarvestFeastManager {
    private val patternGroup = RepoPattern.group("event.harvestfeast")
    private val profileStorage get() = SkyHanniMod.feature.storage.harvestFeastStorage
    private val config get() = SkyHanniMod.feature.event.feast

    private const val MONTH_MIDDLE_DAY = 18
    private val CURRENT_CROPS_SLOTS = listOf(12, 13, 14)
    private val ALL_CROPS_SLOTS = 27..44
    private val isCurrentOutdated get() = isOutdated(currentFeastData) && isDataAvailable()

    private val mainMenuInventoryDetector by lazy { InventoryDetector(feastInventoryPattern) }
    private val allCropsInventoryDetector by lazy { InventoryDetector(allCropsInventoryPattern) }

    private var currentFeastData: EliteFeastData? = null
        set(value) {
            field = value
            saveDataToStorage(value)
            displayDirty = true
        }
    private var displayDirty = false
    private var fetchedFromElite = false
    private var lastFetched = SimpleTimeMark.farPast()
    private var lastSubmit: Pair<Int, Int>? = null
    private var display: Renderable? = null

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
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Harvest Feast Data")

        if (currentFeastData == null) {
            val now = SkyBlockTime.now()
            event.addIrrelevant("Harvest Feast data is null for year ${now.year} and month ${now.month}.")
        }

        // TODO: Add more debug
    }

    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        if (!SkyBlockUtils.inSkyBlock) return

        if (displayDirty) updateDisplay()
        fetch()
    }

    @HandleEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!mainMenuInventoryDetector.isInside()) return
        if (!isCurrentOutdated) return
        event.container.slots.find { it.item.hoverName.string.removeColor().contains("all crops", ignoreCase = true) }
            ?.highlight(Color(255, 100, 100, 100))
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!allCropsInventoryDetector.isInside()) return
        readAllCrops(event.inventoryItems)
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        currentFeastData = profileStorage.storedHarvestFeastData.takeUnless { isOutdated(it) }
        lastSubmit = profileStorage.lastHarvestFeastSubmitYear
            .takeIf { it > 0 }
            ?.let { it to profileStorage.lastHarvestFeastSubmitMonth }
    }

    private fun readAllCrops(items: Map<Int, ItemStack>) {
        val current = readCurrentActiveCrops(items).takeIf { it.size == 3 } ?: return
        val next = readCropTimestamps(items)

        val sendData = EliteFeastJson.of(
            current = current.map { it.cropName },
            next = next.map { it.key.cropName to it.value }.toMap(),
            isGrandFeast = assumeGrandFeast(),
        )

        currentFeastData = sendData.createData().takeIf { it.complete } ?: return
        
        if (config.sharePolicy == SharePolicy.DISABLED) return

        if (config.sharePolicy == SharePolicy.ASK) {
            ChatUtils.clickableChat(
                "§2Click Here to submit the current Harvest Feast data. Thank you for sharing!",
                onClick = { trySubmitData(sendData) },
                "§eClick to submit!",
                oneTimeClick = true,
            )
        } else trySubmitData(sendData)
    }

    private fun trySubmitData(data: EliteFeastJson) {
        if (alreadySubmittedThisSkyBlockMonth()) return
        if (sendingFeastDataMutex.isLocked) {
            ChatUtils.chat { append("You are already submitting data for this Harvest Feast.").withColor(0xFFFF5555.toInt()) }
            return
        }
        CoroutineSettings("submit harvest feast data").withIOContext().withMutex(sendingFeastDataMutex).launchCoroutine {
            if (alreadySubmittedThisSkyBlockMonth()) return@launchCoroutine
            val res = EliteDevApi.submitHarvestFeast(data)
            if (res.success) {
                ChatUtils.chat { append("Successfully submitted harvest feast data. Thank you for sharing!").withColor(0xFF55FF55.toInt()) }

                if (config.sharePolicy == SharePolicy.ASK && !profileStorage.harvestFeastSendingAsked) {
                    ChatUtils.clickableChat(
                        "§2Do you want to automatically submit Harvest Feast data in the future? Click here!",
                        onClick = {
                            config.sharePolicy = SharePolicy.AUTO
                            profileStorage.harvestFeastSendingAsked = true
                            ChatUtils.chat { append("You are now sharing Harvest Feast data automatically.").withColor(0xFF55FF55.toInt()) }
                        },
                        hover = "§eClick to share automatically!",
                        oneTimeClick = true,
                    )
                }
                val now = SkyBlockTime.now()
                lastSubmit = now.year to now.month
                profileStorage.lastHarvestFeastSubmitYear = now.year
                profileStorage.lastHarvestFeastSubmitMonth = now.month
            } else ErrorManager.logErrorStateWithData(
                "Failed to upload Harvest Feast data to EliteSkyBlock. If this happens again, please report this in the Discord!",
                "failed to upload harvest feast data",
                "data" to data,
                "response" to res,
            )
        }
    }

    private fun alreadySubmittedThisSkyBlockMonth(): Boolean {
        val now = SkyBlockTime.now()
        return lastSubmit == (now.year to now.month)
    }

    private fun readCurrentActiveCrops(stacks: Map<Int, ItemStack>): List<CropType> {
        val stacks = stacks.filterKeys { it in CURRENT_CROPS_SLOTS }
        val current = stacks.mapNotNull { CropType.getByNameOrNull(it.value.hoverName.string.removeColor()) }

        if (current.size != 3) {
            ErrorManager.logErrorStateWithData(
                "Error reading current Harvest Feast crops.",
                "current harvest feast crops not 3",
                "current crops" to current,
            )
        }

        return current
    }

    private fun readCropTimestamps(items: Map<Int, ItemStack>): Map<CropType, SimpleTimeMark?> {
        val outputMap = CropType.entries.associateWith { null }.toMutableMap<CropType, SimpleTimeMark?>()

        items.filterKeys { it in ALL_CROPS_SLOTS }.forEach { (_, stack) ->
            val crop = CropType.getByNameOrNull(stack.hoverName.string.removeColor())
            val lore = stack.getLoreComponent().map { it.string.removeColor() }
            willBeInSeasonPattern.firstMatcher(lore) {
                groupOrNull("time")?.let { timeStr ->
                    val time = TimeUtils.getDurationOrNull(timeStr)
                    if (crop != null && time != null) {
                        outputMap[crop] = getTimeStamp(time)
                    }
                }
            }
            outOfSeasonPattern.firstMatcher(lore) {
                if (crop != null) {
                    outputMap[crop] = null
                }
            }
        }

        return outputMap
    }

    private fun assumeGrandFeast(): Boolean {
        val mayorGrandFeast = ElectionApi.currentMayor?.let { Perk.GRAND_FEAST in it.perks } ?: false
        val ministerGrandFeast = ElectionApi.currentMinister?.let { Perk.GRAND_FEAST in it.perks } ?: false
        val timeBasedGrandFeast = currentFeastData?.let { it.month !in 7..9 && it.year == SkyBlockTime.now().year && it.current.isNotEmpty() } ?: false
        return mayorGrandFeast || ministerGrandFeast || timeBasedGrandFeast
    }

    private fun getTimeStamp(time: Duration): SimpleTimeMark {
        val starting = SkyBlockTime.fromTimeMark(SimpleTimeMark.now() + time)
        return (SkyBlockTime.SKYBLOCK_EPOCH_START_MILLIS +
            SkyBlockTime.SKYBLOCK_YEAR_MILLIS * starting.year +
            (SkyBlockTime.SKYBLOCK_MONTH_MILLIS * (starting.month - if (starting.day < MONTH_MIDDLE_DAY) 1 else 0))).asTimeMark()
    }

    private fun fetch() {
        if (!config.fetchAutomatically) return
        if (!isCurrentOutdated) return
        if (lastFetched.passedSince() < 10.minutes) return
        if (fetchingFeastDataMutex.isLocked) return

        CoroutineSettings("harvest feast data fetch").withIOContext().withMutex(fetchingFeastDataMutex).launchCoroutine {
            currentFeastData = EliteDevApi.fetchHarvestFeastData().takeIf { it.complete && !isOutdated(it) }
            handleFetchedFeastData()
            lastFetched = SimpleTimeMark.now()
            displayDirty = true
        }
    }

    private fun handleFetchedFeastData() {
        if (isCurrentOutdated) {
            ChatUtils.chat { append("Harvest feast data is not yet available.\n" +
                "Talk to the Feast Chef Ted in the Hub or on your Garden to fill it in!").withColor(0xFFFF5555.toInt()) }
        } else {
            ChatUtils.debug("Loaded Harvest Feast Data for year ${currentFeastData?.year}, month ${currentFeastData?.month}.")
            fetchedFromElite = true
        }
    }

    private fun saveDataToStorage(data: EliteFeastData? = currentFeastData) {
        profileStorage.storedHarvestFeastData = data
    }

    private fun isOutdated(data: EliteFeastData?): Boolean {
        val data = data ?: return true
        val now = SkyBlockTime.now()
        return data.year < now.year ||
            // Accept data from previous month as well since the next data is always available for at least the next 2-3 months
            // no reason to invalidate only 1 month outdated data
            data.month < (now.month - 1) ||
            data.current.isEmpty()
    }

    private fun isDataAvailable(): Boolean {
        val now = SkyBlockTime.now()
        return now.month in 7..9 || assumeGrandFeast() || (!isOutdated(currentFeastData) && currentFeastData?.isGrandFeast == true)
    }

    private fun resetData() {
        currentFeastData = null
        profileStorage.storedHarvestFeastData = null
        lastFetched = SimpleTimeMark.farPast()
        fetchedFromElite = false
        lastSubmit = null
        profileStorage.lastHarvestFeastSubmitYear = -1
        profileStorage.lastHarvestFeastSubmitMonth = -1
    }

    private fun updateDisplay() {
        display = if (fetchingFeastDataMutex.isLocked) {
            Renderable.text(Component.literal("Fetching Harvest Feast data...").withColor(0xFFFFFF55.toInt()))
        } else {
            Renderable.horizontal {
                if (!isCurrentOutdated) return@horizontal renderDisplay()

                addString("§cFeast data is outdated!")
            }
        }
    }

    private fun MutableList<Renderable>.renderDisplay() {
        val data = currentFeastData ?: return

        addString("§aIn-season: ")

        val duration = data.getActiveDuration()
        val endStamp = SimpleTimeMark.now() + duration

        data.getCurrentCrops().forEach { crop ->
            val cropStack = crop.getItemStackCopy("active_feast_crop:$crop-$endStamp")
            add(
                Renderable.item(cropStack) {
                    scale = 1.0
                }
            )
        }

        addString("§7(§b${duration.format()}§7)")
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onGuiRenderOverlay() {
        if (!config.displayCurrentCrops) return
        if (!SkyBlockUtils.inSkyBlock) return
        if (!GardenApi.inGarden() && !isCurrentOutdated) return
        if (!isDataAvailable()) return
        val display = display ?: return
        config.position.renderRenderable(display, posLabel = "Current Active Crops")
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcopyfeastdata") {
            description = "Copies the current harvest feast data"
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback {
                CoroutineSettings("copy feast data to clipboard").withIOContext().launchCoroutine {
                    ClipboardUtils.copyToClipboardAsync(currentFeastData?.getBody().toString()).await() ?: return@launchCoroutine
                    ChatUtils.chat("Copied harvest feast debug data to clipboard.")
                }
            }
        }
        event.registerBrigadier("shresetfeastdata") {
            description = "Resets current Harvest Feast data"
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback {
                resetData()
                ChatUtils.chat("Reset Harvest Feast data.")
            }
        }
    }
}
