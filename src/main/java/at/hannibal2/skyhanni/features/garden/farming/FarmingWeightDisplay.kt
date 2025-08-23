package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.garden.EliteFarmingWeightConfig.FarmingWeightTextEntry
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getLeaderboardPosition
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getNextPlayer
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getRankGoal
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.loadingLeaderboardMutex
import at.hannibal2.skyhanni.data.garden.FarmingWeight
import at.hannibal2.skyhanni.data.garden.FarmingWeight.getWeight
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.events.CollectionUpdateEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.garden.GardenToolChangeEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addVerticalSpacer
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration.Companion.seconds

// TODO overtake ETA
@SkyHanniModule
object FarmingWeightDisplay {

    private fun shouldShowDisplay(): Boolean =
        !GardenApi.hideExtraGuis() && (apiError || (config.ignoreLow || weight >= 200))

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled() || !shouldShowDisplay()) return
        val currentlyOpen = InventoryUtils.inAnyInventory()

        if (inventoryOpen != currentlyOpen) {
            inventoryOpen = currentlyOpen
            update()
        }

        config.pos.renderRenderables(display, posLabel = "Farming Weight Display")
    }

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        display = emptyList()
    }

    @HandleEvent
    fun onCollectionUpdate(event: CollectionUpdateEvent) {
        if (!isEnabled()) return
        update()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        //check if eta is enabled
        update()
    }

    private val config get() = GardenApi.config.eliteFarmingWeights
    private val storage get() = GardenApi.storage?.farmingWeight
    private val lbName get() = currentLeaderboardType.specificDisplayName
    private val localCounter = mutableMapOf<CropType, Long>()
    private var display = emptyList<Renderable>()
    private var apiError = false
    private var weight = -1.0
    private var rankGoal = -1
    private var lastUpdate: SimpleTimeMark = SimpleTimeMark.farPast()
    private var inventoryOpen = false


    private var currentLeaderboardType: EliteLeaderboardType
        get() = storage?.lastLeaderboardType ?: EliteLeaderboardType.ALL_TIME
        set(value) {
            value.let {
                GardenApi.storage?.farmingWeight?.lastLeaderboardType = it
            }
        }

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

    private var lastOpenWebsite = SimpleTimeMark.farPast()


    // TODO fetch and cache weight, lb pos, next player, eta before drawing display
    fun update() {
        drawDisplay()
    }

    private fun drawDisplay() {
        if (!isEnabled()) return

        val lineMap = mutableMapOf<FarmingWeightTextEntry, Renderable>()
        val weight = getWeight(currentLeaderboardType)?.roundTo(2)?.addSeparators() ?: "Loading..."
        val leaderboardPos = getLeaderboardFormat()
        lineMap[FarmingWeightTextEntry.WEIGHT_POSITION] =
            Renderable.clickable(
                "§6$lbName§7: §e$weight$leaderboardPos",
                tips = listOf("§eClick to open your Farming Profile."),
                onLeftClick = { openWebsite(PlayerUtils.getName()) },
            )

        lineMap[FarmingWeightTextEntry.OVERTAKE] = overtakeRenderable()

        display = formatDisplay(lineMap)

    }

    private fun overtakeRenderable(): Renderable {
        val leaderboardPos = getLeaderboardPosition(currentLeaderboardType)
        if (leaderboardPos == 1) return Renderable.text("§bNo players ahead!")
        var (nextName, weightUntil) = getNextPlayer(currentLeaderboardType) ?: return Renderable.text("§bLoading next player...")

        val rankGoal = getRankGoal()
        if (config.useEtaGoalRank.get() && rankGoal != null && rankGoal < (leaderboardPos ?: Int.MAX_VALUE)) {
            nextName += " §7[§b#${rankGoal.addSeparators()}§7]"
        }

        val text = "§e${weightUntil.roundTo(2).addSeparators()} §bbehind $nextName"
        return Renderable.clickable(
            text,
            tips = listOf("§eClick to open the Farming Profile of §b$nextName."),
            onLeftClick = { openWebsite(nextName) },
        )
    }

    private fun formatDisplay(lineMap: MutableMap<FarmingWeightTextEntry, Renderable>): List<Renderable> {
        if (FarmingWeight.apiError) {
            return errorMessage
        }
        val newList = mutableListOf<Renderable>()
        if (inventoryOpen) newList.buildLeaderboardSwitcher() else newList.addVerticalSpacer()

        newList.addAll(config.text.mapNotNull { lineMap[it] })

        return newList
    }

    private fun getLeaderboardType() = currentLeaderboardType

    private fun MutableList<Renderable>.buildLeaderboardSwitcher() {
        this.addRenderableButton(
            label = "Leaderboard Type",
            current = getLeaderboardType(),
            onChange = { new ->
                currentLeaderboardType = new
                update()
            },
            universe = EliteLeaderboardType.entries,
        )
    }

    private fun getLeaderboardFormat(): String {
        val leaderboardPosition = getLeaderboardPosition(currentLeaderboardType)
        if (!config.leaderboard) return ""
        return if (leaderboardPosition != null) {
            val format = leaderboardPosition.addSeparators()
            " §7[§b#$format§7]"
        } else {
            if (loadingLeaderboardMutex.isLocked) " §7[§b#?§7]" else ""
        }
    }

    private fun resetData() {
        apiError = false
        // We ask both api endpoints after every world switch
    }

    fun isEnabled() = config.display && (inGardenEnabled())
    private fun inGardenEnabled() = SkyBlockUtils.inSkyBlock && (GardenApi.inGarden() || config.showOutsideGarden)

    private fun isEtaEnabled() = config.overtakeETA

    private fun lookUpCommand(it: Array<String>) {
        val name = if (it.size == 1) it[0] else PlayerUtils.getName()
        openWebsite(name, ignoreCooldown = true)
    }

    private var lastName = ""

    private fun openWebsite(name: String, ignoreCooldown: Boolean = false) {
        if (!ignoreCooldown && lastOpenWebsite.passedSince() < 5.seconds && name == lastName) return
        lastOpenWebsite = SimpleTimeMark.now()
        lastName = name

        OSUtils.openBrowser("https://elitebot.dev/@$name/")
        ChatUtils.chat("Opening Farming Profile of player §b$name")
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shfarmingprofile") {
            description = "Look up the farming profile from yourself or another player on elitebot.dev"
            category = CommandCategory.USERS_ACTIVE
            callback { lookUpCommand(it) }
        }
    }
