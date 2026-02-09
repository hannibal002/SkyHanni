package at.hannibal2.skyhanni.features.garden.leaderboarddisplays

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteLeaderboardConfigApi
import at.hannibal2.skyhanni.config.features.garden.leaderboards.EliteLeaderboardConfigApi.getConfigFromClass
import at.hannibal2.skyhanni.config.features.garden.leaderboards.generics.EliteDisplayGenericConfig.LeaderboardTextEntry
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.clearCategories
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getAmount
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getLastPlayer
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getLeaderboardPosition
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getNextPlayer
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getRankGoal
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.isUnranked
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.leaderboardMinAmount
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.loadingLeaderboardMutex
import at.hannibal2.skyhanni.data.garden.FarmingWeightData
import at.hannibal2.skyhanni.data.garden.FarmingWeightData.getWeight
import at.hannibal2.skyhanni.data.garden.FarmingWeightData.openWebsite
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addVerticalSpacer
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import at.hannibal2.skyhanni.utils.renderables.primitives.empty
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds

@Suppress("TooManyFunctions")
abstract class EliteLeaderboardDisplayBase<E : Enum<E>, T : EliteLeaderboardType.WithEnum<E>>(
    private val typeClass: KClass<out T>,
    private val createType: (E, EliteLeaderboardMode) -> EliteLeaderboardType,
    private val name: String
) {
    protected val configBase get() = GardenApi.config.eliteFarmersLeaderboards
    private val config get() = baseClass?.let { getConfigFromClass(it) }
    @Suppress("Unchecked_cast")
    private val baseClass: KClass<out EliteLeaderboardType>?
        get() = typeClass as? KClass<out EliteLeaderboardType>


    var lastUpdate = SimpleTimeMark.farPast()
    protected var inventoryOpen = false
    protected var display = emptyList<Renderable>()
    protected var apiError = false
    protected var amount: Double? = null
    private var leaderboardPos: Int? = null
    private var nextPlayer: Pair<String, Double>? = null

    protected abstract var currentMode: EliteLeaderboardMode
    protected abstract var currentEnum: E?

    abstract fun getDefaultEnum(): E?

    val errorMessage by lazy {
        listOf(
            "§cFarming Weight error: Cannot load",
            "§cdata from Elite Farmers!",
            "§eRejoin the garden or",
            "§eclick here to fix it.",
        ).map {
            Renderable.clickable(
                it,
                tips = listOf("§eClick here to reload the data right now!"),
                onLeftClick = ::reset,
            )
        }
    }

    open val currentLeaderboardType: EliteLeaderboardType?
        get() = (currentEnum ?: getDefaultEnum())?.let { createType(it, currentMode) }

    fun update(overrideCooldown: Boolean = false) {
        // we want to avoid unnecessarily calling the api as much as possible
        if (!isEnabled()) return
        val type = currentLeaderboardType ?: return
        leaderboardPos = getLeaderboardPosition(type, overrideCooldown)
        amount = getAmount(type)
        nextPlayer = getNextPlayer(type)
        drawDisplay(type)
    }

    fun drawDisplay(leaderboardType: EliteLeaderboardType) {
        if (!isEnabled()) return

        val lineMap = mutableMapOf<LeaderboardTextEntry, Renderable>()
        val isFirst = leaderboardPos == 1

        lineMap[LeaderboardTextEntry.WEIGHT_POSITION] = amountPosRenderable(leaderboardType)
        lineMap[LeaderboardTextEntry.OVERTAKE] = overtakeRenderable(leaderboardType, isFirst)
        if (!(isFirst && config?.display?.text?.get()?.contains(LeaderboardTextEntry.OVERTAKE) == true) && !isUnranked(leaderboardType)) {
            lineMap[LeaderboardTextEntry.LAST_PLAYER] = overtakeRenderable(leaderboardType, true)
        }

        display = formatDisplay(lineMap)
    }

    open fun formatDisplay(lineMap: MutableMap<LeaderboardTextEntry, Renderable>): List<Renderable> {
        if (FarmingWeightData.apiError || EliteFarmersLeaderboard.apiError) return errorMessage

        val newList = mutableListOf<Renderable>()
        if (inventoryOpen) newList.buildModeSwitcher() else newList.addVerticalSpacer()
        config?.display?.text?.get()?.let { leaderboardTextEntries -> newList.addAll(leaderboardTextEntries.mapNotNull { lineMap[it] }) }
        if (inventoryOpen) newList.buildTypeSwitcher()
        return newList
    }

    abstract fun MutableList<Renderable>.buildTypeSwitcher()

    private fun amountPosRenderable(leaderboardType: EliteLeaderboardType): Renderable {
        val amountText = amount?.roundTo(2)?.addSeparators() ?: if (isUnranked(leaderboardType)) {
            "Not ranked!"
        } else {
            "Loading..."
        }

        val leaderboardPos = getLeaderboardFormat(leaderboardType)
        val mode = EliteLeaderboardConfigApi.getLeaderboardConfig(leaderboardType).gamemode.get().renderableName
        return Renderable.clickable(
            "§6$leaderboardType$mode§7: §e$amountText$leaderboardPos",
            tips = listOf("§eClick to open your Farming Profile."),
            onLeftClick = { openWebsite(PlayerUtils.getName()) },
        )
    }

    private fun overtakeRenderable(leaderboardType: EliteLeaderboardType, getLastPlayer: Boolean = false): Renderable {
        val next: Pair<String, Double>? = if (getLastPlayer) getLastPlayer(leaderboardType) else getNextPlayer(leaderboardType)

        val rankGoal = getRankGoal(leaderboardType)
        val useRankGoal = useEtaGoalRank() && rankGoal != null
        if (useRankGoal && getLastPlayer) return Renderable.empty()

        var (nextName, amountUntil) = next ?: return nullNextPlayerRenderable(leaderboardType)
        if (useRankGoal) {
            nextName += " §7[§b#${rankGoal?.addSeparators()}§7]"
        }

        val behindOrAhead = if (getLastPlayer) "ahead of" else "behind"
        val overtakeETA = if (getLastPlayer) "" else overtakeEta(amountUntil)
        val text = "§e${amountUntil.roundTo(2).addSeparators()}$overtakeETA §7$behindOrAhead §b$nextName"
        return Renderable.clickable(
            text,
            tips = listOf("§eClick to open the Farming Profile of §b$nextName."),
            onLeftClick = { openWebsite(nextName) },
        )
    }

    abstract fun overtakeEta(amountUntil: Double): String

    private fun useEtaGoalRank(): Boolean {
        return config?.rankGoals?.useRankGoal?.get() ?: false
    }

    private fun nullNextPlayerRenderable(leaderboardType: EliteLeaderboardType): Renderable {
        if (EliteFarmersLeaderboard.apiUnavailable) {
            return Renderable.hoverTips(
                content = "§7Waiting for update...",
                tips = listOf(
                    "§celitebot.dev is currently overloaded or unavailable.",
                    "§7Leaderboard data will update when the service recovers.",
                ),
            )
        }
        return if (isUnranked(leaderboardType)) {
            val minAmount = leaderboardMinAmount(leaderboardType) ?: 0.0
            // the amount eligible to enter every other leaderboard is the all-time amount for that lb, except for the monthly weight lb
            // which doesn't include bonus weight because kaeso personally hates me and wants to make this more annoying than it should be
            val isMonthly = currentMode == EliteLeaderboardMode.MONTHLY
            val isWeightMonthly = currentMode == EliteLeaderboardMode.MONTHLY && leaderboardType is EliteLeaderboardType.Weight

            val currentAmount = if (isWeightMonthly) {
                getWeight(EliteLeaderboardMode.MONTHLY, cropWeightOnly = true)
            } else {
                getAmount(leaderboardType, EliteLeaderboardMode.ALL_TIME)
            }

            val weightUntil = minAmount - (currentAmount ?: 0.0)
            val overtakeEta = overtakeEta(weightUntil)
            val untilRankedTextColor = if (overtakeEta == "") "§7" else "§e"
            val untilRankedText = if (isMonthly) "until eligible!" else "until ranked!"

            val text = "§e${weightUntil.roundTo(2).addSeparators()}$overtakeEta $untilRankedTextColor$untilRankedText"

            val tips = buildList {
                add("§bThis leaderboard requires $minAmount ")
                add("§b$leaderboardType before ${if (isMonthly) "being eligible" else "getting ranked"}!")
                if (isWeightMonthly) {
                    add("")
                    add("§7Excludes bonus weight!")
                }
            }

            Renderable.hoverTips(
                content = text,
                tips = tips,
            )
        } else {
            Renderable.text("§bLoading player...")
        }
    }

    private fun showLeaderboard(): Boolean = config?.display?.leaderboard?.get() ?: false

    private fun getLeaderboardFormat(leaderboardType: EliteLeaderboardType): String {
        if (!showLeaderboard()) return ""
        val format = leaderboardPos?.addSeparators() ?: run {
            return if (loadingLeaderboardMutex[leaderboardType::class]?.isLocked == true) " §7[§b#?§7]" else ""
        }
        return " §7[§b#$format§7]"
    }

    fun reset() {
        baseClass?.let { clearCategories(it) }
        amount = null
        leaderboardPos = null
        nextPlayer = null
        apiError = false
    }

    fun isEnabled(): Boolean = (baseClass?.let { EliteLeaderboards.getFromTypeOrNull(it)?.isEnabled } ?: false) && (inGardenEnabled())

    private fun inGardenEnabled() =
        SkyBlockUtils.inSkyBlock && (GardenApi.inGarden() || (config?.display?.showOutsideGarden ?: false))

    abstract fun shouldShowDisplay(): Boolean

    fun MutableList<Renderable>.buildModeSwitcher() {
        this.addRenderableButton(
            label = "Leaderboard Type:",
            current = currentMode,
            onChange = { new ->
                currentMode = new
                update()
            },
            universe = EliteLeaderboardMode.entries,
        )
    }

    fun renderDisplay(position: Position) {
        if (!isEnabled() || !shouldShowDisplay()) return
        val currentlyOpen = InventoryUtils.inAnyInventory()

        if (inventoryOpen != currentlyOpen) {
            inventoryOpen = currentlyOpen
            update()
        }

        if (lastUpdate.passedSince() > 1.seconds) {
            update()
            lastUpdate = SimpleTimeMark.now()
        }

        position.renderRenderables(display, posLabel = name)
    }
}
