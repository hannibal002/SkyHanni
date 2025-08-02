package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.EnumUtils.next
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import org.apache.commons.net.ntp.NTPUDPClient
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@SkyHanniModule
object ComputerTimeOffset {

    private val devConfig get() = SkyHanniMod.feature.dev
    private val config get() = SkyHanniMod.feature.misc
    private val timeCheckMutex = Mutex()
    private val timeoutMap: MutableMap<String, Int> = mutableMapOf()
    private val offsetFixLink by lazy {
        when {
            OSUtils.isWindows -> "https://support.microsoft.com/en-us/windows/dfaa7122-479f-5b98-2a7b-fa0b6e01b261"
            OSUtils.isLinux -> "https://unix.stackexchange.com/a/79116"
            OSUtils.isMac -> "https://support.apple.com/guide/mac-help/mchlp2996/mac"
            OSUtils.isSolaris -> "https://docs.oracle.com/cd/E53394_01/html/E54798/sysressysinfo-11048.html"
            else -> null
        }
    }

    private var state = State.NORMAL
    private var offsetDuration: Duration? = null
    private var lastSystemTime = System.currentTimeMillis()
    private var timeoutWarned = SimpleTimeMark.farPast()

    enum class State(val duration: Duration) {
        NORMAL(1.seconds),
        SLOW(10.seconds),
        TOTALLY_OFF(Duration.INFINITE),
    }

    init {
        SkyHanniMod.launchIOCoroutine {
            while (state != State.TOTALLY_OFF) {
                delay(state.duration)
                detectTimeChange()
            }
        }
    }

    private fun tryCheckOffset() {
        // probably a problem when the response somehow took longer than 1s?
        if (!timeCheckMutex.tryLock()) {
            state = state.next() ?: error("state is already TOTALLY_OFF")
            if (state == State.TOTALLY_OFF) ErrorManager.logErrorStateWithData(
                "Error when checking Computer Time Offset",
                "trying to check again even though the previous check is still not done",
            ) else if (state == State.SLOW) ChatUtils.chat(
                "Computer Time Offset calculation took longer than normal. Checking less often now.",
            )
            return
        } else timeCheckMutex.unlock() // Immediate release, we only want to check if it's already running

        val wasOffsetBefore = (offsetDuration?.absoluteValue ?: 0.seconds) > 5.seconds
        SkyHanniMod.launchIOCoroutineWithMutex(timeCheckMutex) {
            offsetDuration = getNtpOffset(devConfig.ntpServer)
            offsetDuration?.let {
                tryDisplayOffset(wasOffsetBefore)
            }
        }
    }

    private fun getNtpOffset(ntpServer: String): Duration? = runCatching {
        val timeouts = timeoutMap[ntpServer] ?: 0
        if (timeouts > 10) {
            if (timeoutWarned.passedSince() > 10.minutes) {
                timeoutMap[ntpServer] = 0
                timeoutWarned = SimpleTimeMark.now()
                ChatUtils.clickableChat(
                    "NTP server $ntpServer is not responding ($timeouts failures). Check your connection, " +
                        "try disconnecting from any VPNs/proxies, or click here to change NTP servers.",
                    hover = "Click to open Dev Config",
                    onClick = { devConfig::ntpServer.jumpToEditor() }
                )
            }
            return@runCatching null
        }
        NTPUDPClient().apply {
            setDefaultTimeout(10.seconds.toJavaDuration())
        }.use { client ->
            val address = InetAddress.getByName(ntpServer)
            val timeInfo = client.getTime(address)
            timeInfo.computeDetails()
            timeInfo.offset.milliseconds
        }
    }.onFailure { e ->
        if (e is SocketTimeoutException || e is UnknownHostException) {
            timeoutMap.addOrPut(ntpServer, 1)
            return@onFailure
        } else if (SkyBlockUtils.inSkyBlock && config.warnAboutPcTimeOffset) ErrorManager.logErrorWithData(
            e,
            "Failed to get NTP offset",
            "server" to ntpServer,
        ) else SkyHanniMod.logger.error(e.stackTraceToString())
    }.getOrNull()

    private fun detectTimeChange() {
        val currentSystemTime = System.currentTimeMillis()
        val timeDifference = (currentSystemTime - lastSystemTime).milliseconds
        lastSystemTime = currentSystemTime

        val expectedDuration = 1.seconds
        val deviation = timeDifference - expectedDuration

        if (deviation.absoluteValue > 1.seconds) {
            tryCheckOffset()
        }
    }

    private fun tryDisplayOffset(wasOffsetBefore: Boolean) {
        if (!config.warnAboutPcTimeOffset || !SkyBlockUtils.onHypixel) return
        val offsetDuration = offsetDuration?.absoluteValue?.takeIf {
            it >= 5.seconds
        } ?: run {
            if (wasOffsetBefore) ChatUtils.chat("Congratulations! Your computer's clock is now accurate.")
            return
        }

        ChatUtils.clickableLinkChat(
            "Your computer's clock is off by ${offsetDuration.format()}.\n" +
                "§ePlease update your time settings. Many features may not function correctly until you do.\n" +
                "§eClick here for instructions on how to fix your clock.",
            url = offsetFixLink ?: return,
            prefixColor = "§c",
            replaceSameMessage = true,
        )
    }

    @HandleEvent(ProfileJoinEvent::class)
    fun onProfileJoin() = DelayedRun.runDelayed(5.seconds, ::tryCheckOffset)

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Computer Time Offset")

        if (state != State.NORMAL) {
            event.addData("state is $state")
            return
        }

        val offset = offsetDuration ?: run {
            event.addIrrelevant("not calculated yet")
            return
        }

        val relevant = offset.absoluteValue > 1.seconds
        if (relevant) {
            event.addData {
                add(offset.toString())
                offsetFixLink?.let {
                    add("Instructions on how to fix your clock can be found here:")
                    add(it)
                }
            }
        } else {
            event.addIrrelevant(offset.toString())
        }
    }
}
