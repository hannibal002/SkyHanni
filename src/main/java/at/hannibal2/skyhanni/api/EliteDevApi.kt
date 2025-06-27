package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.other.elitedev.EliteContestsResponse
import at.hannibal2.skyhanni.data.jsonobjects.other.elitedev.EliteFarmingContest
import at.hannibal2.skyhanni.data.jsonobjects.other.elitedev.EliteLeaderboard
import at.hannibal2.skyhanni.data.jsonobjects.other.elitedev.EliteLeaderboardJson
import at.hannibal2.skyhanni.data.jsonobjects.other.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.data.jsonobjects.other.elitedev.ElitePlayerWeightJson
import at.hannibal2.skyhanni.data.jsonobjects.other.elitedev.EliteWeightsJson
import at.hannibal2.skyhanni.data.jsonobjects.other.elitedev.WeightProfile
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ApiUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.json.BaseGsonBuilder
import at.hannibal2.skyhanni.utils.json.SkyHanniTypeAdapters
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.JsonObject

object EliteDevApi {

    private const val ELITEBOT_API_URL = "https://api.elitebot.dev"

    private const val CONTEST_API_NAME = "Elitebot Farming Contests"
    private const val CONTEST_API_URL = "$ELITEBOT_API_URL/contests/at/now"

    private const val FARMING_WEIGHT_API_NAME = "Elitebot Farming Weight"
    private const val FARMING_WEIGHT_URL = "$ELITEBOT_API_URL/weight"
    private const val API_WEIGHTS_URL = "$ELITEBOT_API_URL/weights/all"

    private const val WEIGHT_LEADERBOARD_API_NAME = "Elitebot Farming Weight Leaderboard"
    private const val WEIGHT_LEADERBOARD_URL = "$ELITEBOT_API_URL/leaderboard/farmingweight"

    // Todo when ConfigManager gson has type adapters specifically registered, see
    //  if we can replace this custom gson with ConfigManager.gson
    private val eliteGson by lazy {
        BaseGsonBuilder.gson()
            .registerTypeAdapter(CropType::class.java, SkyHanniTypeAdapters.CROP_TYPE.nullSafe())
            .registerTypeAdapter(PestType::class.java, SkyHanniTypeAdapters.PEST_TYPE.nullSafe())
            .create()
    }

    // <editor-fold desc="Upcoming Contests">
    fun fetchUpcomingContests(): List<EliteFarmingContest>? = try {
        val jsonContestsResponse = ApiUtils.getJSONResponse(CONTEST_API_URL, apiName = CONTEST_API_NAME).asJsonObject
        val contestResponse = ConfigManager.Companion.gson.fromJson<EliteContestsResponse>(jsonContestsResponse)
        contestResponse.responseContests
    } catch (e: Exception) {
        ErrorManager.logErrorWithData(
            e, "Failed to fetch upcoming contests. Please report this error if it continues to occur",
        )
        null
    }

    fun submitContests(contests: List<EliteFarmingContest>): Boolean = try {
        val body = ConfigManager.Companion.gson.toJson(
            contests.associate { contest ->
                contest.startTime.toMillis() / 1000 to contest.crops.map { crop -> crop.cropName }
            },
        )
        ApiUtils.postJSONIsSuccessful(CONTEST_API_URL, body, apiName = CONTEST_API_NAME)
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
    private var weightApiResponse: JsonObject? = null
    fun fetchWeightProfile(localProfile: String): WeightProfile? = try {
        require(localProfile.isNotBlank()) { "Local profile cannot be blank" }

        val uuid = PlayerUtils.getUuid()
        weightUrl = "$FARMING_WEIGHT_URL/$uuid"

        weightApiResponse = ApiUtils.getJSONResponse(weightUrl, apiName = FARMING_WEIGHT_API_NAME).asJsonObject
        val weightApiResponse = weightApiResponse ?: throw IllegalStateException("Response was null")
        val weightData = eliteGson.fromJson<ElitePlayerWeightJson>(weightApiResponse)

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
            "weightApiResponse" to weightApiResponse,
            "localProfile" to localProfile,
        )
        null
    }

    private var apiWeightsResponse: JsonObject? = null
    fun fetchApiWeights(): EliteWeightsJson? = try {
        apiWeightsResponse = ApiUtils.getJSONResponse(API_WEIGHTS_URL, apiName = FARMING_WEIGHT_API_NAME).asJsonObject
        val apiWeightsResponse = apiWeightsResponse ?: throw IllegalStateException("Response was null")
        eliteGson.fromJson<EliteWeightsJson>(apiWeightsResponse)
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
    fun fetchLeaderboardPositions(
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

        lbApiResponse = ApiUtils.getJSONResponse(lbUrl, apiName = WEIGHT_LEADERBOARD_API_NAME).asJsonObject
        val lbApiResponse = lbApiResponse ?: throw IllegalStateException("Response was null")
        val lbData = eliteGson.fromJson<EliteLeaderboardJson>(lbApiResponse)
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
}
