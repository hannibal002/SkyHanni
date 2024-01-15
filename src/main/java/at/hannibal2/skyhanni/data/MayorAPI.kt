package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.local.MayorJson
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.put
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object MayorAPI {
    private var lastUpdate = 0L
    private var dispatcher = Dispatchers.IO

    var rawMayorData: MayorJson? = null
    var candidates = mapOf<Int, MayorJson.Candidate>()
    var currentMayor: MayorJson.Candidate? = null
    var timeTillNextMayor = 0L

    fun isPerkActive(mayor: String, perk: String) = currentMayor?.let { currentCandidate ->
        currentCandidate.name == mayor && currentCandidate.perks.any { it.name == perk }
    } ?: false

    /**
     * @param input: The name of the mayor
     * @return: The neu color of the mayor + the name of the mayor; If no mayor was found, it will return "§cUnknown Mayor: §7"
     */
    fun mayorNameToColorCode(input: String): String {
        return when (input) {
            // Normal Mayors
            "Aatrox" -> "§3"
            "Cole" -> "§e"
            "Diana" -> "§2"
            "Diaz" -> "§6"
            "Finnegan" -> "§c"
            "Foxy" -> "§d"
            "Marina" -> "§b"
            "Paul" -> "§c"

            // Special Mayors
            "Scorpius" -> "§d"
            "Jerry" -> "§d"
            "Derpy" -> "§d"
            "Dante" -> "§d"
            else -> "§cUnknown Mayor: §7"
        }
    }
    
    /**
     * @param input: The name of the mayor
     * @return: The neu color of the mayor + the name of the mayor; If no mayor was found, it will return "§cUnknown Mayor: §7"
     */
    fun mayorNameWithColorCode(input: String) = mayorNameToColorCode(input) + input
    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.onHypixel) return

        if (event.repeatSeconds(3)) {
            checkHypixelAPI()
            getTimeTillNextMayor()
        }
    }

    private fun getTimeTillNextMayor() {
        var currentYear = SkyBlockTime.now().year
        val month = 3 // Late Spring
        // check if either the month is already over or the day is after 27th and third month
        if (SkyBlockTime.now().month > month || (SkyBlockTime.now().day >= 27 && SkyBlockTime.now().month == month)) {
            // if so, next mayor will be in the next year
            currentYear++
        }
        val nextMayorTime = SkyBlockTime(currentYear, month, day = 27).toMillis()

        timeTillNextMayor = nextMayorTime - System.currentTimeMillis()
    }

    private fun checkHypixelAPI() {
        if (System.currentTimeMillis() > lastUpdate + 60_000 * 5) {
            lastUpdate = System.currentTimeMillis()
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
            }
        }

        checkCurrentMayor()
    }

    private fun checkCurrentMayor() {
        var currentYear = SkyBlockTime.now().year

        // The time in the current SkyBlock year when the election circle will restart
        val month = 3 // Late Spring
        val nextMayorTime = SkyBlockTime(currentYear, month, day = 27).toMillis()

        // Is it still the mayor from old sb year?
        if (nextMayorTime > System.currentTimeMillis()) {
            currentYear--
        }
        currentMayor = candidates[currentYear]
    }

    private fun MayorJson.Election.getPairs() = year + 1 to candidates.bestCandidate()

    private fun List<MayorJson.Candidate>.bestCandidate() = maxBy { it.votes }
}
