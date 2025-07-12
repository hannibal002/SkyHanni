package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.EnumArgumentType
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteAuctionsResponse
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteBazaarResponse
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteContestsResponse
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteFarmingContest
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteItemResponse
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboard
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardJson
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.ElitePlayerWeightJson
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteWeightsJson
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.WeightProfile
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ApiUtils
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.JsonObject

@SkyHanniModule
object EliteDevApi {

    enum class EliteResourceType(private val displayName: String) {
        ITEM("Item"),
        AUCTION("Auction"),
        BAZAAR("Bazaar"),
        ;

        override fun toString() = displayName
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shfetcheliteresource") {
            description = "Fetches the specified Elite resource from elitebot.dev"
            category = CommandCategory.DEVELOPER_DEBUG
            arg("resource", EnumArgumentType.name<EliteResourceType>()) { resource ->
                callback {
                    SkyHanniMod.launchIOCoroutine {
                        fetchResourceCommand(getArg(resource))
                    }
                }
            }
        }
    }

    private suspend fun fetchResourceCommand(resourceType: EliteResourceType) {
        val startTime = SimpleTimeMark.now()
        val resourcesFetched = when (resourceType) {
            EliteResourceType.ITEM -> fetchItemResources()?.items?.size
            EliteResourceType.AUCTION -> fetchAuctionResources()?.items?.size
            EliteResourceType.BAZAAR -> fetchBazaarResources()?.products?.size
        }
        val elapsedFormat = startTime.passedSince().format()
        if (resourcesFetched == null || resourcesFetched == 0) {
            ChatUtils.chat("§cFailed to fetch §e$resourceType §cresources!")
            return
        }
        ChatUtils.chat("Fetched $resourcesFetched $resourceType resources in $elapsedFormat.")
    }

    private const val ELITEBOT_API_URL = "https://api.elitebot.dev"
    private const val FARMING_WEIGHT_API_NAME = "Elitebot Farming Weight"
    private const val FARMING_WEIGHT_URL = "$ELITEBOT_API_URL/weight"

    private val contestStatic = ApiUtils.StaticApiPath(
        "$ELITEBOT_API_URL/contests/at/now",
        "Elitebot Farming Contests"
    )

    private val apiWeightsStatic = ApiUtils.StaticApiPath(
        "$ELITEBOT_API_URL/weights/all",
        FARMING_WEIGHT_API_NAME
    )

    private const val WEIGHT_LEADERBOARD_API_NAME = "Elitebot Farming Weight Leaderboard"
    private const val WEIGHT_LEADERBOARD_URL = "$ELITEBOT_API_URL/leaderboard/farmingweight"

    private const val RESOURCE_API_NAME = "Elitebot Resources"
    private const val RESOURCE_API_URL = "$ELITEBOT_API_URL/resources"

    // <editor-fold desc="Upcoming Contests">
    var contestsApiResponse: JsonObject? = null
    suspend fun fetchUpcomingContests(): List<EliteFarmingContest>? = try {
        contestsApiResponse = ApiUtils.getTypedJSONResponse(contestStatic)
        val contestsApiResponse = contestsApiResponse ?: throw IllegalStateException("Response was null")
        val contestResponse = ConfigManager.Companion.gson.fromJson<EliteContestsResponse>(contestsApiResponse)
        contestResponse.responseContests
    } catch (e: Exception) {
        ErrorManager.logErrorWithData(
            e,
            "Failed to fetch upcoming contests. Please report this error if it continues to occur",
            "contestApiResponse" to contestsApiResponse,
        )
        null
    }

    suspend fun submitContests(contests: List<EliteFarmingContest>): Boolean = try {
        val body = ConfigManager.Companion.gson.toJson(
            contests.associate { contest ->
                contest.startTime.toMillis() / 1000 to contest.crops.map { crop -> crop.cropName }
            },
        )
        val response = ApiUtils.postJSON(contestStatic, body)
        response.success
    } catch (e: Exception) {
        ErrorManager.logErrorWithData(
            e,
            "Failed to submit upcoming contests. Please report this error if it continues to occur.",
            "contests" to contests,
        )
        false
    }
    // </editor-fold>

    // <editor-fold desc="Farming Weight">
    private var weightUrl = ""
    private var weightProfileApiResponse: JsonObject? = null
    suspend fun fetchWeightProfile(localProfile: String): WeightProfile? = try {
        require(localProfile.isNotBlank()) { "Local profile cannot be blank" }

        val uuid = PlayerUtils.getUuid()
        weightUrl = "$FARMING_WEIGHT_URL/$uuid"

        weightProfileApiResponse = ApiUtils.getTypedJSONResponse(weightUrl, apiName = FARMING_WEIGHT_API_NAME)
        val weightApiResponse = weightProfileApiResponse ?: throw IllegalStateException("Response was null")
        val weightData = ConfigManager.gson.fromJson<ElitePlayerWeightJson>(weightApiResponse)

        val selectedProfileId = weightData.selectedProfileId
        val selectedProfileEntry = weightData.profiles.firstOrNull {
            val idMatch = it.profileId == selectedProfileId
            val nameMatch = it.profileName.lowercase() == localProfile.lowercase()
            // Prioritize matching by ID, but also allow matching by name
            (idMatch && nameMatch) || nameMatch
        } ?: throw IllegalStateException(
            "No profile found matching the local profile: $localProfile",
        )

        selectedProfileEntry
    } catch (e: Exception) {
        ErrorManager.logErrorWithData(
            e,
            "Error loading user farming weight\n" +
                "§eLoading the farming weight data from elitebot.dev failed!\n" +
                "§eYou can re-enter the garden to try to fix the problem.\n" +
                "§cIf this message repeats, please report it on Discord",
            "weightUrl" to weightUrl,
            "weightApiResponse" to weightProfileApiResponse,
            "localProfile" to localProfile,
        )
        null
    }

    private var apiWeightsResponse: JsonObject? = null
    suspend fun fetchApiWeights(): EliteWeightsJson? = try {
        apiWeightsResponse = ApiUtils.getTypedJSONResponse(apiWeightsStatic)
        val apiWeightsResponse = apiWeightsResponse ?: throw IllegalStateException("Response was null")
        ConfigManager.gson.fromJson<EliteWeightsJson>(apiWeightsResponse)
    } catch (e: Exception) {
        ErrorManager.logErrorWithData(
            e, "Error getting crop weights from elitebot.dev",
            "apiWeightsResponse" to apiWeightsResponse,
        )
        null
    }
    // </editor-fold>

    // <editor-fold desc="Weight Leaderboard">
    private var lbUrl = ""
    private var lbApiResponse: JsonObject? = null
    suspend fun fetchLeaderboardPositions(
        profileId: String,
        lbType: EliteLeaderboardType,
        upcomingCount: Int? = null,
        atRank: Int? = null,
    ): EliteLeaderboard? = try {
        require(profileId.isNotBlank()) { "Profile ID cannot be blank" }
        val uuid = PlayerUtils.getUuid()

        val upcomingPlayersParam = upcomingCount?.let { "upcoming=$it" }
        val atRankParam = atRank?.let { "atRank=$it" }
        val params = listOfNotNull(upcomingPlayersParam, atRankParam)
        val paramString = if (params.isEmpty()) "" else {
            "?" + params.joinToString("&")
        }
        val lbSuffix = lbType.suffix
        lbUrl = "$WEIGHT_LEADERBOARD_URL$lbSuffix/$uuid/$profileId$paramString"

        lbApiResponse = ApiUtils.getTypedJSONResponse(lbUrl, apiName = WEIGHT_LEADERBOARD_API_NAME)
        val lbApiResponse = lbApiResponse ?: throw IllegalStateException("Response was null")
        val lbData = ConfigManager.gson.fromJson<EliteLeaderboardJson>(lbApiResponse)
        lbData.data
    } catch (e: Exception) {
        ErrorManager.logErrorWithData(
            e, "Error getting weight leaderboard position",
            "url" to lbUrl,
            "apiResponse" to lbApiResponse,
        )
        null
    }
    // </editor-fold>

    // <editor-fold desc="Resources">
    private var resourceUrl = ""
    private var resourceApiResponse: JsonObject? = null
    private suspend inline fun <reified T> fetchResources(
        subUrl: String,
    ): T? = try {
        resourceUrl = "$RESOURCE_API_URL/$subUrl"
        resourceApiResponse = ApiUtils.getTypedJSONResponse(resourceUrl, apiName = RESOURCE_API_NAME)
        val resourceApiResponse = resourceApiResponse ?: throw IllegalStateException("Response was null")
        ConfigManager.gson.fromJson(resourceApiResponse, T::class.java)
    } catch (e: Exception) {
        ErrorManager.logErrorWithData(
            e, "Error getting resources from elitebot.dev",
            "resourceUrl" to resourceUrl,
            "resourceApiResponse" to resourceApiResponse,
        )
        null
    }

    private suspend fun fetchItemResources() = fetchResources<EliteItemResponse>("items")
    private suspend fun fetchAuctionResources() = fetchResources<EliteAuctionsResponse>("auctions")
    private suspend fun fetchBazaarResources() = fetchResources<EliteBazaarResponse>("bazaar")
    // </editor-fold>
}
