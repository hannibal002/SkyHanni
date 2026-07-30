package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.events.slayer.SlayerChangeEvent
import at.hannibal2.skyhanni.events.slayer.SlayerProgressChangeEvent
import at.hannibal2.skyhanni.events.slayer.SlayerQuestCompleteEvent
import at.hannibal2.skyhanni.events.slayer.SlayerStateChangeEvent
import at.hannibal2.skyhanni.features.misc.pathfind.AreaNode
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.features.slayer.SlayerType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessaryOrNull
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import at.hannibal2.skyhanni.features.slayer.SlayerType as Type


@SkyHanniModule
object SlayerApi {

    val config get() = SkyHanniMod.feature.slayer
    val debugConfig get() = SkyHanniMod.feature.dev.debug.slayerDebug

    private val trackerConfig get() = config.itemProfitTracker

    private val patternGroup = RepoPattern.group("slayer.api")

    // <editor-fold desc="Patterns">
    /**
     * WRAPPED-REGEX-TEST: "  SLAYER QUEST STARTED!"
     */
    private val questStartPattern by patternGroup.pattern(
        "quest.start",
        "\\s*SLAYER QUEST STARTED!",
    )

    /**
     * WRAPPED-REGEX-TEST: "  SLAYER QUEST COMPLETE!"
     */
    private val questCompletePattern by patternGroup.pattern(
        "quest.complete",
        "\\s*SLAYER QUEST COMPLETE!",
    )

    /**
     * WRAPPED-REGEX-TEST: "  SLAYER QUEST FAILED!"
     */
    private val questFailedPattern by patternGroup.pattern(
        "quest.failed",
        "\\s*SLAYER QUEST FAILED!",
    )

    /**
     * WRAPPED-REGEX-TEST: "  YOU COCOONED YOUR SLAYER BOSS"
     */
    private val slayerCocoonPattern by patternGroup.pattern(
        "cocooned",
        "\\s+YOU COCOONED YOUR SLAYER BOSS",
    )
    // </editor-fold>

    private val nameCache = TimeLimitedCache<Pair<NeuInternalName, Int>, Pair<String, Double>>(1.minutes)

    var questStartTime = SimpleTimeMark.farPast()

    /**
     * Are we having the right slayer quest in the right area?
     */
    var isInCorrectArea = false

    /**
     * Are we in any area that is good for any slayer? - ignoring if we have an active quest
     */
    var isInAnyArea = false

    // for an enum, use activeType
    var latestCategory = ""
    var tier = 0

    var latestWrongAreaWarning = SimpleTimeMark.farPast()

    /**
     * What is the current progress of the slayer boss? could be a string with text, or percentage, or x/x kills.
     */
    var latestProgress = ""

    /**
     * What slayer type should be fought in the current area we are in
     */
    var currentAreaType: SlayerType? = null

    private val outsideRiftData = SlayerData()
    private val insideRiftData = SlayerData()

    /**
     * what are we currently doing with the slayer? grinding,
     */
    val state get() = getCurrentData().currentState

    /**
     * The enum type of slayer currently doing
     */
    val activeType get() = getCurrentData().type

    /**
     * Are we currently fighting a slayer boss?
     */
    fun isInBossFight() = state == ActiveQuestState.BOSS_FIGHT || state == ActiveQuestState.COCOONED

    /**
     * For how many scoreboard updates have we seen a category that is invalid?
     */
    private var invalidCategoryTicks = 0

    // This Timer is mostly just a fail-safe so it doesn't get stuck in COCOONED state
    private var cocoonTimestamp: SimpleTimeMark = SimpleTimeMark.farPast()

    private class SlayerData {
        var currentState: ActiveQuestState? = ActiveQuestState.NO_ACTIVE_QUEST
        var currentStateRaw: String? = null
        var type: Type? = null
    }

    private fun getCurrentData() = if (RiftApi.inRift()) insideRiftData else outsideRiftData

    /**
     * Do we have a slayer quest in the scoreboard?
     */
    fun hasActiveQuest() = latestCategory != ""

    fun getItemNameAndPrice(internalName: NeuInternalName, amount: Int): Pair<String, Double> =
        nameCache.getOrPut(internalName to amount) {
            val price = internalName.getPrice()
            val npcPrice = internalName.getNpcPriceOrNull() ?: 0.0
            val maxPrice = npcPrice.coerceAtLeast(price)
            val totalPrice = maxPrice * amount

            internalName.getPriceName(amount, pricePer = maxPrice) to totalPrice
        }

    @HandleEvent
    private fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Slayer")

