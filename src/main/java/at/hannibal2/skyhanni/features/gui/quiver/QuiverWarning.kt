package at.hannibal2.skyhanni.features.gui.quiver

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ArrowType
import at.hannibal2.skyhanni.data.QuiverAPI
import at.hannibal2.skyhanni.data.QuiverAPI.arrowAmount
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.events.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.DungeonEnterEvent
import at.hannibal2.skyhanni.events.KuudraCompleteEvent
import at.hannibal2.skyhanni.events.KuudraEnterEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.QuiverUpdateEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.createCommaSeparatedList
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class QuiverWarning {

    private val config get() = SkyHanniMod.feature.combat.quiverConfig

    private var arrow: ArrowType? = null
    private var amount = QuiverAPI.currentAmount
    private var lastLowQuiverReminder = SimpleTimeMark.farPast()
    private var arrowsUsedInRun = mutableListOf<ArrowType>()
    private var arrowsToAlert = mutableListOf<String>()
    private var inInstance = false

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        arrowsUsedInRun = mutableListOf()
        arrowsToAlert = mutableListOf()
        inInstance = false
    }

    @SubscribeEvent
    fun onDungeonEnter(event: DungeonEnterEvent) {
        onInstanceEnter()
    }

    @SubscribeEvent
    fun onKuudraEnter(event: KuudraEnterEvent) {
        onInstanceEnter()
    }

    private fun onInstanceEnter() {
        arrowsUsedInRun = mutableListOf()
        arrowsToAlert = mutableListOf()
        inInstance = true
    }

    @SubscribeEvent
    fun onDungeonComplete(event: DungeonCompleteEvent) {
        onInstanceComplete()
    }

    @SubscribeEvent
    fun onKuudraComplete(event: KuudraCompleteEvent) {
        onInstanceComplete()
    }

    private fun onInstanceComplete() {
        if (!config.reminderAfterRun) return
        if (arrowsUsedInRun.isEmpty()) return
        for (arrow in arrowsUsedInRun) {
            val internalName = arrow.internalName
            val amount = arrowAmount[internalName] ?: continue
            if (amount > config.lowQuiverAmount) continue
            val rarity = internalName.getItemStackOrNull()?.getItemRarityOrNull()?.chatColorCode ?: "§f"
            arrowsToAlert.add(rarity + arrow.arrow)
        }
        if (arrowsToAlert.isNotEmpty()) instanceAlert()
    }

    private fun instanceAlert() {
        DelayedRun.runNextTick {
            TitleManager.sendTitle("§cLow on arrows!", 5.seconds, 3.6, 7f)
            ChatUtils.chat("Low on ${arrowsToAlert.createCommaSeparatedList()}!")
            SoundUtils.repeatSound(100, 30, SoundUtils.plingSound)
        }
    }

    private fun lowQuiverAlert() {
        if (lastLowQuiverReminder.passedSince() < 30.seconds) return
        lastLowQuiverReminder = SimpleTimeMark.now()
        val itemStack = getItemStackOrNull(arrow?.internalName?.asString() ?: return) ?: return
        val rarity = itemStack.getItemRarityOrNull()?.chatColorCode ?: "§f"
        TitleManager.sendTitle("§cLow on $rarity${arrow?.arrow}!", 5.seconds, 3.6, 7f)
        ChatUtils.chat("Low on $rarity${arrow?.arrow} §e(${amount.addSeparators()} left)")
    }

    @SubscribeEvent
    fun onQuiverUpdate(event: QuiverUpdateEvent) {
        val lastArrow = arrow
        val lastAmount = amount

        if (config.lowQuiverNotification && amount <= config.lowQuiverAmount) {
            if (arrow != lastArrow || (arrow == lastArrow && amount <= lastAmount)) lowQuiverAlert()
        }

        if (inInstance) {
            if (!arrowsUsedInRun.contains(arrow)) arrowsUsedInRun.add(arrow ?: return)
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(35, "inventory.quiverAlert", "combat.quiverConfig.lowQuiverNotification")
    }
}
