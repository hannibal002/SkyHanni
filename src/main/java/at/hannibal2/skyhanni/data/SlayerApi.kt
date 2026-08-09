package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
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
import at.hannibal2.skyhanni.utils.ServerTimeMark
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
    private val trackerConfig get() = config.itemProfitTracker

    private val patternGroup = RepoPattern.group("slayer.api")

    private const val GRACE_UPDATE_COUNT = 3

    /**
     * Cocoons take 5 seconds to burst, but we give +1 second of grace time to account for any delays in the scoreboard update
     */
    private val GRACE_COCOON_TIME = 6.seconds

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
    private val cocoonPattern by patternGroup.pattern(
        "cocooned.colorless",
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

    var latestWrongAreaWarningTime = SimpleTimeMark.farPast()

    /**
     * What is the current progress of the slayer boss? could be a string with text, or percentage, or x/x kills.
     */
    var latestProgress = ""

    /**
     * What slayer type should be fought in the current area we are in
     */
    var currentAreaType: SlayerType? = null

    /**
     * How many consecutive updates have we seen that are invalid?
     */
    private var invalidUpdates = 0

    /**
     * The last time we saw a cocoon message, used to ensure it doesn't get stuck in a state where we think we are cocooned when we are not
     */
    private var latestCocoonTime = ServerTimeMark.farPast()

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
    fun isInBossFight() = state == ActiveQuestState.BOSS_FIGHT

    private class SlayerData {
        var currentState: ActiveQuestState = ActiveQuestState.NO_ACTIVE_QUEST
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
            add("latestProgress: '$latestProgress'")

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
                ChatUtils.debug("SlayerApi: Slayer quest complete detected, posting SlayerQuestCompleteEvent")
                SlayerQuestCompleteEvent.post()
            }
            questFailedPattern.matches(message) -> {
                val data = getCurrentData()
                ChatUtils.debug("SlayerApi: Slayer quest failed, posting SlayerStateChangeEvent")
                data.currentState = FAILED
                data.currentStateRaw = "no slayer"
                SlayerStateChangeEvent(FAILED).post()
            }
            cocoonPattern.matches(message) -> {
                val data = getCurrentData()
                ChatUtils.debug("SlayerApi: Slayer boss cocooned, posting SlayerStateChangeEvent")
                data.currentState = COCOONED
                data.currentStateRaw = "cocooned"
                latestCocoonTime = ServerTimeMark.now()
                SlayerStateChangeEvent(COCOONED).post()
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

    // returns null if no slayer quest is found, and exception if the parsing fails
    private fun getParsedSlayerOrNull(lines: List<String>): ParsedSlayer? {
        val questIndex = lines.indexOfFirst { Type.getByName(it) != null }
        if (questIndex == -1) return null

        val category = lines[questIndex]
        val type = Type.getByName(category) ?: return null
        val progress = lines.getOrNull(questIndex + 1) ?: throw SlayerParseException("Progress line missing for category '$category'")

        val tierString = category.substringAfterLast(' ', "")
        val parsedTier =
            tierString.romanToDecimalIfNecessaryOrNull() ?: throw SlayerParseException("Failed to parse tier from category '$category'")

        return ParsedSlayer(
            type = type,
            category = category,
            tier = parsedTier,
            progress = progress,
        )
    }

    private fun updateSlayerState() {
        if (ProfileStorageData.profileSpecific == null) return

        val (lines, source) = getSlayerLines()
        val parsed = try {
            getParsedSlayerOrNull(lines)
        } catch (e: SlayerParseException) {
            invalidUpdates++
            if (invalidUpdates == GRACE_UPDATE_COUNT) {
                val message = "Slayer Exception: ${e.message}"
                ErrorManager.skyHanniError(
                    message,
                    "lines" to lines,
                    "source" to source.name,
                )
            }
            return
        }
        invalidUpdates = 0

        val progress = parsed?.progress ?: "no slayer"

        updateCategory(parsed)
        updateProgress(progress)
        updateActiveState(parsed, progress)
        updateArea()
    }

    private fun updateCategory(parsed: ParsedSlayer?) {
        val category = parsed?.category.orEmpty()
        if (category == latestCategory) return

        val old = latestCategory
        latestCategory = category
        tier = parsed?.tier ?: 0

        ChatUtils.debug("SlayerApi: $old -> $category")
        SlayerChangeEvent(old, category).post()
    }

    private fun updateProgress(progress: String) {
        if (progress == latestProgress) return

        ChatUtils.debug("SlayerApi: $latestProgress -> $progress")
        SlayerProgressChangeEvent(latestProgress, progress).post()
        latestProgress = progress
    }

    private fun updateActiveState(parsed: ParsedSlayer?, progress: String) {
        val data = getCurrentData()
        data.type = parsed?.type

        val oldStateRaw = data.currentStateRaw ?: "no slayer"
        if (oldStateRaw == progress) return

        data.currentStateRaw = progress

        var newState = detectState(progress)

        val cocooned = data.currentState == COCOONED && latestCocoonTime.passedSince() <= GRACE_COCOON_TIME
        if (cocooned && (newState == NO_ACTIVE_QUEST || newState == SLAIN)) {
            ChatUtils.debug("SlayerApi: Cocooned state detected, overriding $newState to COCOONED")
            newState = COCOONED
        }

        // If the player kills the boss immediately after the boss spawns
        if (data.currentState == BOSS_FIGHT && newState == GRINDING) {
            ChatUtils.debug("SlayerApi: Intermediate state change detected: BOSS_FIGHT -> SLAIN -> GRINDING")
            SlayerStateChangeEvent(SLAIN).post()
        }
        if (data.currentState == GRINDING && newState == SLAIN) {
            ChatUtils.debug("SlayerApi: Intermediate state change detected: GRINDING -> BOSS_FIGHT -> SLAIN")
            SlayerStateChangeEvent(BOSS_FIGHT).post()
        }

        if (newState != data.currentState) {
            ChatUtils.debug("SlayerApi: ${data.currentState} -> $newState")
            data.currentState = newState
            SlayerStateChangeEvent(newState).post()
        }
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

    enum class ActiveQuestState {
        GRINDING, // spawning, collecting combat xp
        BOSS_FIGHT,
        FAILED,
        SLAIN,
        COCOONED,
        NO_ACTIVE_QUEST,
    }

    private fun detectState(new: String): ActiveQuestState = when {
        new.inGrind() -> GRINDING
        new.inBoss() -> BOSS_FIGHT
        new.bossSlain() -> SLAIN
        else -> NO_ACTIVE_QUEST
    }

    @HandleEvent(priority = HandleEvent.LOW)
    private fun onAreaChange() {
        currentAreaType = checkTypeForCurrentArea()
        updateArea()
    }

    @HandleEvent
    private fun onConfigLoad() {
        with(trackerConfig) {
            ConditionalUtils.onToggle(revenantInGraveyard, voidgloomInNest, voidgloomInNoArea) {
                currentAreaType = checkTypeForCurrentArea()
                updateArea()
            }
        }
    }

    @HandleEvent
    private fun onWorldChange() {
        // Using outsideRiftData since rift does not have slayer cocoon
        // and using getCurrentData is ambiguous while changing worlds (inside/outside rift)
        val data = outsideRiftData
        if (data.currentState != COCOONED) return
        ChatUtils.debug("SlayerApi: World change detected, resetting cocooned state")
        data.currentState = NO_ACTIVE_QUEST
        data.currentStateRaw = null
        latestCocoonTime = ServerTimeMark.farPast()
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
        val type: Type,
        val category: String,
        val tier: Int,
        val progress: String,
    )

    private class SlayerParseException(message: String) : Exception(message)
}
