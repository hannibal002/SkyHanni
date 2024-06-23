package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ChocolateFactoryTimeTowerManager {

    private val config get() = ChocolateFactoryAPI.config
    private val profileStorage get() = ChocolateFactoryAPI.profileStorage

    private var lastTimeTowerWarning = SimpleTimeMark.farPast()
    private var lastTimeTowerReminder = SimpleTimeMark.farPast()

    private const val HOVER_TEXT = "§eClick to run /cf!"

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val profileStorage = profileStorage ?: return

        if (profileStorage.currentTimeTowerEnds.isInPast()) {
            profileStorage.currentTimeTowerEnds = SimpleTimeMark.farPast()
        }

        if (ChocolateFactoryAPI.inChocolateFactory) return

        if (config.timeTowerReminder) {
            timeTowerReminder()
        }

        val nextCharge = profileStorage.nextTimeTower

        if (nextCharge.isInPast() && !nextCharge.isFarPast() && currentCharges() < maxCharges()) {
            profileStorage.currentTimeTowerUses++

            val nextTimeTower = profileStorage.nextTimeTower + profileStorage.timeTowerCooldown.hours
            profileStorage.nextTimeTower = nextTimeTower

            if (!config.timeTowerWarning) return
            ChatUtils.clickableChat(
                "Your Time Tower has another charge available §7(${timeTowerCharges()})§e, " +
                    "Click here to use one",
                onClick = { HypixelCommands.chocolateFactory() },
                HOVER_TEXT,
            )
            SoundUtils.playBeepSound()
            lastTimeTowerWarning = SimpleTimeMark.now()
            return
        }
        checkTimeTowerWarning(false)
    }

    fun checkTimeTowerWarning(inInventory: Boolean) {
        if (!ChocolateFactoryAPI.isEnabled()) return
        if (!config.timeTowerWarning) return
        if (!timeTowerFull()) return
        if (ReminderUtils.isBusy()) return

        val warningSeparation = if (inInventory) 30.seconds else 5.minutes
        if (lastTimeTowerWarning.passedSince() < warningSeparation) return

        ChatUtils.clickableChat(
            "§cYour Time Tower is full §7(${timeTowerCharges()})§c, " +
                "Use one to avoid wasting time tower usages!",
            onClick = { HypixelCommands.chocolateFactory() },
            HOVER_TEXT
        )
        SoundUtils.playBeepSound()
        lastTimeTowerWarning = SimpleTimeMark.now()
    }

    fun timeTowerCharges(): String {
        return "${currentCharges()}/${maxCharges()} Charges"
    }

    fun currentCharges(): Int {
        return profileStorage?.currentTimeTowerUses ?: -1
    }

    private fun maxCharges(): Int {
        return profileStorage?.maxTimeTowerUses ?: 3
    }

    fun timeTowerFull() = currentCharges() >= maxCharges()

    fun timeTowerActive(): Boolean {
        val currentTime = profileStorage?.lastDataSave ?: SimpleTimeMark.farPast()
        val endTime = timeTowerEnds()

        return endTime > currentTime
    }

    private fun timeTowerEnds(): SimpleTimeMark = profileStorage?.currentTimeTowerEnds ?: SimpleTimeMark.farPast()

    private fun timeTowerReminder() {
        if (lastTimeTowerReminder.passedSince() < 20.seconds) return

        val timeUntil = timeTowerEnds().timeUntil()
        if (timeUntil < 1.minutes && timeUntil.isPositive()) {
            ChatUtils.clickableChat(
                "§cYour Time Tower is about to end! " +
                    "Open the Chocolate Factory to avoid wasting the multiplier!",
                onClick = { HypixelCommands.chocolateFactory() },
                HOVER_TEXT
            )
            SoundUtils.playBeepSound()
            lastTimeTowerReminder = SimpleTimeMark.now()
        }
    }

    fun timeTowerFullTimeMark(): SimpleTimeMark {
        val profileStorage = profileStorage ?: return SimpleTimeMark.farPast()
        if (timeTowerFull()) return SimpleTimeMark.farPast()
        val nextChargeDuration = profileStorage.nextTimeTower
        val remainingChargesAfter = profileStorage.maxTimeTowerUses - (profileStorage.currentTimeTowerUses + 1)
        val endTime = nextChargeDuration + ChocolateFactoryAPI.timeTowerChargeDuration() * remainingChargesAfter

        return endTime
    }

    fun timeTowerActiveDuration(): Duration {
        if (!timeTowerActive()) return Duration.ZERO
        val currentTime = profileStorage?.lastDataSave ?: SimpleTimeMark.farPast()
        val endTime = profileStorage?.currentTimeTowerEnds ?: SimpleTimeMark.farPast()

        return endTime - currentTime
    }

    @SubscribeEvent
    fun onProfileChange(event: ProfileJoinEvent) {
        lastTimeTowerWarning = SimpleTimeMark.farPast()
    }
}