        if (!hasActiveQuest()) {
            event.addIrrelevant("no active slayer quest")
            return
        }

        event.addData {
            add("activeType: $activeType")
            add("isInCorrectArea: $isInCorrectArea")
            if (!isInCorrectArea) {
                add("currentAreaType: $currentAreaType")
                add(" graph area: ${SkyBlockUtils.graphArea}")
                add(" /shtestwaypoint ${PlayerUtils.blockPosition().toLocalFormat()} pathfind")
            }
            add("isInAnyArea: $isInAnyArea")
            add("latestProgress: '${latestProgress.removeColor()}'")

            val data = getCurrentData()
            add("active data:")
            add("  type: ${data.type}")
            add("  currentState: ${data.currentState}")
            add("  currentStateRaw: ${data.currentStateRaw}")
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        val message = event.cleanMessage

        when {
            questStartPattern.matches(message) -> {
                questStartTime = SimpleTimeMark.now()
            }
            questCompletePattern.matches(message) -> {
                SlayerQuestCompleteEvent.post()
            }
            questFailedPattern.matches(message) -> {
                val data = getCurrentData()
                if (data.currentState != FAILED) {
                    data.currentState = FAILED
                    SlayerStateChangeEvent(FAILED).post()
                }
            }
            slayerCocoonPattern.matches(message) -> {
                val data = getCurrentData()
                if (data.currentState != COCOONED) {
                    cocoonTimestamp = SimpleTimeMark.now()
                    data.currentStateRaw = "cocooned"
                    data.currentState = COCOONED
                    SlayerStateChangeEvent(COCOONED).post()
                }
            }
        }
    }

    private fun updateArea() {
        if (SkyBlockUtils.isStrandedProfile) {
            isInAnyArea = true
            isInCorrectArea = true
        } else {
            isInAnyArea = currentAreaType != null
            isInCorrectArea = currentAreaType == activeType && currentAreaType != null
        }
    }

    private fun getSlayerLines(): Pair<List<String>, SlayerLinesSource> {
        val scoreboardLines = ScoreboardData.sidebarLinesFormatted
            .map { it.removeColor().trim() }
            .dropWhile { it != "Slayer Quest" }
        if (scoreboardLines.isNotEmpty()) return scoreboardLines to SlayerLinesSource.SCOREBOARD

        val tabLines = TabWidget.SLAYER.lines.map { it.string.removeColor().trim() }
        if (tabLines.isNotEmpty()) return tabLines to SlayerLinesSource.TAB

        return emptyList<String>() to SlayerLinesSource.NONE
    }

    private fun getParsedSlayer(lines: List<String>): ParsedSlayer? {
        val questIndex = lines.indexOfFirst { Type.getByName(it) != null }
        if (questIndex == -1) return null

        return ParsedSlayer(
            type = Type.getByName(lines[questIndex]),
            category = lines[questIndex],
            progress = lines.getOrNull(questIndex + 1) ?: "no slayer",
        )
    }

    private fun updateSlayerState() {
        if (ProfileStorageData.profileSpecific == null) return

        val (lines, source) = getSlayerLines()
        val parsed = getParsedSlayer(lines)

        val category = parsed?.category.orEmpty()
        val progress = parsed?.progress ?: "no slayer"

        if (category != latestCategory) {
            val tierString = category.substringAfterLast(' ', "")
            val parsedTier = tierString.romanToDecimalIfNecessaryOrNull()

            if (category.isNotEmpty() && parsedTier == null) {
                invalidCategoryTicks++

                if (invalidCategoryTicks >= 2) {
                    ErrorManager.skyHanniError(
                        "latestCategory does not contain roman number or int: '$category'",
                        "lines" to lines,
                        "source" to source.name,
                    )
                }
                return
            }

            invalidCategoryTicks = 0

            val old = latestCategory
            latestCategory = category
            tier = parsedTier ?: 0

            SlayerChangeEvent(old, category).post()
        } else {
            invalidCategoryTicks = 0
        }

        if (progress != latestProgress) {
            SlayerProgressChangeEvent(latestProgress, progress).post()
            latestProgress = progress
        }

        val data = getCurrentData()
        data.type = parsed?.type

        val oldStateRaw = data.currentStateRaw ?: "no slayer"
        if (oldStateRaw != progress) {
            data.currentStateRaw = progress

            val newState = detectState(data.currentState, oldStateRaw, progress)
            if (newState != data.currentState) {
                ChatUtils.debug("${data.currentState} -> $newState")
                data.currentState = newState
                SlayerStateChangeEvent(newState).post()
            }
        }

        updateArea()
    }

