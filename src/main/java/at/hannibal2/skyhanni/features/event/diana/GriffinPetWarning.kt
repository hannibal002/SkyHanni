package at.hannibal2.hanni.features.event.diana

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.IslandChangeEvent
import at.hannibal2.hanni.events.diana.BurrowDugEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.SimpleTimeMark
import kotlin.time.Duration.Companion.seconds

@HanniModule
object GriffinPetWarning {
    private val config get() = HanniMod.feature.event.diana
    private var wasCorrectPetAlready = false
    private var lastWarnTime = SimpleTimeMark.farPast()

    @HandleEvent
    fun onTick(event: HanniTickEvent) {
        if (!event.isMod(10)) return
        if (!config.petWarning) return
        if (!DianaApi.isDoingDiana()) return
        if (!DianaApi.hasSpadeInHand()) return

        val hasGriffinPet = DianaApi.hasGriffinPet()
        if (hasGriffinPet) {
            wasCorrectPetAlready = true
            return
        }

        if (wasCorrectPetAlready) return

        warn()
    }

    @HandleEvent
    fun onBurrowDug(event: BurrowDugEvent) {
        DelayedRun.runDelayed(2.seconds) {
            wasCorrectPetAlready = false
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        DelayedRun.runDelayed(5.seconds) {
            wasCorrectPetAlready = false
        }
    }

    private fun warn() {
        ChatUtils.clickToActionOrDisable(
            "Reminder to use a Griffin pet for Mythological Ritual!",
            config::petWarning,
            actionName = "open pets menu",
            action = { HypixelCommands.pet() },
        )
        if (lastWarnTime.passedSince() > 30.seconds) {
            lastWarnTime = SimpleTimeMark.now()
            TitleManager.sendTitle("§cGriffin Pet!")
        }
    }
}
