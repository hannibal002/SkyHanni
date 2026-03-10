package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SpringBootsHelper {

    private val config get() = SkyHanniMod.feature.dungeon

    private val SPRING_BOOTS = "SPRING_BOOTS".toInternalName()

    private const val startSound = "entity.firework_rocket.launch"
    private const val streakSound = "block.note_block.pling"
    private const val endSound = "entity.generic.eat"
    private val springBootsSounds = setOf(startSound, streakSound, endSound)

    /**
     * Two [PlaySoundEvent] get created roughly every 100ms. This tracks the amount of times
     * we heard the `block.note_block.pling` sound while wearing Spring Boots and sneaking.
     */
    private var soundStreak = 0

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onSound(event: PlaySoundEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.getBoots()?.getInternalName() != SPRING_BOOTS) return
        if (event.soundName !in springBootsSounds) return

        if (event.soundName != streakSound) {
            soundStreak = 0
            return
        }
        if (!PlayerUtils.isSneaking()) return
        soundStreak += 1
        if (soundStreak == 5) {
            TitleManager.sendTitle("§cSpring Boots ready!", duration = 2.seconds)
            SoundUtils.playBeepSound()
        }
    }

    private fun shouldShow(): Boolean = DungeonApi.isOneOf("F7", "M7") && DungeonApi.inBossRoom

    private fun isEnabled() = shouldShow() && config.springBootsNotification
}