    @HandleEvent(ScoreboardUpdateEvent::class, onlyOnSkyblock = true)
    private fun onScoreboardChange() {
        updateSlayerState()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.SLAYER)) return
        updateSlayerState()
    }

    private fun String.inGrind() = contains("Combat") || contains("Kills")
    private fun String.inBoss() = this == "Slay the boss!"
    private fun String?.bossSlain() = this == "Boss slain!"
    private fun String.noSlayer() = this == "no slayer"

    enum class ActiveQuestState {
        GRINDING, // spawning, collecting combat xp
        BOSS_FIGHT,
        COCOONED,
        FAILED,
        SLAIN,
        NO_ACTIVE_QUEST,
    }

    private fun detectState(currentState: ActiveQuestState?, old: String, new: String): ActiveQuestState = when {
        // The scoreboard says "Boss slain!" While the boss is cocooned
        // This is 6 seconds instead of 5 seconds just to be safe
        new.bossSlain() && currentState == COCOONED && cocoonTimestamp.passedSince() <= 6.seconds -> COCOONED
        new.inGrind() -> GRINDING
        new.inBoss() -> BOSS_FIGHT
        old.inBoss() && new.noSlayer() -> FAILED
        new.bossSlain() -> SLAIN
        else -> NO_ACTIVE_QUEST
    }

    @HandleEvent(GraphAreaChangeEvent::class, priority = -1)
    private fun onAreaChange() {
        currentAreaType = checkTypeForCurrentArea()
        updateArea()
    }

    @HandleEvent
    private fun onIslandLeave() {
        currentAreaType = null
        updateArea()
        val data = getCurrentData()
        if (data.currentState == COCOONED) {
            data.currentStateRaw = null
            data.currentState = NO_ACTIVE_QUEST
            SlayerStateChangeEvent(NO_ACTIVE_QUEST).post()
        }
    }

    @HandleEvent(ConfigLoadEvent::class)
    private fun onConfigLoad() {
        with(trackerConfig) {
            ConditionalUtils.onToggle(revenantInGraveyard, voidgloomInNest, voidgloomInNoArea) {
                currentAreaType = checkTypeForCurrentArea()
                updateArea()
            }
        }
    }

    // TODO USE SH-REPO
    private fun checkTypeForCurrentArea() = when (SkyBlockUtils.graphArea) {
        "Graveyard" -> if (trackerConfig.revenantInGraveyard.get() && IslandType.HUB.isInIsland()) Type.REVENANT else null
        "Revenant Cave" -> Type.REVENANT

        "Spider Mound",
        "Arachne's Burrow",
        "Arachne's Sanctuary",
        "Burning Desert",
        -> Type.TARANTULA

        "Ruins",
        "Howling Cave",
        "Soul Cave",
        "Spirit Cave",
        -> Type.SVEN

        "Void Sepulture",
        "Zealot Bruiser Hideout",
        -> Type.VOID

        "Dragon's Nest" -> if (trackerConfig.voidgloomInNest.get() && IslandType.THE_END.isInIsland()) Type.VOID else null
        AreaNode.NO_AREA -> if (trackerConfig.voidgloomInNoArea.get() && IslandType.THE_END.isInIsland()) Type.VOID else null

        "Stronghold",
        "The Wasteland", // TODO check if we can remove this
        "Smoldering Tomb",
        -> Type.INFERNO

        "Stillgore Château",
        "Oubliette",
        -> Type.VAMPIRE

        else -> null
    }

    private enum class SlayerLinesSource {
        NONE,
        SCOREBOARD,
        TAB,
    }

    private data class ParsedSlayer(
        val type: Type?,
        val category: String,
        val progress: String,
    )

    @HandleEvent
    private fun onSlayerChange(event: SlayerChangeEvent) {
        if (!debugConfig) return
        ChatUtils.chat("SlayerChangeEvent: ${event.oldSlayer} -> ${event.newSlayer}")
    }

    @HandleEvent
    private fun onSlayerStateChange(event: SlayerStateChangeEvent) {
        if (!debugConfig) return
        ChatUtils.chat("SlayerStateChangeEvent: ${event.state}")
    }

    @HandleEvent
    private fun onSlayerProgressChange(event: SlayerProgressChangeEvent) {
        if (!debugConfig) return
        ChatUtils.chat("SlayerProgressChangeEvent: ${event.oldProgress} -> ${event.newProgress}")
    }

    @HandleEvent
    private fun onSlayerQuestComplete() {
        if (!debugConfig) return
        ChatUtils.chat("SlayerQuestCompleteEvent")
    }
}
