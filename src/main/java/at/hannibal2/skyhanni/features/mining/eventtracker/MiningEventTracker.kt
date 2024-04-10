package at.hannibal2.skyhanni.features.mining.eventtracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.BossbarData
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.BossbarUpdateEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.fromJson
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class MiningEventTracker {
    private val config get() = SkyHanniMod.feature.mining.miningEvent

    private val patternGroup = RepoPattern.group("mining.eventtracker")
    private val bossbarPassivePattern by patternGroup.pattern(
        "bossbar.passive",
        "§e§lPASSIVE EVENT (?<event>.+) §e§lRUNNING FOR §a§l(?<time>\\S+)§r"
    )
    private val bossbarActivePattern by patternGroup.pattern(
        "bossbar.active",
        "§e§lEVENT (?<event>.+) §e§lACTIVE IN (?<area>.+) §e§lfor §a§l(?<time>\\S+)§r"
    )
    private val eventStartedPattern by patternGroup.pattern(
        "started",
        "(?:§.)*\\s+(?:§.)+§l(?<event>.+) STARTED!"
    )
    private val eventEndedPattern by patternGroup.pattern(
        "ended",
        "(?:§.)*\\s+(?:§.)+§l(?<event>.+) ENDED!"
    )

    private val defaultCooldown = 1.minutes

    private var eventEndTime = SimpleTimeMark.farPast()
    private var lastSentEvent: MiningEventType? = null

    private var canRequestAt = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        eventEndTime = SimpleTimeMark.farPast()
        lastSentEvent = null
    }

    @SubscribeEvent
    fun onBossbarChange(event: BossbarUpdateEvent) {
        if (!LorenzUtils.inAdvancedMiningIsland()) return
        if (LorenzUtils.lastWorldSwitch.passedSince() < 5.seconds) return
        if (!eventEndTime.isInPast()) {
            return
        }

        bossbarPassivePattern.matchMatcher(event.bossbar) {
            sendData(group("event"), group("time"))
        }
        bossbarActivePattern.matchMatcher(event.bossbar) {
            sendData(group("event"), group("time"))
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inAdvancedMiningIsland()) return

        eventStartedPattern.matchMatcher(event.message) {
            sendData(group("event"), null)
        }
        eventEndedPattern.matchMatcher(event.message) {
            lastSentEvent = null
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!config.enabled) return
        if (!LorenzUtils.inSkyBlock || (!config.outsideMining && !LorenzUtils.inAdvancedMiningIsland())) return
        if (!canRequestAt.isInPast()) return

        fetchData()
    }

    private fun sendData(eventName: String, time: String?) {
        val eventType = MiningEventType.fromEventName(eventName) ?: run {
            if (!config.enabled) return
            ErrorManager.logErrorWithData(
                Exception("UnknownMiningEvent"), "Unknown mining event detected from string $eventName",
                "eventName" to eventName,
                "bossbar" to BossbarData.getBossbar(),
                "serverType" to LorenzUtils.skyBlockIsland,
                "fromChat" to (time == null)
            )
            return
        }

        if (!IslandType.DWARVEN_MINES.isInIsland() && eventType.dwarvenSpecific) return

        if (lastSentEvent == eventType) return
        lastSentEvent = eventType

        val timeRemaining = if (time == null) {
            eventType.defaultLength
        } else {
            TimeUtils.getDuration(time)
        }
        eventEndTime = SimpleTimeMark.now() + timeRemaining

        val serverId = HypixelData.serverId ?: return

        val miningEventData = MiningEventDataSend(
            LorenzUtils.skyBlockIsland,
            serverId,
            eventType,
            timeRemaining.inWholeMilliseconds,
            LorenzUtils.getPlayerUuid()
        )
        val miningEventJson = ConfigManager.gson.toJson(miningEventData)
        SkyHanniMod.coroutineScope.launch {
            sendData(miningEventJson)
        }
    }

    private fun sendData(json: String) {
        val response = APIUtil.postJSON("https://api.soopy.dev/skyblock/chevents/set", json)
        if (!response.success) return

        val formattedResponse = ConfigManager.gson.fromJson<MiningEventDataReceive>(response.data)
        if (!formattedResponse.success) {
            if (!config.enabled) return
            ErrorManager.logErrorWithData(
                Exception("PostFailure"), "Sending mining event data was unsuccessful",
                "cause" to formattedResponse.cause,
                "sentData" to json
            )
        }
    }

    private fun fetchData() {
        canRequestAt = SimpleTimeMark.now() + defaultCooldown
        SkyHanniMod.coroutineScope.launch {
            val data = APIUtil.getJSONResponse("https://api.soopy.dev/skyblock/chevents/get")
            val miningEventData = ConfigManager.gson.fromJson(data, MiningEventDataReceive::class.java)

            if (!miningEventData.success) {
                ErrorManager.logErrorWithData(
                    Exception("PostFailure"), "Sending mining event data was unsuccessful",
                    "cause" to miningEventData.cause,
                    "recievedData" to data
                )
                return@launch
            }

            canRequestAt = SimpleTimeMark.now() + miningEventData.data.updateIn.milliseconds

            MiningEventDisplay.updateData(miningEventData.data)
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(29, "mining.miningEvent.showType") { element ->
            if (element.asString == "BOTH") JsonPrimitive("ALL") else element
        }
    }
}
