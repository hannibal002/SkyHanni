package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.CrystalHollowsLobbyAgeWarningJson
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.abs

class CrystalHollowsLobbyAgeWarning {
    private val config get() = SkyHanniMod.feature.mining.crystalHollowsLobbyAgeWarning
    private val mc get() = Minecraft.getMinecraft()

    private val THE_CRYSTAL_HOLLOWS = IslandType.CRYSTAL_HOLLOWS

    private val TICKS_PER_MC_DAY: Long = 24000L

    private val crystalHollowsLobbyRepoGroup = RepoPattern.group("chlobbyage")
    private val playerCountTabListPattern by crystalHollowsLobbyRepoGroup.pattern(
        "playercount.tablist",
        " *(?:§.)*Players[\\S ]*[({\\[](?<playerCount>[\\S ]+)[)}\\]]"
    )

    private var lobbyIsPastAgeThreshold: Boolean = false
    private var oldPlayerCount: Long = 0L
    private var oldLobbyAge: Long = 0L
    private var minPlayers: Int = 4
    private var minLobbyAgeMCDays: Int = 18
    private var maxLobbyAgeMCDays: Int = 25

    private fun getLobbyAgeInMinecraftDays(): Long = (mc.theWorld.worldTime) / TICKS_PER_MC_DAY //world day changes at 6AM in skyblock time

    private fun processPlayerCount() {
        if (!isInCrystalHollows()) return //avoid race condition
        for (line in TabListData.getTabList()) {
            playerCountTabListPattern.matchMatcher(line) {
                val playerCount = group("playerCount").formatNumber()
                if (oldPlayerCount == playerCount) return
                oldPlayerCount = playerCount
                if (config.playerCountReminders) LorenzUtils.chat("§a$playerCount players are currently in this Crystal Hollows lobby.")
                if (!lobbyIsPastAgeThreshold) return
                if (playerCount in 1..minPlayers) LorenzUtils.chat("§cThere are $playerCount players remaining in this Crystal Hollows lobby. §4§lIt will shut down very soon.")
                else LorenzUtils.chat("There are $playerCount players remaining in this Crystal Hollows lobby. §cIt will shut down when there are fewer than $minPlayers people left, or when this lobby reaches Day $maxLobbyAgeMCDays.")
            }
        }
    }

    private fun processLobbyAge() {
        if (!isInCrystalHollows()) return //avoid race condition
        val lobbyAge = getLobbyAgeInMinecraftDays()
        if (oldLobbyAge == lobbyAge) return
        oldLobbyAge = lobbyAge
        if (config.lobbyAgeReminders && !lobbyIsPastAgeThreshold) LorenzUtils.chat("§aThis Crystal Hollows lobby is currently at Day $lobbyAge, ${StringUtils.optionalPlural(number = abs(minLobbyAgeMCDays - lobbyAge).toInt(),"day", plural = "days")} away from Day $minLobbyAgeMCDays.")
        if (lobbyAge in minLobbyAgeMCDays..< maxLobbyAgeMCDays && !lobbyIsPastAgeThreshold) {
            lobbyIsPastAgeThreshold = true
            LorenzUtils.chat("This Crystal Hollows lobby has reached Day $minLobbyAgeMCDays. It no longer accepts new players, §cand will shut down on Day $maxLobbyAgeMCDays or when there are fewer than $minPlayers people left.")
        } else if (lobbyAge >= maxLobbyAgeMCDays) LorenzUtils.chat("§cThis Crystal Hollows lobby has reached Day $maxLobbyAgeMCDays. §4§lIt will shut down very soon.")
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) return
        if (!event.repeatSeconds(1)) return
        processLobbyAge()
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) return
        processPlayerCount()
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland != THE_CRYSTAL_HOLLOWS) {
            lobbyIsPastAgeThreshold = false
            oldPlayerCount = 0L
            oldLobbyAge = 0L
        }
    }
    
    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<CrystalHollowsLobbyAgeWarningJson>("CrystalHollowsLobbyAgeWarning")
        minPlayers = data.minPlayers
        minLobbyAgeMCDays = data.minLobbyAgeMCDays
        maxLobbyAgeMCDays = data.maxLobbyAgeMCDays
    }

    private fun isEnabled() = config.enabled && isInCrystalHollows()
    private fun isInCrystalHollows() = THE_CRYSTAL_HOLLOWS.isInIsland()
}
