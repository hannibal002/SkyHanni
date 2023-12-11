package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.garden.pests.PestSpawnConfig
import at.hannibal2.skyhanni.config.features.garden.pests.PestSpawnConfig.ChatMessageFormatEntry
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.garden.pests.PestSpawnEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class PestSpawn {
    private val config get() = PestAPI.config.pestSpawn

    private val patternOnePest = "§6§l.*! §7A §6Pest §7has appeared in §aPlot §7- §b(?<plot>.*)§7!".toPattern()
    private val patternMultiplePests =
        "§6§l.*! §6(?<amount>\\d) Pests §7have spawned in §aPlot §7- §b(?<plot>.*)§7!".toPattern()

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

        if (blocked && config.chatMessageFormat != PestSpawnConfig.ChatMessageFormatEntry.HYPIXEL) {
            event.blockedReason = "pests_spawn"
        }
    }

    private fun pestSpawn(amount: Int, plotName: String) {
        PestSpawnEvent(amount, plotName).postAndCatch()

        if (config.showTitle) {
            LorenzUtils.sendTitle("§aPest Spawn! §e$amount §ain §b$plotName§a!", 7.seconds)
        }

        if (config.chatMessageFormat == PestSpawnConfig.ChatMessageFormatEntry.COMPACT) {
            LorenzUtils.clickableChat(
                "§aPest Spawn! §e$amount §ain §b$plotName§a!",
                "tptoplot $plotName"
            )
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(14, "garden.pests.pestSpawn.chatMessageFormat") { element ->
            ConfigUtils.migrateIntToEnum(element, ChatMessageFormatEntry::class.java)
        }
    }
}
