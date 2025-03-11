package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.RescueParkourJson
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
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
    private val recruiterMagePattern by RepoPattern.pattern(
        "recruiter.mage",
        "§e\\[NPC] §eRescue Recruiter§f: §rYou want to help us rescue a hostage from those filthy brutes over there\\?",
    )

    // TODO fix
    /**
     * REGEX-TEST: §e[NPC] §eRescue Recruiter§f:
     */
    private val recruiterBarbarianPattern by RepoPattern.pattern(
        "recruiter.barbarian",
        "§e\\[NPC] §eRescue Recruiter§f: ",
    )

    /**
     * REGEX-TEST: §cYou took too long and the agents found someone else.
     */
    private val cancelTimeoutPattern by RepoPattern.pattern(
        "cancel.timeout",
        "§cYou took too long and the agents found someone else.",
    )

    /**
     * REGEX-TEST: §e[NPC] §eUndercover Agent§f: §rIt's too dangerous for you?
     */
    private val cancelAfraid by RepoPattern.pattern(
        "cancel.afraid",
        "§e[NPC] §eUndercover Agent§f: §rIt's too dangerous for you?",
    )

    /**
     * REGEX-TEST: §aⒸ §eRescue Mission
     */
    private val questTierPattern by RepoPattern.pattern(
        "tier",
        "§a(?<tier>.) §cRescue Mission",
    )

    private var parkourHelper: ParkourHelper? = null
    private var tier: String? = null
    private var tierWasUnknown = false
    private var data: RescueParkourJson? = null

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        parkourHelper = null
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Rescue") return
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
                data.barb["S${config.variant}"]?.let {
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
            parkourHelper = null
        },
    )

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        data = event.getConstant<RescueParkourJson>("RescueMissionWaypoints")
        updateConfig()
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (config.hostagePath) {
            agentDialoguePattern.matchMatcher(event.message.removeColor()) {
                tier?.let {
                    initParkour()
                } ?: run {
                    ChatUtils.userError("Rescue mission tier still not known! Check out the Quest Board first as I told you earlier!")
                    navigateToQuestBoard("forgot to check out tier")
                }
            }
        }
        if (recruiterMagePattern.matches(event.message) || recruiterBarbarianPattern.matches(event.message)) {
            if (config.hostagePath) {
                if (tier == null) {
                    DelayedRun.runNextTick {
                        ChatUtils.userError("Rescue mission tier not known! Check out the Quest Board first!")
                    }
                    tierWasUnknown = true
                    return
                }
            }
            navigateToUndercoverAgent()
        }
        if (config.hostagePath) {
            if (cancelAfraid.matches(event.message)) {
                parkourHelper = null
            }
            if (cancelTimeoutPattern.matches(event.message)) {
                parkourHelper = null
                navigateToQuestBoard("run out of time")
            }
        }
    }

    private fun navigateToUndercoverAgent() {
        if (!config.agentPath) return
        val factionType = CrimsonIsleReputationHelper.factionType ?: return
        val location = when (factionType) {
            FactionType.MAGE -> LorenzVec(-15.5, 93.0, -843.7)

            // TODO fix
            FactionType.BARBARIAN -> LorenzVec(-15.5, 93.0, -843.7)
        }
        IslandGraphs.pathFind(
            location,
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
            condition = { config.agentPath },
        )
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.hostagePath) return

        parkourHelper?.render(event)
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.chroma, config.solidColor, config.lookAhead) {
            updateConfig()
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

}
