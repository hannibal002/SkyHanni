package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object MuteStereoPants {

    private const val MIN_PITCH = 0.4920635f
    private const val MAX_PITCH = 2f

    private val MUSIC_PANTS = "MUSIC_PANTS".toInternalName()

    private val config get() = SkyHanniMod.feature.misc

    private val sounds = setOf(
        "block.note_block.basedrum",
        "block.note_block.bass",
        "block.note_block.harp",
        "block.note_block.hat",
        "block.note_block.snare",
    )

    private val playersWearingStereoPants = TimeLimitedSet<Player>(1.seconds)

    // We cache the players seen wearing Stereo Pants for a second, as well as allow a distance of
    // 5.0 rather than 0.5, to avoid the sounds getting through for a moment when you take them off
    // or you're moving.
    @HandleEvent(onlyOnSkyblock = true)
    fun onPlaySound(event: PlaySoundEvent) {
        if (!config.muteStereoPants) return
        if (event.soundName !in sounds) return
        if (event.pitch !in MIN_PITCH..MAX_PITCH) return
        if (!event.location.toDoubleArray().all(::isCentered)) return

        if (playersWearingStereoPants.isNotEmpty()) {
            event.cancel()
            return
        }

        for (player in EntityUtils.getEntitiesInBoundingBox<Player>(event.location.boundingCenter(5.0))) {
            if (player.getItemBySlot(EquipmentSlot.LEGS).getInternalName() == MUSIC_PANTS) {
                playersWearingStereoPants.add(player)
                event.cancel()
                return
            }
        }
    }

    private fun isCentered(coordinate: Double) = abs(coordinate % 1) == 0.5
}
