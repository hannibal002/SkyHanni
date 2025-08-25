package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.garden.EliteFarmingWeightConfig.FarmingWeightTextEntry
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getLeaderboardPosition
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getMinWeight
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getNextPlayer
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.getRankGoal
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.isUnranked
import at.hannibal2.skyhanni.data.garden.EliteFarmersLeaderboard.loadingLeaderboardMutex
import at.hannibal2.skyhanni.data.garden.FarmingWeight
import at.hannibal2.skyhanni.data.garden.FarmingWeight.getFactor
import at.hannibal2.skyhanni.data.garden.FarmingWeight.getWeight
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLeaderboardType
import at.hannibal2.skyhanni.events.CollectionUpdateEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getLatestBlocksPerSecond
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addVerticalSpacer
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FarmingWeightDisplay {
    private val config get() = GardenApi.config.eliteFarmingWeights
    private val storage get() = GardenApi.storage?.farmingWeight
    private val lbName get() = currentLeaderboardType.specificDisplayName

    private var display = emptyList<Renderable>()
    private var apiError = false
    private var inventoryOpen = false
    private var weight: Double? = null
    private var leaderboardPos: Int? = null
    private var nextPlayer: Pair<String, Double>? = null
    private var lastFarmedCrop: CropType? = null

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
        update()
    }


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

    fun update(overrideCooldown: Boolean = false) {
        weight = getWeight(currentLeaderboardType, overrideCooldown)
        leaderboardPos = getLeaderboardPosition(currentLeaderboardType, overrideCooldown)
        nextPlayer = getNextPlayer(currentLeaderboardType)
        drawDisplay()
    }

    private fun drawDisplay() {
        if (!isEnabled()) return

        val lineMap = mutableMapOf<FarmingWeightTextEntry, Renderable>()

        lineMap[FarmingWeightTextEntry.WEIGHT_POSITION] = weightPosRenderable()
        lineMap[FarmingWeightTextEntry.OVERTAKE] = overtakeRenderable()

        display = formatDisplay(lineMap)

    }

    private fun weightPosRenderable(): Renderable {
        val weightText = weight?.roundTo(2)?.addSeparators() ?: if (isUnranked(currentLeaderboardType)) {
            "Not ranked!"
        } else {
            "Loading..."
        }

        val leaderboardPos = getLeaderboardFormat()
        return Renderable.clickable(
            "§6$lbName§7: §e$weightText$leaderboardPos",
            tips = listOf("§eClick to open your Farming Profile."),
            onLeftClick = { openWebsite(PlayerUtils.getName()) },
        )
    }

    private fun getLeaderboardFormat(): String {
        if (!config.leaderboard.get()) return ""
        val format = leaderboardPos?.addSeparators() ?: return if (loadingLeaderboardMutex.isLocked) " §7[§b#?§7]" else ""
        return " §7[§b#$format§7]"
    }

    private fun overtakeRenderable(): Renderable {
        // I was planning on showing the player behind you if you're first, but elite api does not support that without some shenanigans
        // This should be changed in the future so leaving it in for now
        if (leaderboardPos == 1) return Renderable.text("§bNo players ahead!")

        var (nextName, weightUntil) = nextPlayer ?: return nullNextPlayerRenderable()

        val rankGoal = getRankGoal(currentLeaderboardType)
        if (config.useEtaGoalRank.get() && rankGoal != null) {
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

    private fun nullNextPlayerRenderable(): Renderable {
        return if ((weight ?: 0.0) < (getMinWeight(currentLeaderboardType) ?: 0.0)) {
            val minWeight = getMinWeight(currentLeaderboardType) ?: 1000.00
            // Min weight to get on lb is 1k all-time weight for all-time lb (including bonus weight), and 1k all-time crop weight
            // for monthly lb because kaeso personally hates me and wants to make this more annoying than it should be
            val isMonthly = currentLeaderboardType == EliteLeaderboardType.MONTHLY
            val currentWeight = getWeight(
                EliteLeaderboardType.ALL_TIME,
                cropWeightOnly = isMonthly
            )
            val weightUntil = minWeight - (currentWeight ?: 0.0)
            val overtakeEta = overtakeEta(weightUntil)
            val minWeightText = "${if (isMonthly) "Crop" else "Farming"} Weight"
            val untilRankedTextColor = if (overtakeEta == "") "§7" else "§e"
            val untilRankedText = if (isMonthly) "until eligible!" else "until ranked!"
            val text = "§e${weightUntil.roundTo(2).addSeparators()}$overtakeEta $untilRankedTextColor$untilRankedText"
            val tips = mutableListOf(
                "§bThis leaderboard requires $minWeight ",
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


    // TODO support crop/dicer drops
    private fun overtakeEta(weightUntil: Double): String {
        if (!config.overtakeETA.get() || !config.overtakeETAAlways.get() && !GardenApi.isCurrentlyFarming()) return ""
        lastFarmedCrop = GardenApi.getCurrentlyFarmedCrop() ?: if (config.overtakeETAAlways.get()) lastFarmedCrop else null
        val crop = lastFarmedCrop ?: return ""
        val cropsPerSecond = crop.getSpeed() ?: return ""
        val mooshroomCowCropsPerSecond = if (GardenApi.mushroomCowPet) {
            (CurrentPetApi.currentPet?.level ?: 0) / 100 * (crop.getLatestBlocksPerSecond() ?: 0.0)
        } else {
            0.0
        }
        val weightPerSecond = cropsPerSecond / crop.getFactor() + mooshroomCowCropsPerSecond / CropType.MUSHROOM.getFactor()
        val timeUntil = (weightUntil / weightPerSecond).seconds
        return " §7(§b${timeUntil.format()}§7)"
    }

    private fun formatDisplay(lineMap: MutableMap<FarmingWeightTextEntry, Renderable>): List<Renderable> {
        if (FarmingWeight.apiError || EliteFarmersLeaderboard.apiError) {
            return errorMessage
        }

        val newList = mutableListOf<Renderable>()
        if (inventoryOpen) newList.buildLeaderboardSwitcher() else newList.addVerticalSpacer()
        newList.addAll(config.text.get().mapNotNull { lineMap[it] })
        return newList
    }

    private fun getLeaderboardType() = currentLeaderboardType

    private fun MutableList<Renderable>.buildLeaderboardSwitcher() {
        this.addRenderableButton(
            label = "Leaderboard Type:",
            current = getLeaderboardType(),
            onChange = { new ->
                currentLeaderboardType = new
                update()
            },
            universe = EliteLeaderboardType.entries,
        )
    }

    fun resetData() {
        apiError = false
        nextPlayer = null
        weight = null
        leaderboardPos = null
        EliteFarmersLeaderboard.reset()
        FarmingWeight.reset()
        update(true)
    }

    fun isEnabled() = config.display && (inGardenEnabled())
    private fun inGardenEnabled() = SkyBlockUtils.inSkyBlock && (GardenApi.inGarden() || config.showOutsideGarden)

    private fun shouldShowDisplay(): Boolean =
        !GardenApi.hideExtraGuis() && (apiError || (config.ignoreLow || (getWeight(EliteLeaderboardType.ALL_TIME) ?: 0.0) >= 200.0))

    private var lastName = ""
    private var lastOpenWebsite = SimpleTimeMark.farPast()

    private fun openWebsite(name: String, ignoreCooldown: Boolean = false) {
        if (!ignoreCooldown && lastOpenWebsite.passedSince() < 5.seconds && name == lastName) return
        lastOpenWebsite = SimpleTimeMark.now()
        lastName = name

        OSUtils.openBrowser("https://elitebot.dev/@$name/")
        ChatUtils.chat("Opening Farming Profile of player §b$name")
    }

    private fun lookUpCommand(it: Array<String>) {
        val name = if (it.size == 1) it[0] else PlayerUtils.getName()
        openWebsite(name, ignoreCooldown = true)
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shfarmingprofile") {
            description = "Look up the farming profile from yourself or another player on elitebot.dev"
            category = CommandCategory.USERS_ACTIVE
            callback { lookUpCommand(it) }
        }
        event.register("shresetfarmingweight") {
            description = "Reset farming weight display."
            category = CommandCategory.USERS_RESET
            callback { resetData() }
        }
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        onToggle(
            config.overtakeETA,
            config.overtakeETAAlways,
            config.text
        ) {
            update()
        }
    }

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

        val displayList: List<FarmingWeightTextEntry> = buildList {
            add(FarmingWeightTextEntry.WEIGHT_POSITION)
            event.transform(103, "garden.eliteFarmingWeights.overtakeETA") { entry ->
                if (entry.asBoolean) add(FarmingWeightTextEntry.OVERTAKE)
                entry
            }
        }

        event.add(103, "garden.eliteFarmingWeights.text") {
            ConfigManager.gson.toJsonTree(displayList)
        }

        val base = "#garden.farmingWeight"
        event.move(101, "$base.lastFarmingWeightLeaderboard", "$base.lastLeaderboard")
    }
}
