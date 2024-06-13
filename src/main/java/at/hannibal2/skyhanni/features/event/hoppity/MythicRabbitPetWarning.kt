package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.data.PetAPI
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration.Companion.seconds

object MythicRabbitPetWarning {
    val mythicRabbit = "§dRabbit"
    var lastCheck = SimpleTimeMark.farPast()

    fun check() {
        if (!HoppityEggsManager.config.petWarning) return

        if (lastCheck.passedSince() < 30.seconds) return

        if (!PetAPI.isCurrentPet(mythicRabbit)) {
            lastCheck = SimpleTimeMark.now()
            warn()
        }
    }

    private fun warn() {
        ChatUtils.chat("Use a mythic Rabbit pet for more chocolate!")
        LorenzUtils.sendTitle("§cNo Rabbit Pet!", 3.seconds)
    }
}
