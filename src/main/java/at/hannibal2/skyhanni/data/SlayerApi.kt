package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.events.slayer.SlayerChangeEvent
import at.hannibal2.skyhanni.events.slayer.SlayerProgressChangeEvent
import at.hannibal2.skyhanni.events.slayer.SlayerQuestCompleteEvent
import at.hannibal2.skyhanni.events.slayer.SlayerStateChangeEvent
import at.hannibal2.skyhanni.features.misc.pathfind.AreaNode
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.features.slayer.SlayerType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.minutes
import at.hannibal2.skyhanni.features.slayer.SlayerType as Type


@SkyHanniModule
object SlayerApi {

    val config get() = SkyHanniMod.feature.slayer
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
    fun isInBossFight() = state == ActiveQuestState.BOSS_FIGHT

    private class SlayerData {
        var currentState: ActiveQuestState? = ActiveQuestState.NO_ACTIVE_QUEST
        var currentStateRaw: String? = null
        var type: Type? = null
    }

    private fun getCurrentData() = if (RiftApi.inRift()) outsideRiftData else insideRiftData

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
    fun onDebug(event: DebugDataCollectEvent) {
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
    fun onChat(event: SkyHanniChatEvent.Allow) {
        val message = event.cleanMessage

        if (questStartPattern.matches(message)) {
            questStartTime = SimpleTimeMark.now()
        }

        if (questCompletePattern.matches(message)) {
            SlayerQuestCompleteEvent.post()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(event: SkyHanniTickEvent) {
        // wait with sending SlayerChangeEvent until profile is detected
        if (ProfileStorageData.profileSpecific == null) return

        val lines = getSlayerLines()

        val slayerQuest = lines.getOrNull(1).orEmpty()
        val slayerProgress = lines.getOrNull(2).orEmpty()
        if (event.isMod(5) || slayerQuest != latestCategory || latestProgress != slayerProgress) {
            updateArea()
        }
        if (slayerQuest != latestCategory) {
            val old = latestCategory
            latestCategory = slayerQuest
            tier = slayerQuest.split(" ").lastOrNull()?.romanToDecimalIfNecessary()
                ?: error(("latestCategory does not contain roman number or int: '$slayerQuest'"))
            SlayerChangeEvent(old, latestCategory).post()
        }

        if (latestProgress != slayerProgress) {
            SlayerProgressChangeEvent(latestProgress, slayerProgress).post()
            latestProgress = slayerProgress
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

    private fun getSlayerLines(): List<String> =
        ScoreboardData.sidebarLinesFormatted.dropWhile { it != "Slayer Quest" }
            .ifEmpty { TabWidget.SLAYER.lines.map { it.string } }.map { it.trim() }

    private fun updateSlayerState() {
        val lines = getSlayerLines()

        val slayerType = lines.getOrNull(1)
        val type = slayerType?.let { Type.getByName(it) }

        val slayerProgress = lines.getOrNull(2) ?: "no slayer"
        val newState = slayerProgress.removeColor()

        val slayerData = getCurrentData()
        if (slayerData.currentStateRaw == newState) return
        slayerData.type = type

        val old = slayerData.currentStateRaw ?: "no slayer"
        slayerData.currentStateRaw = newState
        val state = detectState(old, newState)
        if (slayerData.currentState == state) return
        ChatUtils.debug("${slayerData.currentState} -> $state")
        slayerData.currentState = state
        SlayerStateChangeEvent(state).post()
    }

    @HandleEvent(ScoreboardUpdateEvent::class, onlyOnSkyblock = true)
    fun onScoreboardChange() {
        updateSlayerState()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
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
        FAILED,
        SLAIN,
        NO_ACTIVE_QUEST,
    }

    private fun detectState(old: String, new: String): ActiveQuestState = when {
        new.inGrind() -> ActiveQuestState.GRINDING
        new.inBoss() -> ActiveQuestState.BOSS_FIGHT
        old.inBoss() && new.noSlayer() -> ActiveQuestState.FAILED
        new.bossSlain() -> ActiveQuestState.SLAIN
        else -> ActiveQuestState.NO_ACTIVE_QUEST
    }

    @HandleEvent(GraphAreaChangeEvent::class, priority = -1)
    fun onAreaChange() {
        currentAreaType = checkTypeForCurrentArea()
        updateArea()
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        with(trackerConfig) {
            ConditionalUtils.onToggle(revenantInGraveyard, voidgloomInNest, voidgloomInNoArea) {
                currentAreaType = checkTypeForCurrentArea()
                updateArea()
            }
        }
    }

    // TODO USE SH-REPO
    private fun checkTypeForCurrentArea() = when (SkyBlockUtils.graphArea) {
        "Graveyard" -> if (trackerConfig.revenantInGraveyard.get() && IslandType.HUB.isCurrent()) Type.REVENANT else null
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

        "Dragon's Nest" -> if (trackerConfig.voidgloomInNest.get() && IslandType.THE_END.isCurrent()) Type.VOID else null
        AreaNode.NO_AREA -> if (trackerConfig.voidgloomInNoArea.get() && IslandType.THE_END.isCurrent()) Type.VOID else null

        "Stronghold",
        "The Wasteland", // TODO check if we can remove this
        "Smoldering Tomb",
        -> Type.INFERNO

        "Stillgore Château",
        "Oubliette",
        -> Type.VAMPIRE

        else -> null
    }
}