// TODO configfix for overtake eta
    
    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(1, "garden.eliteFarmingWeightoffScreenDropMessage")
        event.move(3, "garden.eliteFarmingWeightDisplay", "garden.eliteFarmingWeights.display")
        event.move(3, "garden.eliteFarmingWeightPos", "garden.eliteFarmingWeights.pos")
        event.move(3, "garden.eliteFarmingWeightLeaderboard", "garden.eliteFarmingWeights.leaderboard")
        event.move(3, "garden.eliteFarmingWeightOvertakeETA", "garden.eliteFarmingWeights.overtakeETA")
        event.move(3, "garden.eliteFarmingWeightOffScreenDropMessage", "garden.eliteFarmingWeights.offScreenDropMessage")
        event.move(3, "garden.eliteFarmingWeightOvertakeETAAlways", "garden.eliteFarmingWeights.overtakeETAAlways")
        event.move(3, "garden.eliteFarmingWeightETAGoalRank", "garden.eliteFarmingWeights.ETAGoalRank")
        event.move(3, "garden.eliteFarmingWeightIgnoreLow", "garden.eliteFarmingWeights.ignoreLow")
        event.move(14, "garden.eliteFarmingWeight.offScreenDropMessage", "garden.eliteFarmingWeights.showLbChange")
        event.move(34, "garden.eliteFarmingWeights.ETAGoalRank", "garden.eliteFarmingWeights.etaGoalRank")

        val base = "#garden.farmingWeight"
        event.move(101, "$base.lastFarmingWeightLeaderboard", "$base.lastLeaderboard")
    }
}
