package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.TimeUtils.format
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.apache.commons.net.ntp.NTPUDPClient
import java.net.InetAddress
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@SkyHanniModule
object TimeOffset {
    var offsetMillis = 0L
        private set

    init {
        SkyHanniMod.coroutineScope.launch {
            offsetMillis = getNtpOffset("time.google.com") ?: 0
            print("Time offset: $offsetMillis")
        }
    }

    private fun getNtpOffset(ntpServer: String): Long? {
        return try {
            val client = NTPUDPClient()
            val address = InetAddress.getByName(ntpServer)
            val timeInfo = client.getTime(address)

            timeInfo.computeDetails()
            timeInfo.offset
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        if (offsetMillis > 10 * 1000) {
            val formatted = offsetMillis.toDuration(DurationUnit.MILLISECONDS).format()
            ChatUtils.userError("Your computer's clock is off by $formatted. Please update your time settings.")
        }
    }

    @SubscribeEvent
    fun onDebugCollect(event: DebugDataCollectEvent) {
        event.title("Time Offset")
        event.addData("$offsetMillis ms")
    }
}
