package at.hannibal2.skyhanni.features.mining.eventtracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.BossbarData
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.BossbarUpdateEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.getBoolean
import at.hannibal2.skyhanni.utils.getStringOrValue
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
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

    private var lastRequestSent = SimpleTimeMark.farPast()
    private var lastWorldSwitch = SimpleTimeMark.farPast()
    private var eventEndTime = SimpleTimeMark.farPast()

    private var lastSentEvent: MiningEvent? = null

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        lastRequestSent = SimpleTimeMark.now()
        lastWorldSwitch = SimpleTimeMark.farPast()
        eventEndTime = SimpleTimeMark.farPast()

        lastSentEvent = null
    }

    @SubscribeEvent
    fun onBossbarChange(event: BossbarUpdateEvent) {
        if (!isEnabled()) return
        if (lastWorldSwitch.passedSince() < 2.seconds) return
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
        if (!isEnabled()) return

        eventStartedPattern.matchMatcher(event.message) {
            sendData(group("event"), null)
        }
        eventEndedPattern.matchMatcher(event.message) {
            lastSentEvent = null
        }
    }

    private fun sendData(eventName: String, time: String?) {
        val eventType = MiningEvent.fromBossbarName(eventName)
        if (lastSentEvent == eventType) return
        if (eventType == null) {
            ErrorManager.logErrorWithData(
                Exception("UnknownMiningEvent"), "Unknown mining event detected from string $eventName",
                "eventName" to eventName,
                "bossbar" to BossbarData.getBossbar(),
                "serverType" to LorenzUtils.skyBlockIsland,
                "fromChat" to (time == null)
            )
            return
        }
        lastSentEvent = eventType

        val timeRemaining = if (time == null) {
            eventType.defaultLength
        } else {
            TimeUtils.getDuration(time)
        }
        eventEndTime = SimpleTimeMark.now() + timeRemaining

        val serverId = HypixelData.getCurrentServerId()
        if (serverId == null) {
            ErrorManager.logErrorWithData(
                Exception("NoServerId"), "Could not find server id",
                "islandType" to LorenzUtils.skyBlockIsland,
                "tablist" to TabListData.getTabList(),
                "scoreboard" to ScoreboardData.sidebarLinesFormatted
            )
            return
        }

        val miningEventData = MiningEventData(
            LorenzUtils.skyBlockIsland,
            serverId,
            eventType,
            timeRemaining.inWholeMilliseconds,
            LorenzUtils.getPlayerUuid()
        )
        val miningEventJson = ConfigManager.gson.toJson(miningEventData)
//         //todo remove
//         println("\n```json$miningEventJson```")
        SkyHanniMod.coroutineScope.launch {
            sendData(miningEventJson)
        }
    }

    private fun isEnabled() = (IslandType.DWARVEN_MINES.isInIsland() || IslandType.CRYSTAL_HOLLOWS.isInIsland()) && config.sendData
//         && config.enabled


    private fun sendData(json: String) {
        val response = APIUtil.postJSON("https://api.soopy.dev/skyblock/chevents/set", json)
        if (!response.success) return
        val success = response.data.getBoolean("success")
        if (!success) {
            val cause = response.data.getStringOrValue("cause", "unknown")
            ErrorManager.logErrorWithData(
                Exception("PostFailure"), "Sending mining event data was unsuccessful",
                "cause" to cause,
                "sentData" to json
            )
        }
    }
}
