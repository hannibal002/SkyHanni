package at.hannibal2.skyhanni.data.hypixel.location

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.HypixelJoinEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.events.modapi.HypixelHelloEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.hypixel.data.region.Environment
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object NetworkData {
    private val patternGroup = RepoPattern.group("data.hypixeldata.servername")

    private val serverNameConnectionPattern by patternGroup.pattern(
        "servername",
        "(?<prefix>.+\\.)?hypixel\\.net",
    )
    private val serverNameScoreboardPattern by patternGroup.pattern(
        "servername",
        "§e(?<prefix>.+\\.)?hypixel\\.net",
    )

    private var hypixelEnvironment: Environment? = null

    fun onHypixel() = hypixelEnvironment != null
    fun onLive() = hypixelEnvironment == Environment.PRODUCTION
    fun onAlpha() = hypixelEnvironment == Environment.BETA

    @HandleEvent
    fun onHypixelHello(event: HypixelHelloEvent) {
        onJoinHypixel(event.environment)
    }

    @HandleEvent
    fun onDisconnect(event: ClientDisconnectEvent) {
        hypixelEnvironment = null
    }

    @SubscribeEvent
    fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        if (hypixelEnvironment == null) {
            val newEnvironment = readEnvironmentFromWorld()
            if (newEnvironment != null) {
                hypixelEnvironment = newEnvironment
                onJoinHypixel(newEnvironment)
            }
        }
    }

    private fun onJoinHypixel(environment: Environment) {
        hypixelEnvironment = environment
        HypixelJoinEvent(environment).postAndCatch()
        SkyHanniMod.repo.displayRepoStatus(true)
    }

    private fun readEnvironmentFromWorld(): Environment? {
        val mc = Minecraft.getMinecraft()
        val player = mc.thePlayer ?: return null

        var hypixel = false
        var hypixelAlpha = false

        player.clientBrand?.let {
            if (it.contains("hypixel", ignoreCase = true)) {
                hypixel = true
            }
        }

        serverNameConnectionPattern.matchMatcher(mc.currentServerData?.serverIP ?: "") {
            hypixel = true
            if (group("prefix") == "alpha.") {
                hypixelAlpha = true
            }
        }

        for (line in ScoreboardData.sidebarLinesFormatted) {
            serverNameScoreboardPattern.matchMatcher(line) {
                hypixel = true
                if (group("prefix") == "alpha.") {
                    hypixelAlpha = true
                }
            }
        }

        return when {
            hypixel && hypixelAlpha -> Environment.BETA
            hypixel -> Environment.PRODUCTION
            else -> null
        }
    }
}
