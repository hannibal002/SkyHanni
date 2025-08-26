import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getAmount
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getLeaderboardPosition
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getNextPlayer
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
    private val config get() = GardenApi.config.eliteFarmingWeights

    private var display = emptyList<Renderable>()
    private var apiError = false
    private var inventoryOpen = false
    private var amount: Double? = null
    private var leaderboardPos: Int? = null
    private var nextPlayer: Pair<String, Double>? = null
    protected open var currentMode: EliteLeaderboardMode
        get() = storage?.second ?: EliteLeaderboardMode.ALL_TIME
        set(value) {
            val enumValue = storage?.first
            storage = Pair(enumValue, value)
        }

    protected open var currentEnum: E?
        get() = storage?.first ?: getDefaultEnum()
        set(value) {
            val mode = storage?.second ?: EliteLeaderboardMode.ALL_TIME
            storage = Pair(value, mode)
        }

    abstract fun getDefaultEnum(): E


    private val errorMessage by lazy {
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
        get() = storage?.let { (storageEnum, mode) ->
            storageEnum?.let { createType(it, mode) }
        }

    fun update(overrideCooldown: Boolean = false) {
        val type = currentLeaderboardType ?: return
        amount = getAmount(type)
        leaderboardPos = getLeaderboardPosition(type, overrideCooldown)
        nextPlayer = getNextPlayer(type)
        drawDisplay(type)
    }

    abstract fun drawDisplay(leaderboardType: EliteLeaderboardType)

    abstract fun formatDisplay(): Renderable

    private fun weightPosRenderable(leaderboardType: EliteLeaderboardType): Renderable {
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

    private fun getLeaderboardFormat(): String {
        if (!config.leaderboard.get()) return ""
        val format = leaderboardPos?.addSeparators() ?: return if (loadingLeaderboardMutex.isLocked) " §7[§b#?§7]" else ""
        return " §7[§b#$format§7]"
    }

    abstract fun resetData()

    abstract fun isEnabled(): Boolean

    abstract fun shouldShowDisplay(): Boolean

    abstract fun MutableList<Renderable>.buildLeaderboardSwitcher()

    private fun MutableList<Renderable>.buildModeSwitcher() {
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
