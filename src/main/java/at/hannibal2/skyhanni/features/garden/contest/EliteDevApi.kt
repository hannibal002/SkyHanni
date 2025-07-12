package at.hannibal2.skyhanni.features.garden.contest

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ApiUtils
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.minutes

object EliteDevApi {
    private val contestStatic = ApiUtils.StaticApiPath(
        "https://api.elitebot.dev/contests/at/now",
        "Elitebot Farming Contests"
    )
    private val contestDuration = 20.minutes

    @KSerializable
    data class ContestsResponse(
        @Expose val year: Int,
        @Expose val count: Int,
        @Expose val complete: Boolean,

        @Expose
        @Deprecated("Use `responseContests` instead", ReplaceWith("responseContests"))
        val contests: Map<String, List<String>>,
    ) {
        @Suppress("Deprecation")
        val responseContests: List<EliteFarmingContest> = contests.mapNotNull { (timestampStr, cropStrList) ->
            val longTimeStamp = timestampStr.toLongOrNull() ?: return@mapNotNull null
            val crops = cropStrList.mapNotNull { cropStr ->
                CropType.getByNameOrNull(cropStr)
            }.takeIf { it.size == 3 } ?: return@mapNotNull null
            EliteFarmingContest((longTimeStamp * 1000).asTimeMark(), crops)
        }
    }

    data class EliteFarmingContest(
        @Expose val startTime: SimpleTimeMark,
        @Expose val crops: List<CropType>,
        @Expose var boostedCrop: CropType? = null,
    ) {
        val endTime = startTime + contestDuration
    }

    suspend fun fetchUpcomingContests(): List<EliteFarmingContest>? = try {
        val jsonContestsResponse = ApiUtils.getJSONResponse(contestStatic)
            ?: return null
        val contestResponse = ConfigManager.gson.fromJson<ContestsResponse>(jsonContestsResponse)
        if (contestResponse.complete) contestResponse.responseContests
        else {
            ChatUtils.chat(
                "This year's contests aren't available to fetch automatically yet, " +
                    "please load them from your calendar or wait 10 minutes.",
            )
            ChatUtils.clickableChat(
                "Click here to open your calendar!",
                onClick = { HypixelCommands.calendar() },
                "§eClick to run /calendar!",
            )
            null
        }
    } catch (e: Exception) {
        ErrorManager.logErrorWithData(
            e, "Failed to fetch upcoming contests. Please report this error if it continues to occur",
        )
        null
    }

    suspend fun submitContests(contests: List<EliteFarmingContest>) = try {
        val body = ConfigManager.gson.toJson(
            contests.associate { contest ->
                contest.startTime.toMillis() / 1000 to contest.crops.map { crop -> crop.cropName }
            },
        )
        val apiResponse = ApiUtils.postJSON(contestStatic, body)
        apiResponse.message
        if (apiResponse.success) {
            ChatUtils.chat("Successfully submitted this years upcoming contests, thank you for helping everyone out!")
        } else ErrorManager.logErrorStateWithData(
            "Something went wrong submitting upcoming contests!",
            "submitContestsToElite not successful",
            extraData = listOf(
                "apiResponse.message" to apiResponse.message,
                "apiResponse.data" to apiResponse.data,
            ).toTypedArray()
        )
    } catch (e: Exception) {
        ErrorManager.logErrorWithData(
            e, "Failed to submit upcoming contests. Please report this error if it continues to occur.",
            "contests" to contests,
        )
        null
    }
}
