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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@SkyHanniModule
object ComputerTimeOffset {
    var offsetMillis: Duration? = null
        private set

    private val offsetFixLinks by lazy {
        when {
            OSUtils.isWindows -> "https://support.microsoft.com/en-us/windows/how-to-set-your-time-and-time-zone-dfaa7122-479f-5b98-2a7b-fa0b6e01b261"
            OSUtils.isLinux -> "https://unix.stackexchange.com/a/79116"
            OSUtils.isMac -> "https://support.apple.com/guide/mac-help/set-the-date-and-time-automatically-mchlp2996/mac"
            else -> null
        }
    }

    init {
        SkyHanniMod.coroutineScope.launch {
            offsetMillis = getNtpOffset("time.google.com")
            offsetMillis?.let {
                print("SkyHanni detected a time offset of ${it.format()}.")
            } ?: print("SkyHanni failed to detect a time offset.")
        }
    }

    private fun getNtpOffset(ntpServer: String): Duration? {
        return try {
            val client = NTPUDPClient()
            val address = InetAddress.getByName(ntpServer)
            val timeInfo = client.getTime(address)

            timeInfo.computeDetails()
            timeInfo.offset.toDuration(DurationUnit.MILLISECONDS)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        if (!SkyHanniMod.feature.misc.warnAboutPcTimeOffset) return
        offsetMillis?.let {
            if (it.absoluteValue > 5.seconds) {
                ChatUtils.clickableLinkChat(
                    "Your computer's clock is off by ${it.format()}. Please update your time settings. Click here for instructions.",
                    offsetFixLinks ?: return,
                    prefixColor = "§c",
                )
            }
        }
    }

    @SubscribeEvent
    fun onDebugCollect(event: DebugDataCollectEvent) {
        offsetMillis?.absoluteValue?.let { if (it < 100.milliseconds) return }
        event.title("Time Offset")
        event.addData("$offsetMillis ms")
    }
}
