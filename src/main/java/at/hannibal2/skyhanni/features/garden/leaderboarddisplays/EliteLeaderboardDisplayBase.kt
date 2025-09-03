package at.hannibal2.skyhanni.features.garden.leaderboarddisplays

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.garden.elitefarmers.LeaderboardData.getAmount
import at.hannibal2.skyhanni.data.garden.elitefarmers.LeaderboardData.getLastPlayer
import at.hannibal2.skyhanni.data.garden.elitefarmers.LeaderboardData.getLeaderboardPosition
import at.hannibal2.skyhanni.data.garden.elitefarmers.LeaderboardData.getNextPlayer
import at.hannibal2.skyhanni.data.garden.elitefarmers.LeaderboardData.getRankGoal
import at.hannibal2.skyhanni.data.garden.elitefarmers.LeaderboardData.isUnranked
import at.hannibal2.skyhanni.data.garden.elitefarmers.LeaderboardData.leaderboardMinAmount
import at.hannibal2.skyhanni.data.garden.elitefarmers.LeaderboardData.loadingLeaderboardMutex
import at.hannibal2.skyhanni.data.garden.elitefarmers.FarmingWeightData.getWeight
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import at.hannibal2.skyhanni.utils.renderables.primitives.empty
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration.Companion.seconds

abstract class EliteLeaderboardDisplayBase<E : Enum<E>, T : EliteLeaderboardType.WithEnum<E>>(
    private val createType: (E, EliteLeaderboardMode) -> EliteLeaderboardType,
    private val name: String
) {
    protected val configBase get() = GardenApi.config.eliteFarmersLeaderboards

    protected var display = emptyList<Renderable>()
    protected var apiError = false
    var inventoryOpen = false
    protected var amount: Double? = null
    protected var leaderboardPos: Int? = null
    private var nextPlayer: Pair<String, Double>? = null

    protected abstract var currentMode: EliteLeaderboardMode
    protected abstract var currentEnum: E?

    abstract fun getDefaultEnum(): E?


    protected val errorMessage by lazy {
        listOf(
            "§cFarming Weight error: Cannot load",
            "§cdata from Elite Farmers!",
            "§eRejoin the garden or",
            "§eclick here to fix it.",
        ).map {
            Renderable.clickable(
                it,
                tips = listOf("§eClick here to reload the data right now!"),
                onLeftClick = ::resetData,
            )
        }
    }

    open val currentLeaderboardType: EliteLeaderboardType?
        get() = (currentEnum ?: getDefaultEnum())?.let { createType(it, currentMode) }

    fun update(overrideCooldown: Boolean = false) {
        val type = currentLeaderboardType ?: return
        leaderboardPos = getLeaderboardPosition(type, overrideCooldown)
        amount = getAmount(type)
        nextPlayer = getNextPlayer(type)
        drawDisplay(type)
    }

    abstract fun drawDisplay(leaderboardType: EliteLeaderboardType)

    protected fun weightPosRenderable(leaderboardType: EliteLeaderboardType): Renderable {
        val amountText = amount?.roundTo(2)?.addSeparators() ?: if (isUnranked(leaderboardType)) {
            "Not ranked!"
        } else {
            "Loading..."
        }

        val leaderboardPos = getLeaderboardFormat(leaderboardType)
        return Renderable.clickable(
            "§6$leaderboardType§7: §e$amountText$leaderboardPos",
            tips = listOf("§eClick to open your Farming Profile."),
            onLeftClick = { openWebsite(PlayerUtils.getName()) },
        )
    }

    fun overtakeRenderable(leaderboardType: EliteLeaderboardType, getLastPlayer: Boolean = false): Renderable {
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

    abstract fun useEtaGoalRank(): Boolean

    private fun nullNextPlayerRenderable(leaderboardType: EliteLeaderboardType): Renderable {
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

    abstract fun showLeaderboard(): Boolean

    private fun getLeaderboardFormat(leaderboardType: EliteLeaderboardType): String {
        if (!showLeaderboard()) return ""
        val format = leaderboardPos?.addSeparators()
            ?: return if (loadingLeaderboardMutex[leaderboardType::class]?.isLocked == true) " §7[§b#?§7]" else ""
        return " §7[§b#$format§7]"
    }

    private fun resetData() {
        leaderboardPos = null
        amount = null
        nextPlayer = null
    }

    fun reset() {
        amount = null
        leaderboardPos = null
        nextPlayer = null
        apiError = false
    }

    abstract fun isEnabled(): Boolean

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

        position.renderRenderables(display, posLabel = name)
    }

    private var lastName = ""
    private var lastOpenWebsite = SimpleTimeMark.farPast()

    private fun openWebsite(name: String, ignoreCooldown: Boolean = false) {
        if (!ignoreCooldown && lastOpenWebsite.passedSince() < 5.seconds && name == lastName) return
        lastOpenWebsite = SimpleTimeMark.now()
        lastName = name

        OSUtils.openBrowser("https://elitebot.dev/@$name/")
        ChatUtils.chat("Opening Farming Profile of player §b$name")
    }
}
