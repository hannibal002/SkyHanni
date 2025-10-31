package at.hannibal2.hanni.features.event.hoppity

import at.hannibal2.hanni.api.pet.CurrentPetApi
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.SimpleTimeMark
import kotlin.time.Duration.Companion.seconds

object MythicRabbitPetWarning {
    private val MYTHIC_RABBIT = "RABBIT;5".toInternalName()
    private var lastCheck = SimpleTimeMark.farPast()

    fun check() {
        if (!HoppityEggsManager.config.petWarning) return

        if (lastCheck.passedSince() < 30.seconds) return

        if (!correctPet()) {
            lastCheck = SimpleTimeMark.now()
            warn()
        }
    }

    fun correctPet() = CurrentPetApi.isCurrentPet(MYTHIC_RABBIT)

    private fun warn() {
        ChatUtils.chat("Use a §dMythic Rabbit Pet §efor more chocolate!")
        TitleManager.sendTitle("§cNo Rabbit Pet!")
    }
}
