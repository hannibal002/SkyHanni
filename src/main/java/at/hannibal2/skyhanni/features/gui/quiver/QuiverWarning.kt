package at.hannibal2.hanni.features.gui.quiver

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.ArrowType
import at.hannibal2.hanni.data.QuiverApi
import at.hannibal2.hanni.data.QuiverApi.amount
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.QuiverUpdateEvent
import at.hannibal2.hanni.events.dungeon.DungeonCompleteEvent
import at.hannibal2.hanni.events.kuudra.KuudraCompleteEvent
import at.hannibal2.hanni.features.dungeon.DungeonApi
import at.hannibal2.hanni.features.nether.kuudra.KuudraApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.hanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SoundUtils
import at.hannibal2.hanni.utils.StringUtils.createCommaSeparatedList
import kotlin.time.Duration.Companion.seconds

@HanniModule
object QuiverWarning {

    private val config get() = HanniMod.feature.combat.quiverConfig

    private var lastLowQuiverReminder = SimpleTimeMark.farPast()
    private val arrowsInInstance = mutableSetOf<ArrowType>()

    @HandleEvent(eventTypes = [DungeonCompleteEvent::class, KuudraCompleteEvent::class])
    fun onInstanceComplete() {
        val arrows = arrowsInInstance.filterTo(mutableSetOf()) { it.amount <= config.lowQuiverAmount }
        arrowsInInstance.clear()

        if (arrows.isNotEmpty() && config.reminderAfterRun) {
            DelayedRun.runNextTick {
                instanceAlert(arrows)
            }
        }
    }

    private fun instanceAlert(arrows: Set<ArrowType>) {
        val arrowsText = arrows.map { arrowType ->
            val rarity = arrowType.internalName.getItemStackOrNull()?.getItemRarityOrNull()?.chatColorCode ?: "§f"
            "$rarity${arrowType.arrow}"
        }.createCommaSeparatedList()
        TitleManager.sendTitle("§cLow on arrows!")
        ChatUtils.chat("Low on $arrowsText!")
        SoundUtils.repeatSound(100, 30, SoundUtils.plingSound)
    }

    private fun lowQuiverAlert(amount: Int) {
        if (lastLowQuiverReminder.passedSince() < 30.seconds) return
        lastLowQuiverReminder = SimpleTimeMark.now()
        TitleManager.sendTitle("§cLow on arrows!")
        ChatUtils.chat("Low on arrows §e(${amount.addSeparators()} left)")
    }

    @HandleEvent
    fun onQuiverUpdate(event: QuiverUpdateEvent) {
        val amount = event.currentAmount
        val arrow = event.currentArrow ?: return
        if (arrow == QuiverApi.NONE_ARROW_TYPE) return

        if (inInstance()) arrowsInInstance.add(arrow)

        if (amount > config.lowQuiverAmount) return
        if (config.lowQuiverNotification) {
            lowQuiverAlert(amount)
        }
    }

    @HandleEvent
    fun onWorldChange() = arrowsInInstance.clear()

    private fun inInstance() = DungeonApi.inDungeon() || KuudraApi.inKuudra

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(35, "inventory.quiverAlert", "combat.quiverConfig.lowQuiverNotification")
    }
}
