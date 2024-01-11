package at.hannibal2.skyhanni.features.mining.crystalhollows

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.abs

class CrystalHollowsLobbyAgeWarning {
    private val mc get() = Minecraft.getMinecraft()

    private val TICKS_PER_MC_DAY_MINECRAFT: Long = 24000L

    private val crystalHollowsLobbyRepoGroup = RepoPattern.group("crystalhollows.lobbyage")
    private val playerCountTabListPattern by crystalHollowsLobbyRepoGroup.pattern(("playercount.tablist"), (" *(?:§.)*Players[\\S ]*[\\(\\[\\{](?<playerCount>[\\S ]+)[\\)\\]\\}]"))

    private var lobbyIsPastAgeThreshold: Boolean = false
    private var oldPlayerCount: Long = 0L
    private var oldLobbyAge: Long = 0L

    private fun getLobbyAgeInMinecraftDays(): Long {
        return (mc.theWorld.worldTime) / TICKS_PER_MC_DAY_MINECRAFT
    }

    private fun processPlayerCount() {
        if (!isInCrystalHollows()) return
        for (line in TabListData.getTabList()) {
            playerCountTabListPattern.matchMatcher(line) {
                val playerCount = group("playerCount").formatNumber()
                if (oldPlayerCount == playerCount) return
                oldPlayerCount = playerCount
                if (config.playerCountReminders) LorenzUtils.chat("§a$playerCount players are currently in this Crystal Hollows lobby.")
                if (!lobbyIsPastAgeThreshold) return
                if (playerCount in 1..config.minPlayers) LorenzUtils.chat("§cThere are $playerCount players remaining in this Crystal Hollows lobby. §4§lIt will shut down very soon.")
                else LorenzUtils.chat("There are $playerCount players remaining in this Crystal Hollows lobby. §cIt will shut down when there are fewer than ${config.minPlayers} people left, or when this lobby reaches Day ${config.maxLobbyAgeThreshold} §e(whichever happens first).")
            }
        }
    }

    private fun processLobbyAge() {
        if (!isInCrystalHollows()) return
        val lobbyAge = getLobbyAgeInMinecraftDays()
        if (oldLobbyAge == lobbyAge) return
        oldLobbyAge = lobbyAge
        if (config.lobbyAgeReminders && !lobbyIsPastAgeThreshold) LorenzUtils.chat("§aThis Crystal Hollows lobby is currently at Day $lobbyAge, ${abs(config.minLobbyAgeThreshold - lobbyAge)} days away from Day ${config.minLobbyAgeThreshold}.")
        if (lobbyAge in config.minLobbyAgeThreshold..< config.maxLobbyAgeThreshold && !lobbyIsPastAgeThreshold) {
            lobbyIsPastAgeThreshold = true
            LorenzUtils.chat("This Crystal Hollows lobby has reached Day ${config.minLobbyAgeThreshold}. It no longer accepts new players, §cand will shut down on Day ${config.maxLobbyAgeThreshold} or when there are fewer than ${config.minPlayers} people left §e(whichever happens first).")
        } else if (lobbyAge >= config.maxLobbyAgeThreshold) LorenzUtils.chat("§cThis Crystal Hollows lobby has reached Day ${config.maxLobbyAgeThreshold}. §4§lIt will shut down very soon.")
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
        if (event.newIsland != IslandType.CRYSTAL_HOLLOWS) {
            lobbyIsPastAgeThreshold = false
            oldPlayerCount = 0L
        }
    }

    private val config get() = SkyHanniMod.feature.mining.crystalHollowsLobbyAgeWarning
    private fun isEnabled() = config.enabled && isInCrystalHollows()
    private fun isInCrystalHollows() = IslandType.CRYSTAL_HOLLOWS.isInIsland()
}
