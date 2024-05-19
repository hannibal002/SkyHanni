package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.features.garden.EliteFarmingCollectionConfig.CropDisplay
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.jsonobjects.other.EliteCollectionGraphEntry
import at.hannibal2.skyhanni.data.jsonobjects.other.EliteLeaderboard
import at.hannibal2.skyhanni.data.jsonobjects.repo.EliteAPISettingsJson
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getCropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.toDashlessUUID
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.fromJson
import at.hannibal2.skyhanni.utils.renderables.Renderable
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

object FarmingCollectionDisplay {

    private val config get() = SkyHanniMod.feature.garden.eliteFarmingCollection

    private var checkDuration = 10.minutes
    private var worldSwapRefresh = true

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<EliteAPISettingsJson>("EliteAPISettings")
        checkDuration = data.refreshTime.minutes
        worldSwapRefresh = data.worldSwapRefresh
    }

    private val eliteCollectionApiGson by lazy {
        ConfigManager.createBaseGsonBuilder()
            .registerTypeAdapter(CropType::class.java, object : TypeAdapter<CropType>() {
                override fun write(out: JsonWriter, value: CropType) {}

                override fun read(reader: JsonReader): CropType {
                    val crop = reader.nextString()
                    return CropType.entries.firstOrNull { it.simpleName == crop } ?: error("No valid crop type '$crop'")
                }
            }.nullSafe())
            .create()
    }

    private var profileID: UUID? = null
    private val collectionPlacements = mutableMapOf<CropType, Map<Int, Long>>()
    private val collectionRanks = mutableMapOf<CropType, Int>()
    private var currentCollections = mutableMapOf<CropType, Long>()
    private var lastFetchedCrop: CropType? = null

    private var hasCollectionBeenFetched = false

    private var lastBrokenCrop: CropType?
        get() = SkyHanniMod.feature.storage.lastCropBroken
        set(value) {
            SkyHanniMod.feature.storage.lastCropBroken = value
        }
    private var lastLeaderboardFetch = SimpleTimeMark.farPast()

    private var display = emptyList<Renderable>()

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (GardenAPI.hideExtraGuis()) return
        if (!isEnabled()) return


        config.pos.renderRenderables(display, posLabel = "Farming Collection Display")
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (worldSwapRefresh) {
            resetData()
        }
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.crop.afterChange {
            lastLeaderboardFetch = SimpleTimeMark.farPast()
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (profileID == null) return

        if (lastLeaderboardFetch.passedSince() > checkDuration) {
            lastLeaderboardFetch = SimpleTimeMark.now()
            val crop = if (config.crop.get() == CropDisplay.AUTO) {
                lastBrokenCrop ?: CropType.WHEAT
            } else {
                config.crop.get().crop
            }

            SkyHanniMod.coroutineScope.launch {
                collectionPlacements.clear()
                collectionRanks.clear()
                getRanksForCollection(crop)
            }
        }

        updateDisplay()
    }

    @SubscribeEvent
    fun onBlockClicked(event: BlockClickEvent) {
        if (event.clickType == ClickType.RIGHT_CLICK) return
        val crop = event.getBlockState.getCropType() ?: return
        if (!collectionRanks.containsKey(crop) && lastBrokenCrop != crop) {
            SkyHanniMod.coroutineScope.launch {
                getRanksForCollection(crop)
            }
        }
        lastBrokenCrop = crop
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (event.message.startsWith("§8Profile ID: ")) {
            val id = event.message.removePrefix("§8Profile ID: ")
            val newID = try {
                UUID.fromString(id)
            } catch (_: Exception) {
                null
            }
            if (profileID != newID) {
                resetData()
                profileID = newID
            }
        }
    }

    fun addCrop(crop: CropType, amount: Int) {
        if (!isEnabled()) return
        if (!config.estimateCollected) return

        currentCollections.addOrPut(crop, amount.toLong())
    }

    private fun resetData() {
        hasCollectionBeenFetched = false
        lastLeaderboardFetch = SimpleTimeMark.farPast()
        collectionRanks.clear()
        collectionPlacements.clear()
    }

    private fun updateDisplay() {
        if (lastFetchedCrop == null) return
        if (collectionPlacements.isEmpty()) return
        if (currentCollections.isEmpty()) {
            display = listOf(Renderable.wrappedString("§cCheck if your Collections \nAPI is enabled!", width = 200))
            return
        }

        val rank = collectionRanks[lastFetchedCrop] ?: return
        val nextRank = if (rank == -1) 5000 else rank - 1

        val placements = collectionPlacements[lastFetchedCrop] ?: return
        val collection = currentCollections[lastFetchedCrop] ?: 0
        val amountToBeat = placements[nextRank] ?: 0

        val difference = amountToBeat - collection

        val newDisplay = mutableListOf<Renderable>()
        newDisplay.add(
            Renderable.clickAndHover(
                "§6§l$lastFetchedCrop: §e${collection.addSeparators()}",
                listOf("§eClick to open your Farming Profile."),
                onClick = {
                    OSUtils.openBrowser("https://elitebot.dev/@${LorenzUtils.getPlayerName()}/")
                    ChatUtils.chat("Opening Farming Profile of player §b${LorenzUtils.getPlayerName()}")
                }
            )
        )
        if (nextRank <= 0) {
            newDisplay.add(
                Renderable.string("§aNo players ahead of you!")
            )
        } else if (difference <= 0) {
            newDisplay.add(
                Renderable.clickAndHover(
                    "§7You have passed §b#${nextRank.addSeparators()}",
                    listOf("§bClick to refresh."),
                    onClick = {
                        lastLeaderboardFetch = SimpleTimeMark.farPast()
                        ChatUtils.chat("Collection leaderboard updating...")
                    }
                )
            )
        } else {
            newDisplay.add(
                Renderable.string("§e${difference.addSeparators()} §7behind §b#${nextRank.addSeparators()}")
            )
        }
        if (config.showTimeUntilRefresh) {
            val time = checkDuration - lastLeaderboardFetch.passedSince()
            val timedisplay = if (time.isNegative()) "Now" else time.format()

            newDisplay.add(
                Renderable.string("§7Refreshes in: §b$timedisplay")
            )
        }
        display = newDisplay
    }

    private fun getRanksForCollection(crop: CropType) {
        if (profileID == null) return
        val url =
            "https://api.elitebot.dev/Leaderboard/rank/${getEliteBotLeaderboardForCrop(crop)}/${LorenzUtils.getPlayerUuid()}/${profileID!!.toDashlessUUID()}?includeUpcoming=true"

        val response = APIUtil.getJSONResponseAsElement(url)

        try {
            val data = eliteCollectionApiGson.fromJson<EliteLeaderboard>(response)

            collectionPlacements.clear()

            collectionRanks[crop] = data.rank
            val placements = mutableMapOf<Int, Long>()
            var rank = data.upcomingRank
            data.upcomingPlayers.forEach {
                //weight is amount
                placements[rank] = it.weight.toLong()
                rank--
            }
            collectionPlacements[crop] = placements
            lastFetchedCrop = crop
            currentCollections[crop] = data.amount

            if (!hasCollectionBeenFetched && data.amount == 0L) {
                hasCollectionBeenFetched = true
                getCurrentCollection()
            }
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e,
                "Error loading user farming collection leaderboard\n" +
                    "§eLoading the farming collection leaderboard data from elitebot.dev failed!\n" +
                    "§eYou can re-enter the garden to try to fix the problem.\n" +
                    "§cIf this message repeats, please report it on Discord!\n",
                "url" to url,
                "apiResponse" to response,
            )
        }
    }

    private fun getCurrentCollection() {
        if (profileID == null) return
        val url =
            "https://api.elitebot.dev/Graph/${LorenzUtils.getPlayerUuid()}/${profileID!!.toDashlessUUID()}/crops?days=1"
        val response = APIUtil.getJSONResponseAsElement(url)

        try {
            val data = eliteCollectionApiGson.fromJson<Array<EliteCollectionGraphEntry>>(response)

            data.sortBy { it.timestamp }
            currentCollections = data.lastOrNull()?.crops?.toMutableMap() ?: mutableMapOf()

        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e,
                "Error loading user farming collection\n" +
                    "§eLoading the farming collection data from elitebot.dev failed!\n" +
                    "§eYou can re-enter the garden to try to fix the problem.\n" +
                    "§cIf this message repeats, please report it on Discord!\n",
                "url" to url,
                "apiResponse" to response,
            )
        }
    }

    private fun getEliteBotLeaderboardForCrop(crop: CropType) = when (crop) {
        CropType.NETHER_WART -> "netherwart"
        CropType.SUGAR_CANE -> "sugarcane"
        else -> crop.simpleName
    }

    private fun isEnabled() =
        config.display && LorenzUtils.inSkyBlock && (GardenAPI.inGarden() || config.showOutsideGarden)
}
