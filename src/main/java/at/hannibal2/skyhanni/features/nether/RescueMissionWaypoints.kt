package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.RescueParkourJson
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.nether.reputationhelper.CrimsonIsleReputationHelper
import at.hannibal2.skyhanni.features.nether.reputationhelper.FactionType
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.DailyQuestHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.ParkourHelper
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColor
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object RescueMissionWaypoints {

    // Logic for the parkour location waypoint system is based on the Deep Caverns Guide by hannibal2

    private val config get() = SkyHanniMod.feature.crimsonIsle.reputationHelper.rescueMission
    private val patternGroup = RepoPattern.group("crimson.rescue")

    /**
     * REGEX-TEST: [NPC] Undercover Agent: Here's what you need to do.
     */
    private val agentDialoguePattern by patternGroup.pattern(
        "start",
        "\\[NPC] Undercover Agent: Here's what you need to do.",
    )

    /**
     * REGEX-TEST: §e[NPC] §eRescue Recruiter§f: §rYou want to help us rescue a hostage from those filthy brutes over there?
     */
    private val recruiterPattern by RepoPattern.pattern(
        "recruiter",
        "§e\\[NPC] §eRescue Recruiter§f: §rYou want to help us rescue a hostage from those filthy brutes over there\\?",
    )

    /**
     * REGEX-TEST: §cThe guards captured you and threw you out.
     */
    private val caughtPattern by RepoPattern.pattern(
        "caught",
        "§cThe guards captured you and threw you out.",
    )

    /**
     * REGEX-TEST: §cYou took too long and the agents found someone else.
     */
    private val cancelTimeoutPattern by RepoPattern.pattern(
        "cancel.timeout",
        "§cYou took too long and the agents found someone else\\.",
    )

    /**
     * REGEX-TEST: §e[NPC] §eUndercover Agent§f: §rIt's too dangerous for you?
     */
    private val cancelAfraidPattern by RepoPattern.pattern(
        "cancel.afraid",
        "§e\\[NPC] §eUndercover Agent§f: §rIt's too dangerous for you\\?",
    )

    /**
     * REGEX-TEST: §cYou left the area and failed your rescue mission.
     */
    private val cancelRunAwayPattern by RepoPattern.pattern(
        "cancel.run-away",
        "§cYou left the area and failed your rescue mission\\.",
    )

    /**
     * REGEX-TEST: §aⒸ §eRescue Mission
     * REGEX-TEST: §5Ⓐ §cRescue Mission
     */
    private val questTierPattern by RepoPattern.pattern(
        "tier",
        "§.(?<tier>.) §.Rescue Mission",
    )

    /**
     * REGEX-TEST: Rescue
     */
    private val menuPattern by RepoPattern.pattern(
        "menu",
        "Rescue",
    )

    private var parkourHelper: ParkourHelper? = null
    private var tier: String? = null
    private var tierWasUnknown = false
    private var data: RescueParkourJson? = null

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        tier = null
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        stopParkour()
    }

    private fun stopParkour() {
        if (parkourHelper != null) {
            IslandGraphs.enableAllNodes()
        }
        parkourHelper = null
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!menuPattern.matches(event.inventoryName)) return
        val name = event.inventoryItems[22]?.displayName ?: return

        tier = questTierPattern.matchMatcher(name) {
            group("tier").toLetter()
        }
        if (tier != null) {
            if (tierWasUnknown) {
                tierWasUnknown = false
                ChatUtils.chat("Now the tier is known!")
                navigateToUndercoverAgent()
            }
        }
    }

    private fun initParkour() {
        val tier = tier ?: return

        if (tier == "S") {
            if (CrimsonIsleReputationHelper.factionType == FactionType.MAGE) {
                ErrorManager.logErrorStateWithData(
                    "No data present for Mage S-rank Rescue Mission",
                    "No Mage S-Rank in repo",
                )
                return
            }

            // will get data["S1"] or data["S2"]
            parkourHelper = data?.let { data ->
                data.barb["S${config.variant.get()}"]?.let {
                    createParkour(it)
                }
            }
            return
        }

        parkourHelper = data?.let { data ->
            val source = when (CrimsonIsleReputationHelper.factionType) {
                FactionType.MAGE -> data.mage
                FactionType.BARBARIAN -> data.barb
                null -> null
            }
            source?.get(tier)?.let {
                createParkour(it)
            }
        }
    }

    private fun createParkour(locations: List<LorenzVec>) = ParkourHelper(
        locations,
        emptyList(),
        platformSize = 1.0,
        detectionRange = 3.5,
        onEndReach = {
            stopParkour()
        },
    )

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        data = event.getConstant<RescueParkourJson>("RescueMissionWaypoints")
        parkourHelper?.let {
            startParkour()
        }
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (config.hostagePath) {
            agentDialoguePattern.matchMatcher(event.message.removeColor()) {
                tier?.let {
                    startParkour()
                } ?: run {
                    ChatUtils.userError("Rescue mission tier still not known! Check out the Quest Board first as I told you earlier!")
                    navigateToQuestBoard("forgot to check out tier")
                }
            }
            if (recruiterPattern.matches(event.message)) {
                if (tier == null) {
                    DelayedRun.runNextTick {
                        ChatUtils.userError("Rescue mission tier not known! Check out the Quest Board first!")
                    }
                    tierWasUnknown = true
                    return
                }
                navigateToUndercoverAgent()
            }
            if (caughtPattern.matches(event.message)) {
                parkourHelper?.reset()
                navigateToParkourStart()
            }
        }
        parkourHelper?.let {
            if (cancelAfraidPattern.matches(event.message) || cancelRunAwayPattern.matches(event.message)) {
                stopParkour()
                tryRestart()
            }
            if (cancelTimeoutPattern.matches(event.message)) {
                stopParkour()
                if (config.hostagePath) {
                    navigateToQuestBoard("run out of time")
                }
            }
        }
    }

    private fun tryRestart() {
        if (config.hostagePath) {
            ChatUtils.clickableChat(
                "Wanna retry the rescue mission quest? Click here!",
                onClick = {
                    navigateToQuestBoard("try again")
                },
            )
        }
    }

    private fun startParkour() {
        parkourHelper?.reset()
        initParkour()
        updateConfig()
        tweakGraphNetwork()
        navigateToParkourStart()
    }

    // navigation would lead the player from agent to parkour start
    // through an area where the rescue mission would fail
    // or where guards are located.
    // therefore we remove some nodes from the network to only show the correct path
    private fun tweakGraphNetwork() {
        val reason = "rescue mission"
        // outside cathedral
        IslandGraphs.disableNodes(reason, LorenzVec(6.9, 105.0, -852.0), 3.0)

        // in cathedral
        IslandGraphs.disableNodes(reason, LorenzVec(-9.2, 114.1, -882.1), 3.0)
        IslandGraphs.disableNodes(reason, LorenzVec(-30.9, 114.0, -884.1), 3.0)
        IslandGraphs.disableNodes(reason, LorenzVec(-37.4, 114.1, -888.5), 3.0)
    }

    private fun navigateToParkourStart() {
        val location = parkourHelper?.getStartLocation() ?: return
        IslandGraphs.pathFind(
            location,
            "§estart of parkour",
            LorenzColor.YELLOW.toColor(),
            condition = { config.hostagePath && parkourHelper != null },
        )
    }

    private fun navigateToUndercoverAgent() {
        if (!config.agentPath) return
        val factionType = CrimsonIsleReputationHelper.factionType ?: return
        val undercoverAgentLocation = when (factionType) {
            FactionType.MAGE -> LorenzVec(-626.7, 119.0, -960.0)
            FactionType.BARBARIAN -> LorenzVec(-15.5, 93.0, -843.7)
        }
        IslandGraphs.pathFind(
            undercoverAgentLocation,
            "§5${factionType.factionName} Undercover Agent",
            LorenzColor.DARK_PURPLE.toColor(),
            condition = { config.agentPath },
        )
    }

    private fun navigateToQuestBoard(reason: String) {
        val location = DailyQuestHelper.getQuestBoardLocation()
        IslandGraphs.pathFind(
            location,
            "Head back to Quest board, $reason",
            LorenzColor.WHITE.toColor(),
            condition = { (config.agentPath || config.hostagePath) },
        )
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.hostagePath) return

        parkourHelper?.render(event)
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        with(config) {
            ConditionalUtils.onToggle(variant) {
                parkourHelper?.let {
                    startParkour()
                }
            }
            ConditionalUtils.onToggle(chroma, solidColor, lookAhead) {
                updateConfig()
            }
        }
    }

    private fun String.toLetter(): String = when (this) {
        "Ⓢ" -> "S"
        "Ⓐ" -> "A"
        "Ⓑ" -> "B"
        "Ⓒ" -> "C"
        "Ⓓ" -> "D"
        else -> error("unknown letter '$this'")
    }

    private fun updateConfig() {
        parkourHelper?.run {
            rainbowColor = config.chroma.get()
            monochromeColor = config.solidColor.get().toSpecialColor()
            lookAhead = config.lookAhead.get() + 1
        }
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Rescue Mission Waypoints")

        parkourHelper ?: run {
            event.addIrrelevant("no parkour loaded")
            return
        }

        event.addData {
            add("parkour is loaded")
            add("tier: $tier")
            add("tierWasUnknown: $tierWasUnknown")
            add("factionType: ${CrimsonIsleReputationHelper.factionType}")
        }
    }
}
