package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.data.PetAPI
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration.Companion.seconds

object MythicRabbitPetWarning {
    private const val MYTHIC_RABBIT = "§dRabbit"
    private var lastCheck = SimpleTimeMark.farPast()

    fun check() {
        if (!HoppityEggsManager.config.petWarning) return

        if (lastCheck.passedSince() < 30.seconds) return

        if (!correctPet()) {
            lastCheck = SimpleTimeMark.now()
            warn()
        }
    }

    fun correctPet() = PetAPI.isCurrentPet(MYTHIC_RABBIT)

    private fun warn() {
        ChatUtils.chat("Use a §dMythic Rabbit Pet §efor more chocolate!")
        LorenzUtils.sendTitle("§cNo Rabbit Pet!", 3.seconds)
    }
}
