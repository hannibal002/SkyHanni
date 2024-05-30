package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.Mayor.Companion.setAssumeMayor
import at.hannibal2.skyhanni.data.Mayor.Companion.setAssumeMayorJson
import at.hannibal2.skyhanni.data.jsonobjects.other.MayorCandidate
import at.hannibal2.skyhanni.data.jsonobjects.other.MayorElection
import at.hannibal2.skyhanni.data.jsonobjects.other.MayorJson
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.CollectionUtils.put
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.fromJson
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

object MayorAPI {

    val group = RepoPattern.group("mayorapi")
    val foxyExtraEventPattern by group.pattern(
        "foxy.extraevent",
        "Schedules an extra §.(?<event>.*) §.event during the year\\."
    )
    private val electionOver by group.pattern(
        "election.over",
        "§eThe election room is now closed\\. Clerk Seraphine is doing a final count of the votes\\.\\.\\."
    )

    private var lastMayor: Mayor? = null

    var lastUpdate = SimpleTimeMark.farPast()
    private var dispatcher = Dispatchers.IO

    private var rawMayorData: MayorJson? = null
    var candidates = mapOf<Int, MayorCandidate>()
        private set
    var currentMayor: Mayor? = null
        private set
    var timeTillNextMayor = Duration.ZERO
        private set

    private const val ELECTION_END_MONTH = 3 // Late Spring
    private const val ELECTION_END_DAY = 27

    /**
     * @param input: The name of the mayor
     * @return: The neu color of the mayor; If no mayor was found, it will return "§c"
     */
    fun mayorNameToColorCode(input: String): String = Mayor.getMayorFromName(input)?.color ?: "§c"

    /**
     * @param input: The name of the mayor
     * @return: The neu color of the mayor + the name of the mayor; If no mayor was found, it will return "§c[input]"
     */
    fun mayorNameWithColorCode(input: String) = mayorNameToColorCode(input) + input

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!LorenzUtils.onHypixel) return
        if (event.repeatSeconds(2)) {
            checkHypixelAPI()
            getTimeTillNextMayor()
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.onHypixel) return

        if (electionOver.matches(event.message)) {
            lastMayor = currentMayor
            currentMayor = Mayor.UNKNOWN
        }
    }

    private fun calculateNextMayorTime(): SimpleTimeMark {
        var mayorYear = SkyBlockTime.now().year

        // Check if either the month is already over or the day after 27th in the third month
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
            if (it.name == lastMayor?.name) return

            // TODO: Once Jerry is active, add the sub mayor perks in here
            setAssumeMayorJson(it.name, it.perks)
        }
    }

    private fun checkHypixelAPI() {
        if (lastUpdate.passedSince() < 20.minutes || (currentMayor == Mayor.UNKNOWN && lastUpdate.passedSince() < 1.minutes)) return
        lastUpdate = SimpleTimeMark.now()

        SkyHanniMod.coroutineScope.launch {
            val url = "https://api.hypixel.net/v2/resources/skyblock/election"
            val jsonObject = withContext(dispatcher) { APIUtil.getJSONResponse(url) }
            rawMayorData = ConfigManager.gson.fromJson<MayorJson>(jsonObject)
            val data = rawMayorData ?: return@launch
            val map = mutableMapOf<Int, MayorCandidate>()
            map put data.mayor.election.getPairs()
            data.current?.let {
                map put data.current.getPairs()
            }
            candidates = map
            checkCurrentMayor()
        }
    }

    private fun MayorElection.getPairs() = year + 1 to candidates.bestCandidate()

    private fun List<MayorCandidate>.bestCandidate() = maxBy { it.votes }

    @SubscribeEvent
    fun onConfigReload(event: ConfigLoadEvent) {
        SkyHanniMod.feature.dev.debug.assumeMayor.onToggle {
            val mayor = SkyHanniMod.feature.dev.debug.assumeMayor.get()

            if (mayor == Mayor.DISABLED) {
                checkCurrentMayor()
            } else {
                mayor.setAssumeMayor(mayor.perks.toList())
                currentMayor = mayor
            }
        }
    }

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
