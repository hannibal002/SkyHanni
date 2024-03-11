package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.Mayor.Companion.setMayorWithActivePerks
import at.hannibal2.skyhanni.data.jsonobjects.local.MayorJson
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.CollectionUtils.put
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

object MayorAPI {
    var lastUpdate = SimpleTimeMark.farPast()
    private var dispatcher = Dispatchers.IO

    private var rawMayorData: MayorJson? = null
    var candidates = mapOf<Int, MayorJson.Candidate>()
        private set
    var currentMayor: Mayor? = null
        private set
    var timeTillNextMayor = Duration.ZERO
        private set

    private const val ELECTION_END_MONTH = 3 //Late Spring
    private const val ELECTION_END_DAY = 27

    /**
     * @param input: The name of the mayor
     * @return: The neu color of the mayor; If no mayor was found, it will return "§cUnknown: §7"
     */
    fun mayorNameToColorCode(input: String): String {
        return Mayor.getMayorFromName(input).color
    }

    /**
     * @param input: The name of the mayor
     * @return: The neu color of the mayor + the name of the mayor; If no mayor was found, it will return "§cUnknown: §7[input]"
     */
    fun mayorNameWithColorCode(input: String) = mayorNameToColorCode(input) + input

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.onHypixel) return

        if (event.repeatSeconds(2)) {
            checkHypixelAPI()
            getTimeTillNextMayor()
        }
    }

    private fun calculateNextMayorTime(): SimpleTimeMark {
        var mayorYear = SkyBlockTime.now().year

        // Check if either the month is already over or the day is after 27th in the third month
        if (SkyBlockTime.now().month > ELECTION_END_MONTH || (SkyBlockTime.now().day >= ELECTION_END_DAY && SkyBlockTime.now().month == ELECTION_END_MONTH)) {
            // If so, the next mayor will be in the next year
            mayorYear++
        }

        return SkyBlockTime(mayorYear, ELECTION_END_MONTH, day = ELECTION_END_DAY).asTimeMark()
    }

    private fun getTimeTillNextMayor() {
        val nextMayorTime = calculateNextMayorTime()
        timeTillNextMayor = nextMayorTime - SimpleTimeMark.now()
    }

    private fun checkCurrentMayor() {
        val nextMayorTime = calculateNextMayorTime()

        // Check if it is still the mayor from the old SkyBlock year
        currentMayor = candidates[nextMayorTime.toSkyBlockTime().year - 1]?.let {
            // TODO: Once Jerry is active, add the sub mayor perks in here
            setMayorWithActivePerks(it.name, it.perks)
        }
    }

    private fun checkHypixelAPI() {
        if (lastUpdate.passedSince() < 20.minutes) return
        lastUpdate = SimpleTimeMark.now()

        SkyHanniMod.coroutineScope.launch {
            val url = "https://api.hypixel.net/v2/resources/skyblock/election"
            val jsonObject = withContext(dispatcher) { APIUtil.getJSONResponse(url) }
            rawMayorData = ConfigManager.gson.fromJson(jsonObject, MayorJson::class.java)
            val data = rawMayorData ?: return@launch
            val map = mutableMapOf<Int, MayorJson.Candidate>()
            map put data.mayor.election.getPairs()
            data.current?.let {
                map put data.current.getPairs()
            }
            candidates = map
            checkCurrentMayor()
        }
    }

    private fun MayorJson.Election.getPairs() = year + 1 to candidates.bestCandidate()

    private fun List<MayorJson.Candidate>.bestCandidate() = maxBy { it.votes }

    @SubscribeEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Mayor")
        event.addIrrelevant {
            add("Current Mayor: ${currentMayor?.name ?: "Unknown"}")
            add("Active Perks: ${currentMayor?.activePerks}")
            add("Last Update: $lastUpdate (${lastUpdate.passedSince()} ago)")
            add("Time Till Next Mayor: $timeTillNextMayor")
        }
    }
}
