package at.hannibal2.skyhanni.features.mining.eventtracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.BossbarData
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.BossbarUpdateEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ApiUtils
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.json.fromJson
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonPrimitive
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object MiningEventTracker {

    private val config get() = SkyHanniMod.feature.mining.miningEvent
    private val patternGroup = RepoPattern.group("mining.eventtracker")
    private const val MINING_API_NAME = "Soopy Mining Events"
    private const val MINING_URL = "https://api.soopy.dev/skyblock/chevents"

    private val miningFetchStatic = ApiUtils.StaticApiPath("$MINING_URL/get", MINING_API_NAME)
    private val miningSendStatic = ApiUtils.StaticApiPath("$MINING_URL/set", MINING_API_NAME)

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: §e§lPASSIVE EVENT §b§l§D§LBETTER TOGETHER §e§lRUNNING FOR §a§l06:34§r
     */
    private val bossbarPassivePattern by patternGroup.pattern(
        "bossbar.passive",
        "§e§lPASSIVE EVENT (?<event>.+) §e§lRUNNING FOR §a§l(?<time>\\S+)§r",
    )

    /**
     * REGEX-TEST: §e§lEVENT §6§LRAFFLE §e§lACTIVE IN §b§lFAR RESERVE §e§lfor §a§l01:03§r
     */
    private val bossbarActivePattern by patternGroup.pattern(
        "bossbar.active",
        "§e§lEVENT (?<event>.+) §e§lACTIVE IN (?<area>.+) §e§lfor §a§l(?<time>\\S+)§r",
    )

    /**
     * REGEX-TEST: §f                    §r§d§lBETTER TOGETHER STARTED!
     */
    private val eventStartedPattern by patternGroup.pattern(
        "started",
        "(?:§.)*\\s+(?:§.)+§l(?<event>.+) STARTED!",
    )

    /**
     * REGEX-TEST: §f                      §r§d§lBETTER TOGETHER ENDED!
     */
    private val eventEndedPattern by patternGroup.pattern(
        "ended",
        "(?:§.)*\\s+(?:§.)+§l(?<event>.+) ENDED!",
    )
    // </editor-fold>

    private val defaultCooldown = 1.minutes

    private var eventEndTime = SimpleTimeMark.farPast()
    private var lastSentEvent: MiningEventType? = null

    private var canRequestAt = SimpleTimeMark.farPast()

    var apiErrorCount = 0
    val apiError get() = apiErrorCount > 0

    @HandleEvent
    fun onWorldChange() {
        eventEndTime = SimpleTimeMark.farPast()
        lastSentEvent = null
    }

    @HandleEvent
    fun onBossbarChange(event: BossbarUpdateEvent) {
        if (!isMiningIsland()) return
        if (SkyBlockUtils.lastWorldSwitch.passedSince() < 5.seconds) return
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

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!isMiningIsland()) return

        eventStartedPattern.matchMatcher(event.message) {
            sendData(group("event"), null)
        }
        eventEndedPattern.matchMatcher(event.message) {
            lastSentEvent = null
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!config.enabled) return
        if (!config.outsideMining && !isMiningIsland()) return
        if (!canRequestAt.isInPast()) return

        SkyHanniMod.launchIOCoroutine {
            fetchData()
        }
    }

    private fun sendData(eventName: String, time: String?) {
        // Option to opt out of data sending
        if (!config.allowDataSharing) return

        // we now ignore mineshaft events.
        if (IslandType.MINESHAFT.isCurrent()) return
        // TODO fix this via regex
        if (eventName == "SLAYER QUEST") return

        val eventType = MiningEventType.fromEventName(eventName) ?: run {
            if (!config.enabled) return
            ErrorManager.logErrorWithData(
                Exception("UnknownMiningEvent"), "Unknown mining event detected from string $eventName",
                "eventName" to eventName,
                "bossbar" to BossbarData.getBossbar(),
                "serverType" to SkyBlockUtils.currentIsland,
                "fromChat" to (time == null),
            )
            return
        }

        if (!IslandType.DWARVEN_MINES.isCurrent() && eventType.dwarvenSpecific) return

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
            SkyBlockUtils.currentIsland,
            serverId,
            eventType,
            timeRemaining.inWholeMilliseconds,
            PlayerUtils.getUuid(),
        )
        val miningEventJson = ConfigManager.gson.toJson(miningEventData)

        if (apiError) {
            ChatUtils.debug("blocked sending mining event data: api error")
            return
        }
        SkyHanniMod.launchIOCoroutine {
            sendData(miningEventJson)
        }
    }

    private suspend fun sendData(json: String) {
        val response = ApiUtils.postJSON(miningSendStatic, json).takeIf { it.success } ?: return
        val data = response.data ?: return

        val formattedResponse = ConfigManager.gson.fromJson<MiningEventDataReceive>(data)
        if (!formattedResponse.success && config.enabled) {
            ErrorManager.logErrorWithData(
                Exception("PostFailure"), "Sending mining event data was unsuccessful",
                "cause" to formattedResponse.cause,
                "sentData" to json,
            )
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (apiError) canRequestAt = SimpleTimeMark.now()
    }

    private suspend fun fetchData() {
        canRequestAt = SimpleTimeMark.now() + defaultCooldown
        val receivedData = ApiUtils.getJSONResponse(miningFetchStatic, silentError = !SkyBlockUtils.debug) ?: run {
            apiErrorCount++
            canRequestAt = SimpleTimeMark.now() + 20.minutes
            return
        }
        val miningEventData = ConfigManager.gson.fromJson<MiningEventDataReceive>(receivedData)

        if (!miningEventData.success) {
            if (receivedData.toString().trim() == "{}") ChatUtils.chat(
                "§cFailed loading Mining Event data!\n" +
                    "§cPlease wait until the server-problem fixes itself! There is nothing else to do at the moment.",
                onlySendOnce = true,
            ) else ErrorManager.logErrorWithData(
                Exception("miningEventData.success = false"),
                "Failed to load Mining Event data!",
                "cause" to miningEventData.cause,
                "receivedData" to receivedData,
            )
        }

        apiErrorCount = 0
        canRequestAt = SimpleTimeMark.now() + miningEventData.data.updateIn.milliseconds
        MiningEventDisplay.updateData(miningEventData.data)
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(29, "mining.miningEvent.showType") { element ->
            if (element.asString == "BOTH") JsonPrimitive("ALL") else element
        }
    }

    // ignoring mineshaft here is intentional
    fun isMiningIsland() = IslandType.DWARVEN_MINES.isCurrent() || IslandType.CRYSTAL_HOLLOWS.isCurrent()
}
