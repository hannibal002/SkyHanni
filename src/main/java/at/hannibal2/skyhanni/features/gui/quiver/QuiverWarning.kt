package at.hannibal2.skyhanni.features.gui.quiver

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.KuudraCompleteEvent
import at.hannibal2.skyhanni.events.QuiverUpdateEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class QuiverWarning {

    private val config get() = SkyHanniMod.feature.combat.quiverConfig

    private var lastLowQuiverReminder = SimpleTimeMark.farPast()
    private var lowDuringInstance = false
    private var amount = 0

    @SubscribeEvent
    fun onDungeonComplete(event: DungeonCompleteEvent) {
        onInstanceComplete()
    }

    @SubscribeEvent
    fun onKuudraComplete(event: KuudraCompleteEvent) {
        onInstanceComplete()
    }

    private fun onInstanceComplete() {
        if (!lowDuringInstance) return
        lowDuringInstance = false

        if (config.reminderAfterRun) {
            lowQuiverAlert()
        }
    }

    private fun lowQuiverAlert() {
        if (lastLowQuiverReminder.passedSince() < 30.seconds) return
        lastLowQuiverReminder = SimpleTimeMark.now()
        TitleManager.sendTitle("§cLow on arrows!", 5.seconds, 3.6, 7f)
        ChatUtils.chat("Low on arrows §e(${amount.addSeparators()} left)")
    }

    @SubscribeEvent
    fun onQuiverUpdate(event: QuiverUpdateEvent) {
        amount = event.currentAmount

        if (amount > config.lowQuiverAmount) return
        if (DungeonAPI.inDungeon() || LorenzUtils.inKuudraFight) {
            lowDuringInstance = true
        }
        if (config.lowQuiverNotification) {
            lowQuiverAlert()
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(35, "inventory.quiverAlert", "combat.quiverConfig.lowQuiverNotification")
    }
}
