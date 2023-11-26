package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.events.garden.pests.PestSpawnEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.misc.LockMouseLook
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class PestSpawn {
    private val config get() = PestAPI.config.pestSpawn

    private val patternOnePest = "§6§l.*! §7A §6Pest §7has appeared in §aPlot §7- §b(?<plot>.*)§7!".toPattern()
    private val patternMultiplePests =
        "§6§l.*! §6(?<amount>\\d) Pests §7have spawned in §aPlot §7- §b(?<plot>.*)§7!".toPattern()

    private var lastPlotTp: String? = null

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!GardenAPI.inGarden()) return

        var blocked = false

        patternOnePest.matchMatcher(event.message) {
            pestSpawn(1, group("plot"))
            blocked = true
        }
        patternMultiplePests.matchMatcher(event.message) {
            pestSpawn(group("amount").toInt(), group("plot"))
            blocked = true
        }
        if (event.message == "  §r§e§lCLICK HERE §eto teleport to the plot!") {
            if (PestSpawnTimer.lastSpawnTime.passedSince() < 1.seconds) {
                blocked = true
            }
        }

        if (blocked && config.chatMessageFormat != 0) {
            event.blockedReason = "pests_spawn"
        }
    }

    private fun pestSpawn(amount: Int, plotName: String) {
        PestSpawnEvent(amount, plotName).postAndCatch()
        lastPlotTp = plotName

        if (config.showTitle) {
            LorenzUtils.sendTitle("§aPest Spawn! §e$amount §ain §b$plotName§a!", 7.seconds)
        }

        if (config.chatMessageFormat == 1) {
            LorenzUtils.clickableChat(
                "§aPest Spawn! §e$amount §ain §b$plotName§a!",
                "tptoplot $plotName"
            )
        }
    }

    @SubscribeEvent
    fun onKeyClick(event: LorenzKeyPressEvent) {
        if (!GardenAPI.inGarden()) return
        if (Minecraft.getMinecraft().currentScreen != null) return
        if (NEUItems.neuHasFocus()) return

        if (event.keyCode != config.teleportHotkey) return

        lastPlotTp?.let {
            lastPlotTp = null
            LorenzUtils.sendCommandToServer("tptoplot $it")
            LockMouseLook.autoDisable()
        }
    }
}
