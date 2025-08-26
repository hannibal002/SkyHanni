import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getAmount
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getLeaderboardPosition
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getNextPlayer
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.isUnranked
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.loadingLeaderboardMutex
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardMode
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import kotlin.time.Duration.Companion.seconds

abstract class EliteLeaderboardDisplay<E : Enum<E>, T : EliteLeaderboardType.WithEnum<E>>(
    private var storage: Pair<E?, EliteLeaderboardMode>?,
    private val createType: (E, EliteLeaderboardMode) -> EliteLeaderboardType
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

    val leaderboardType: EliteLeaderboardType?
        get() = storage?.let { (storageEnum, mode) ->
            storageEnum?.let { createType(it, mode) }
        }

    fun update(overrideCooldown: Boolean = false) {
        val type = leaderboardType ?: return
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

    private fun getLeaderboardFormat(): String {
        if (!config.leaderboard.get()) return ""
        val format = leaderboardPos?.addSeparators() ?: return if (loadingLeaderboardMutex.isLocked) " §7[§b#?§7]" else ""
        return " §7[§b#$format§7]"
    }

    abstract fun resetData()

    abstract fun isEnabled(): Boolean

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
