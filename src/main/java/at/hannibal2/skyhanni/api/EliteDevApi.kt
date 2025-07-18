package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.EnumArgumentType
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteAuctionsResponse
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteBazaarResponse
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteContestsRequest
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteContestsResponse
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteFarmingContest
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteItemResponse
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboard
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.ElitePlayerWeightJson
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteWeightsJson
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.WeightProfile
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.api.ApiStaticPath
import at.hannibal2.skyhanni.utils.api.ApiStaticPostPath
import at.hannibal2.skyhanni.utils.api.ApiUtils
import at.hannibal2.skyhanni.utils.api.JsonApiResponse
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
            argCallback("resource", EnumArgumentType.lowercase<EliteResourceType>()) { resource ->
                SkyHanniMod.launchIOCoroutine {
                    fetchResourceCommand(resource)
                }
            }
        }
    }

    private suspend fun fetchResourceCommand(resourceType: EliteResourceType) = runCatching {
        val startTime = SimpleTimeMark.now()
        val resourcesFetched = when (resourceType) {
            EliteResourceType.ITEM -> fetchItemResources().items.size
            EliteResourceType.AUCTION -> fetchAuctionResources().items.size
            EliteResourceType.BAZAAR -> fetchBazaarResources().products.size
        }
        val elapsedFormat = startTime.passedSince().format()
        if (resourcesFetched == 0) {
            ChatUtils.chat("§cFailed to fetch §e$resourceType §cresources!")
            return@runCatching
        }
        ChatUtils.chat("Fetched $resourcesFetched $resourceType resources in $elapsedFormat.")
    }.onFailure {
        ChatUtils.chat("Failed to fetch $resourceType resources!")
    }

    private const val ELITEBOT_API_URL = "https://api.elitebot.dev"
    private const val FARMING_WEIGHT_API_NAME = "Elitebot Farming Weight"
    private const val FARMING_WEIGHT_URL = "$ELITEBOT_API_URL/weight"

    private val contestStatic = ApiStaticPostPath(
        "$ELITEBOT_API_URL/contests/at/now",
        "Elitebot Farming Contests",
    )

    private val apiWeightsStatic = ApiStaticPath(
        "$ELITEBOT_API_URL/weights/all",
        FARMING_WEIGHT_API_NAME,
    )

    private const val WEIGHT_LEADERBOARD_API_NAME = "Elitebot Farming Weight Leaderboard"
    private const val WEIGHT_LEADERBOARD_URL = "$ELITEBOT_API_URL/leaderboard/farmingweight"

    private const val RESOURCE_API_NAME = "Elitebot Resources"
    private const val RESOURCE_API_URL = "$ELITEBOT_API_URL/resources"

    // <editor-fold desc="Upcoming Contests">
    suspend fun fetchUpcomingContests(): List<EliteFarmingContest>? {
        val apiResponse = ApiUtils.getTypedJsonResponse<JsonObject>(contestStatic.toGet())
        val (_, apiData) = apiResponse.assertSuccessWithData() ?: ErrorManager.skyHanniError(
            "Failed to fetch upcoming contests. Please report this error if it continues to occur",
            "apiResponse" to apiResponse,
        )
        val contestResponse = ConfigManager.Companion.gson.fromJson<EliteContestsResponse>(apiData)
        return contestResponse.responseContests
    }

    suspend fun submitContests(contests: List<EliteFarmingContest>): Boolean {
        val body = EliteContestsRequest(contests).getBody()
        return ApiUtils.postJson(contestStatic, body).success
    }
    // </editor-fold>

    // <editor-fold desc="Farming Weight">
    private var weightUrl = ""
    private var weightProfileApiResponse: JsonApiResponse<JsonObject>? = null
    suspend fun fetchWeightProfile(localProfile: String): WeightProfile? = try {
        require(localProfile.isNotBlank()) { "Local profile cannot be blank" }

        weightUrl = "$FARMING_WEIGHT_URL/${PlayerUtils.getUuid()}"
        weightProfileApiResponse = ApiUtils.getTypedJsonResponse<JsonObject>(weightUrl, apiName = FARMING_WEIGHT_API_NAME)
        val (_, apiData) = weightProfileApiResponse?.assertSuccessWithData()
            ?: throw IllegalStateException("Response was not successful, or data was null")

        val weightData = ConfigManager.gson.fromJson<ElitePlayerWeightJson>(apiData)
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

    suspend fun fetchApiWeights(): EliteWeightsJson? {
        val apiWeightsResponse = ApiUtils.getTypedJsonResponse<JsonObject>(apiWeightsStatic.toGet())
        val (_, apiData) = apiWeightsResponse.assertSuccessWithData() ?: ErrorManager.skyHanniError(
            "Error getting crop weights from elitebot.dev",
            "apiWeightsResponse" to apiWeightsResponse,
        )
        return ConfigManager.gson.fromJson<EliteWeightsJson>(apiData)
    }
    // </editor-fold>

    // <editor-fold desc="Weight Leaderboard">
    suspend fun fetchLeaderboardPositions(
        profileId: String,
        lbType: EliteLeaderboardType,
        upcomingCount: Int? = null,
        atRank: Int? = null,
    ): EliteLeaderboard? {
        require(profileId.isNotBlank()) { "Profile ID cannot be blank" }
        val uuid = PlayerUtils.getUuid()

        val upcomingPlayersParam = upcomingCount?.let { "upcoming=$it" }
        val atRankParam = atRank?.let { "atRank=$it" }
        val params = listOfNotNull(upcomingPlayersParam, atRankParam)
        val paramString = if (params.isEmpty()) "" else {
            "?" + params.joinToString("&")
        }
        val lbSuffix = lbType.suffix
        val lbUrl = "$WEIGHT_LEADERBOARD_URL$lbSuffix/$uuid/$profileId$paramString"

        val lbApiResponse = ApiUtils.getTypedJsonResponse<JsonObject>(lbUrl, apiName = WEIGHT_LEADERBOARD_API_NAME)
        val (_, apiData) = lbApiResponse.assertSuccessWithData() ?: ErrorManager.skyHanniError(
            "Error getting weight leaderboard position",
            "url" to lbUrl,
            "apiResponse" to lbApiResponse,
        )
        return ConfigManager.gson.fromJson<EliteLeaderboard>(apiData)
    }
    // </editor-fold>

    // <editor-fold desc="Resources">
    private suspend inline fun <reified T : Any> fetchResources(subUrl: String): T {
        val resourceUrl = "$RESOURCE_API_URL/$subUrl"
        val resourceApiResponse = ApiUtils.getTypedJsonResponse<JsonObject>(resourceUrl, apiName = RESOURCE_API_NAME)
        val (_, apiData) = resourceApiResponse.assertSuccessWithData() ?: ErrorManager.skyHanniError(
            "Error getting resources from elitebot.dev",
            "resourceUrl" to resourceUrl,
            "resourceApiResponse" to resourceApiResponse,
        )
        return ConfigManager.gson.fromJson<T>(apiData)
    }

    private suspend fun fetchItemResources() = fetchResources<EliteItemResponse>("items")
    private suspend fun fetchAuctionResources() = fetchResources<EliteAuctionsResponse>("auctions")
    private suspend fun fetchBazaarResources() = fetchResources<EliteBazaarResponse>("bazaar")
    // </editor-fold>
}
