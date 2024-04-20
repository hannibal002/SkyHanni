package at.hannibal2.skyhanni.features.event.chocolatefactory

import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import kotlin.time.Duration.Companion.seconds

object ChocolateFactoryTimeTowerManager {

    private val config get() = ChocolateFactoryAPI.config
    private val profileStorage get() = ChocolateFactoryAPI.profileStorage

    private var lastWarningSent = SimpleTimeMark.farPast()

    fun trySendTimeTowerFullMessage() {
        if (!ChocolateFactoryAPI.isEnabled()) return
        if (!config.timeTowerWarning) return
        val profileStorage = profileStorage ?: return

        val timeTowerFull = profileStorage.currentTimeTowerUses >= profileStorage.maxTimeTowerUses

        if (!timeTowerFull) return

        if (lastWarningSent.passedSince() < 30.seconds) return
        lastWarningSent = SimpleTimeMark.now()

        ChatUtils.clickableChat(
            "§cYour Time Tower is full §7(${timeTowerCharges()}§c, " +
                "Use it to avoid wasting time tower usages!",
            "cf"
        )
        SoundUtils.playBeepSound()
        lastWarningSent = SimpleTimeMark.now()
    }

    fun timeTowerCharges(): String {
        val profileStorage = profileStorage ?: return "Unknown"
        return "${profileStorage.currentTimeTowerUses}/${profileStorage.maxTimeTowerUses} Charges"
    }

    fun currentCharges(): Int {
        return profileStorage?.currentTimeTowerUses ?: -1
    }
}
