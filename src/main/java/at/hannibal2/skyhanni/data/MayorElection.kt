package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.local.MayorJson
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.put
import at.hannibal2.skyhanni.utils.SkyBlockTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MayorElection {
    private var lastUpdate = 0L
    private var dispatcher = Dispatchers.IO

    companion object {
        var rawMayorData: MayorJson? = null
        var candidates = mapOf<Int, MayorJson.Candidate>()
        var currentCandidate: MayorJson.Candidate? = null

        fun isPerkActive(mayor: String, perk: String) = currentCandidate?.let { currentCandidate ->
            currentCandidate.name == mayor && currentCandidate.perks.any { it.name == perk }
        } ?: false
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.onHypixel) return

        if (event.repeatSeconds(3)) {
            check()
        }
    }

    private fun check() {
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
        currentCandidate = candidates[currentYear]
    }

    private fun MayorJson.Election.getPairs() = year + 1 to candidates.bestCandidate()

    private fun List<MayorJson.Candidate>.bestCandidate() = maxBy { it.votes }
}
