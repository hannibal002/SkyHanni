package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.EnumUtils.next
import at.hannibal2.skyhanni.utils.TimeUtils.format
import kotlinx.coroutines.launch
import org.apache.commons.net.ntp.NTPUDPClient
import java.net.InetAddress
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ComputerTimeOffset {
    private var offsetMillis: Duration? = null

    private val config get() = SkyHanniMod.feature.misc

    private var state = State.NORMAL

    enum class State {
        NORMAL,
        SLOW,
        TOTALLY_OFF,
    }

    private var currentlyChecking = false

    private val offsetFixLinks by lazy {
        when {
            OSUtils.isWindows -> {
                "https://support.microsoft.com/en-us/windows/how-to-set-your-time-and-time-zone-dfaa7122-479f-5b98-2a7b-fa0b6e01b261"
            }

            OSUtils.isLinux -> "https://unix.stackexchange.com/a/79116"
            OSUtils.isMac -> "https://support.apple.com/guide/mac-help/set-the-date-and-time-automatically-mchlp2996/mac"
            else -> null
        }
    }

    private val distanceBetweenTrials get() = if (state == State.NORMAL) 1000L else 10_0000L

    init {
        thread {
            while (state != State.TOTALLY_OFF) {
                Thread.sleep(distanceBetweenTrials)
                detectTimeChange()
            }
        }
    }

    private fun checkOffset() {
        // probably a problem when the response somehow took longer than 1s?
        if (currentlyChecking) {
            state = state.next() ?: error("state is already TOTALLY_OFF")
            if (state == State.TOTALLY_OFF) {
                ErrorManager.logErrorStateWithData(
                    "Error when checking Computer Time Offset",
                    "trying to check again even though the previous check is stil not done",
                )
            }
            if (state == State.SLOW) {
                ChatUtils.chat("Computer Time Offset calculation takes longer than normal. Checkign now less often.")
            }
            currentlyChecking = false
            return
        }
        currentlyChecking = true
        val wasOffsetBefore = (offsetMillis?.absoluteValue ?: 0.seconds) > 5.seconds
        SkyHanniMod.coroutineScope.launch {
            offsetMillis = getNtpOffset(SkyHanniMod.feature.dev.ntpServer)
            currentlyChecking = false
            offsetMillis?.let {
                tryDisplayOffset(wasOffsetBefore)
            }
        }
    }

    private fun getNtpOffset(ntpServer: String): Duration? = try {
        val timeInfo = NTPUDPClient().use { client ->
            val address = InetAddress.getByName(ntpServer)
            client.getTime(address)
        }

        timeInfo.computeDetails()
        timeInfo.offset.milliseconds
    } catch (e: Exception) {
        if (LorenzUtils.inSkyBlock && config.warnAboutPcTimeOffset) ErrorManager.logErrorWithData(
            e, "Failed to get NTP offset",
            "server" to ntpServer,
        )
        else {
            @Suppress("PrintStackTrace")
            e.printStackTrace()
        }
        null
    }

    private var lastSystemTime = System.currentTimeMillis()

    private fun detectTimeChange() {
        val currentSystemTime = System.currentTimeMillis()
        val timeDifference = (currentSystemTime - lastSystemTime).milliseconds
        lastSystemTime = currentSystemTime

        val expectedDuration = 1.seconds
        val deviation = timeDifference - expectedDuration

        if (deviation.absoluteValue > 1.seconds) {
            checkOffset()
        }
    }

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        DelayedRun.runDelayed(5.seconds) {
            checkOffset()
        }
    }

    private fun tryDisplayOffset(wasOffsetBefore: Boolean) {
        if (!config.warnAboutPcTimeOffset || !LorenzUtils.onHypixel) return
        val offsetMillis = offsetMillis ?: return
        if (offsetMillis.absoluteValue < 5.seconds) {
            if (wasOffsetBefore) {
                ChatUtils.chat("Congratulations! Your computer's clock is now accurate.")
            }
            return
        }

        ChatUtils.clickableLinkChat(
            "Your computer's clock is off by ${offsetMillis.absoluteValue.format()}.\n" +
                "§ePlease update your time settings. Many features may not function correctly until you do.\n" +
                "§eClick here for instructions on how to fix your clock.",
            offsetFixLinks ?: return,
            prefixColor = "§c",
        )
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Computer Time Offset")

        if (state != State.NORMAL) {
            event.addData("state is $state")
            return
        }

        val offset = offsetMillis ?: run {
            event.addIrrelevant("not calculated yet")
            return
        }

        val relevant = offset.absoluteValue > 1.seconds
        if (relevant) {
            event.addData {
                add(offset.toString())
                offsetFixLinks?.let {
                    add("Instructions on how to fix your clock can be found here:")
                    add(it)
                }
            }
        } else {
            event.addIrrelevant(offset.toString())
        }
    }
}
