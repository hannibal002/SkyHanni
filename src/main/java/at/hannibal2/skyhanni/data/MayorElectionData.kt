package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.MayorData
import at.hannibal2.skyhanni.utils.MayorData.Candidate
import com.google.gson.GsonBuilder
import io.github.moulberry.moulconfig.observer.PropertyTypeAdapterFactory
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class MayorElectionData {
    private var tick = 0
    private var lastUpdate = 0L

    private val gson = GsonBuilder().setPrettyPrinting()
        .registerTypeAdapterFactory(PropertyTypeAdapterFactory())
        .create()

    companion object {
        var rawMayorData: MayorData? = null
        var candidates = mapOf<Int, Candidate>()
        var currentCandidate: Candidate? = null

        fun isPerkActive(mayor: String, perk: String): Boolean {
            return currentCandidate?.let { currentCandidate ->
                currentCandidate.name == mayor && currentCandidate.perks.any { it.name == perk }
            } ?: false
        }

    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (!LorenzUtils.onHypixel) return

        tick++

        if (tick % 60 == 0) {
            check()
        }
    }

    private fun check() {
        if (System.currentTimeMillis() > lastUpdate + 60_000 * 5) {
            lastUpdate = System.currentTimeMillis()
            SkyHanniMod.coroutineScope.launch {
                val url = "https://api.hypixel.net/resources/skyblock/election"
                val jsonObject = withContext(Dispatchers.IO) { APIUtil.getJSONResponse(url) }
                rawMayorData = gson.fromJson(jsonObject, MayorData::class.java)
                val data = rawMayorData ?: return@launch
                val map = mutableMapOf<Int, Candidate>()
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

        // The time in the current skyblock year when the election circle will restart
        val month = 3 // Late Spring
        val nextMayorTime = SkyBlockTime(currentYear, month, day = 27).toMillis()

        // Is it still the major from old sb year?
        if (nextMayorTime > System.currentTimeMillis()) {
            currentYear--
        }
        currentCandidate = candidates[currentYear]
    }

    private fun MayorData.Election.getPairs() = year + 1 to candidates.bestCandidate()

    private fun List<MayorData.Candidate>.bestCandidate() = maxBy { it.votes }

    private infix fun <K, V> MutableMap<K, V>.put(pairs: Pair<K, V>) {
        this[pairs.first] = pairs.second
    }
}