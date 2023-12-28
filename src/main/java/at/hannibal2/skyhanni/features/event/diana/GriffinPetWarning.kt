package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class GriffinPetWarning {
    private var lastWarnTime = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!event.isMod(20)) return
        if (!SkyHanniMod.feature.event.diana.petWarning) return
        if (!DianaAPI.isDoingDiana()) return
        if (!DianaAPI.hasSpadeInHand()) return

        if (!DianaAPI.hasGriffinPet() && lastWarnTime.passedSince() > 30.seconds) {
            lastWarnTime = SimpleTimeMark.now()
            LorenzUtils.sendTitle("§cGriffin Pet!", 3.seconds)
            LorenzUtils.chat("Reminder to use a Griffin pet for Mythological Ritual!")
        }
    }
}
