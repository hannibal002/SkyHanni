package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.RescueParkourJson
import at.hannibal2.skyhanni.data.repo.RepoManager
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.features.nether.reputationhelper.CrimsonIsleReputationHelper
import at.hannibal2.skyhanni.features.nether.reputationhelper.FactionType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.ParkourHelper
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColor
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object RescueMissionWaypoints {

    // Logic for the parkour location waypoint system is based on the Deep Caverns Guide by hannibal2

    private val config get() = SkyHanniMod.feature.crimsonIsle.reputationHelper.rescueMissionConfig
    private val patternGroup = RepoPattern.group("crimson.rescue")
    private val agentDialoguePattern by patternGroup.pattern(
        "start",
        "\\[NPC] Undercover Agent: Here's what you need to do."
    )
    private val questTierPattern by RepoPattern.pattern(
        "tier",
        "§\\w(?<tier>.) §[\\w ]+"
    )



    private var show = false
    private var parkourHelper: ParkourHelper? = null
    private var tier: String? = null
    private var data: RescueParkourJson? = null

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        show = false
        parkourHelper?.reset()
    }

    @HandleEvent
    fun onTooltip(event: ToolTipEvent) {
        if (!isEnabled()) return
        if (!IslandType.CRIMSON_ISLE.isInIsland()) return
        if (parkourHelper != null) return
        if (data == null) return

        questTierPattern.matchMatcher(event.itemStack.name) {
            tier = group("tier").toLetter()
        }

        if (tier.isNullOrEmpty()) return

        if (tier == "S") {
            if (CrimsonIsleReputationHelper.factionType == FactionType.MAGE) {
                ErrorManager.logErrorStateWithData(
                    "No data present for Mage S-rank Rescue Mission",
                    "No Mage S-Rank in repo"
                )
                return
            }

            // will get data["S1"] or data["S2"]
            parkourHelper = data!!.barb["S${config.variant}"]?.let {
                ParkourHelper(
                    it,
                    listOf(),
                    platformSize = 1.0,
                    detectionRange = 3.5,
                    onEndReach = {
                        show = false
                    }
                )
            }
            return
        }

        parkourHelper = when (CrimsonIsleReputationHelper.factionType) {
            FactionType.MAGE -> data!!.mage[tier]?.let {
                ParkourHelper(
                    it,
                    listOf(),
                    platformSize = 1.0,
                    detectionRange = 3.5,
                    onEndReach = {
                        show = false
                    }
                )
            }
            FactionType.BARBARIAN -> data!!.barb[tier]?.let {
                ParkourHelper(
                    it,
                    listOf(),
                    platformSize = 1.0,
                    detectionRange = 3.5,
                    onEndReach = {
                        show = false
                    }
                )
            }
            null -> null
        }
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        data = event.getConstant<RescueParkourJson>("RescueMissionWaypoints")
        updateConfig()
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return

        agentDialoguePattern.matchMatcher(event.message.removeColor()) {
            if (!show) {
                startWaypoints()
            }
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (!show) return

        parkourHelper?.render(event)
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.chroma, config.solidColor, config.lookAhead) {
            updateConfig()
        }
    }

    private fun startWaypoints() {
        show = true
        parkourHelper?.reset()
//         updateConfig()
        if (parkourHelper == null) {
            ChatUtils.clickableChat(
                "RescueMissionWaypoints missing in SkyHanni Repo! Try /shupdaterepo to fix it!",
                onClick = {
                    RepoManager.updateRepo()
                },
                "§eClick to update the repo!",
                prefixColor = "§c"
            )
        }
    }

    private fun String.toLetter(): String {
        return when (this) {
            "Ⓢ" -> "S"
            "Ⓐ" -> "A"
            "Ⓑ" -> "B"
            "Ⓒ" -> "C"
            "Ⓓ" -> "D"
            else -> ""
        }
    }

    private fun updateConfig() {
        parkourHelper?.run {
            rainbowColor = config.chroma.get()
            monochromeColor = config.solidColor.get().toSpecialColor()
            lookAhead = config.lookAhead.get() + 1
        }
    }

    fun isEnabled() = config.enabled
}
