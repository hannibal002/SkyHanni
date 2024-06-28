package at.hannibal2.skyhanni.features.misc.datahistory.purse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.misc.datahistory.DataPoint
import at.hannibal2.skyhanni.features.misc.datahistory.TimeChartRenderer
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NumberUtil.million
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import com.google.gson.Gson
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PurseHistory {

    private val config get() = SkyHanniMod.feature.misc.dataHistory

    var lastSave = SimpleTimeMark.farPast()
    var lastPrice = 0.0

    val dataPoints: MutableList<DataPoint>?
        get() = ProfileStorageData.profileSpecific?.historyData?.purse

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (!event.repeatSeconds(5)) return

        val purse = (PurseAPI.currentPurse / 1.million).round(2)
        val change = lastPrice != purse
        val interval = if (change) 20.seconds else 20.minutes
        if (lastSave.passedSince() > interval) {
            save(purse)
        }
    }

    private fun save(purse: Double) {
        val dataPoints = dataPoints ?: return

        lastPrice = purse
        lastSave = SimpleTimeMark.now()

        ChatUtils.debug("saved purse history: ${purse}m")
        dataPoints.add(DataPoint(System.currentTimeMillis(), purse))

        val json = Gson().toJson(dataPoints)
        println(json)
    }

    private fun formattedTime(): String {
        val currentTime = LocalDateTime.now()
        val currentZone = ZoneId.systemDefault()

        val formatter = DateTimeFormatter.ISO_DATE_TIME
        return currentTime.atZone(currentZone).format(formatter)
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.purse

    fun onCommand() {
        if (!LorenzUtils.inSkyBlock) {
            ChatUtils.userError("Only available on SkyBlock!")
            return
        }
        if (!config.purse) {
            ChatUtils.chatAndOpenConfig(
                "§cPurse history is disabled!",
                config::purse,
            )
            return
        }


        val dataPoints = dataPoints
        if (dataPoints == null) {
            ErrorManager.logErrorStateWithData(
                "failed to load purse history",
                "profile is null while trying to read dataPoints for purse history",
            )
            return
        }

        TimeChartRenderer.openTimeChart(
            dataPoints,
            title = "Purse History",
            label = "Coins (Millions)",
        )
    }
}
