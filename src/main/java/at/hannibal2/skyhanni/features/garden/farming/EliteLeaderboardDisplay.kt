import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getAmount
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getLastPlayer
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getLeaderboardPosition
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getNextPlayer
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getRankGoal
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.isUnranked
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.leaderboardMinAmount
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.loadingLeaderboardMutex
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
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration.Companion.seconds

abstract class EliteLeaderboardDisplay<E : Enum<E>, T : EliteLeaderboardType.WithEnum<E>>(
    private var storage: Pair<E?, EliteLeaderboardMode>?,
    private val createType: (E, EliteLeaderboardMode) -> EliteLeaderboardType,
    private val name: String
) {
    protected val configBase get() = GardenApi.config.eliteFarmersLeaderboards

    protected var display = emptyList<Renderable>()
    protected var apiError = false
    var inventoryOpen = false
    protected var amount: Double? = null
    protected var leaderboardPos: Int? = null
    protected var nextPlayer: Pair<String, Double>? = null
    protected open var currentMode: EliteLeaderboardMode
        get() = storage?.second ?: EliteLeaderboardMode.ALL_TIME
        set(value) {
            val enumValue = storage?.first
            storage = Pair(enumValue, value)
        }

    protected open var currentEnum: E?
        get() = storage?.first
        set(value) {
            val mode = storage?.second ?: EliteLeaderboardMode.ALL_TIME
            storage = Pair(value, mode)
        }

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

    val currentLeaderboardType: EliteLeaderboardType?
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

        val leaderboardPos = getLeaderboardFormat()
        return Renderable.clickable(
            "§6${leaderboardType}§7: §e$amountText$leaderboardPos",
            tips = listOf("§eClick to open your Farming Profile."),
            onLeftClick = { openWebsite(PlayerUtils.getName()) },
        )
    }

    fun overtakeRenderable(leaderboardType: EliteLeaderboardType): Renderable {

        val next: Pair<String, Double>? = if (leaderboardPos == 1) getLastPlayer(leaderboardType) else getNextPlayer(leaderboardType)

        var (nextName, weightUntil) = next ?: return nullNextPlayerRenderable(leaderboardType)

        val rankGoal = getRankGoal(leaderboardType)
        if (useEtaGoalRank() && rankGoal != null) {
            nextName += " §7[§b#${rankGoal.addSeparators()}§7]"
        }

        val behindOrAhead = if (leaderboardPos == 1) "ahead of" else "behind"
        val overtakeETA = if (leaderboardPos == 1) "" else overtakeEta(weightUntil)
        val text = "§e${weightUntil.roundTo(2).addSeparators()}$overtakeETA §7$behindOrAhead §b$nextName"
        return Renderable.clickable(
            text,
            tips = listOf("§eClick to open the Farming Profile of §b$nextName."),
            onLeftClick = { openWebsite(nextName) },
        )
    }

    abstract fun overtakeEta(weightUntil: Double): String

    abstract fun useEtaGoalRank(): Boolean

    // TODO abstract this out
    private fun nullNextPlayerRenderable(leaderboardType: EliteLeaderboardType): Renderable {
        return if ((amount ?: 0.0) < (leaderboardMinAmount(leaderboardType) ?: 0.0)) {
            val minAmount = leaderboardMinAmount(leaderboardType) ?: 0.0
            // Min weight to get on lb is 1k all-time weight for all-time lb (including bonus weight), and 1k all-time crop weight
            // for monthly lb because kaeso personally hates me and wants to make this more annoying than it should be
            val isMonthly = currentMode == EliteLeaderboardMode.MONTHLY
            val currentAmount = getAmount(leaderboardType)
            val weightUntil = minAmount - (currentAmount ?: 0.0)
            val overtakeEta = ""//overtakeEta(weightUntil)
            val minWeightText = "$leaderboardType"
            val untilRankedTextColor = if (overtakeEta == "") "§7" else "§e"
            val untilRankedText = if (isMonthly) "until eligible!" else "until ranked!"
            val text = "§e${weightUntil.roundTo(2).addSeparators()}$overtakeEta $untilRankedTextColor$untilRankedText"
            val tips = mutableListOf(
                "§bThis leaderboard requires $minAmount ",
                "§b$minWeightText before getting ranked!",
            )
            if (isMonthly) {
                tips.addAll(
                    listOf(
                        "",
                        "§7Excludes bonus weight!"
                    )
                )
            }
            Renderable.hoverTips(
                content = text,
                tips = tips,
            )
        } else {
            Renderable.text("§bLoading next player...")
        }
    }

    abstract fun showLeaderboard(): Boolean

    private fun getLeaderboardFormat(): String {
        if (!showLeaderboard()) return ""
        val format = leaderboardPos?.addSeparators() ?: return if (loadingLeaderboardMutex.isLocked) " §7[§b#?§7]" else ""
        return " §7[§b#$format§7]"
    }

    private fun resetData() {
        leaderboardPos = null
        amount = null
        nextPlayer = null
    }

    abstract fun reset()

    abstract fun isEnabled(): Boolean

    abstract fun shouldShowDisplay(): Boolean

    abstract fun MutableList<Renderable>.buildTypeSwitcher()

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
