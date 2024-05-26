package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object ChocolateFactoryTimeTowerManager {

    private val config get() = ChocolateFactoryAPI.config
    private val profileStorage get() = ChocolateFactoryAPI.profileStorage

    private var lastTimeTowerWarning = SimpleTimeMark.farPast()
    private var lastTimeTowerReminder = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val profileStorage = profileStorage ?: return

        if (SimpleTimeMark(profileStorage.currentTimeTowerEnds).isInPast()) {
            profileStorage.currentTimeTowerEnds = SimpleTimeMark.farPast().toMillis()
        }

        if (ChocolateFactoryAPI.inChocolateFactory) return

        if (config.timeTowerReminder) {
            timeTowerReminder()
        }

        val nextCharge = SimpleTimeMark(profileStorage.nextTimeTower)

        if (nextCharge.isInPast() && !nextCharge.isFarPast() && currentCharges() < maxCharges()) {
            profileStorage.currentTimeTowerUses++

            val nextTimeTower = SimpleTimeMark(profileStorage.nextTimeTower) + (profileStorage.timeTowerCooldown).hours
            profileStorage.nextTimeTower = nextTimeTower.toMillis()

            if (!config.timeTowerWarning) return
            ChatUtils.clickableChat(
                "Your Time Tower has another charge available §7(${timeTowerCharges()})§e, " +
                    "Click here to use one",
                onClick = {
                    HypixelCommands.chocolateFactory()
                }
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
            onClick = {
                HypixelCommands.chocolateFactory()
            }
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
        val currentTime = profileStorage?.lastDataSave ?: 0
        val endTime = timeTowerEnds()

        return endTime > currentTime
    }

    private fun timeTowerEnds(): Long {
        return profileStorage?.currentTimeTowerEnds ?: 0
    }

    private fun timeTowerReminder() {
        if (lastTimeTowerReminder.passedSince() < 20.seconds) return

        val timeUntil = SimpleTimeMark(timeTowerEnds()).timeUntil()
        if (timeUntil < 1.minutes && timeUntil.isPositive()) {
            ChatUtils.clickableChat(
                "§cYour Time Tower is about to end! " +
                    "Open the Chocolate Factory to avoid wasting the multiplier!",
                onClick = {
                    HypixelCommands.chocolateFactory()
                }
            )
            SoundUtils.playBeepSound()
            lastTimeTowerReminder = SimpleTimeMark.now()
        }
    }

    fun timeTowerFullTimeMark(): SimpleTimeMark {
        val profileStorage = profileStorage ?: return SimpleTimeMark.farPast()
        if (timeTowerFull()) return SimpleTimeMark.farPast()
        val nextChargeDuration = SimpleTimeMark(profileStorage.nextTimeTower)
        val remainingChargesAfter = profileStorage.maxTimeTowerUses - (profileStorage.currentTimeTowerUses + 1)
        val endTime = nextChargeDuration + (profileStorage.timeTowerCooldown).hours * remainingChargesAfter

        return endTime
    }

    fun timeTowerActiveDuration(): Duration {
        if (!timeTowerActive()) return Duration.ZERO
        val currentTime = profileStorage?.lastDataSave ?: 0
        val endTime = profileStorage?.currentTimeTowerEnds ?: 0

        val duration = endTime - currentTime
        return duration.milliseconds
    }

    @SubscribeEvent
    fun onProfileChange(event: ProfileJoinEvent) {
        lastTimeTowerWarning = SimpleTimeMark.farPast()
    }
}
