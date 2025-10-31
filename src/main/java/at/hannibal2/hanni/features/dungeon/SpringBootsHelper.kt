package at.hannibal2.hanni.features.dungeon

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.PlaySoundEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.SoundUtils
import at.hannibal2.hanni.utils.compat.MinecraftCompat
import kotlin.time.Duration.Companion.seconds

@HanniModule
object SpringBootsHelper {

    private val config get() = HanniMod.feature.dungeon

    private val SPRING_BOOTS = "SPRING_BOOTS".toInternalName()

    /**
     * Two [PlaySoundEvent] get created roughly every 100ms. This tracks the amount of times we heard the `note.pling` sound while wearing Spring Boots and sneaking.
     */
    private var soundStreak = 0

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onSound(event: PlaySoundEvent) {
        if (!isEnabled()) return
        if (!(event.soundName == "note.pling" || event.soundName == "fireworks.launch" || event.soundName == "random.eat")) return
        if (InventoryUtils.getBoots()?.getInternalName() != SPRING_BOOTS) return
        if (event.soundName == "fireworks.launch" || event.soundName == "random.eat") {
            soundStreak = 0
            return
        }
        if (!MinecraftCompat.localPlayer.isSneaking) return
        soundStreak += 1

        if (soundStreak == 5) {
            TitleManager.sendTitle("§cSpring Boots ready!", duration = 2.seconds)
            SoundUtils.playBeepSound()
        }
    }

    private fun shouldShow(): Boolean {
        if (!DungeonApi.inBossRoom) return false
        if (!DungeonApi.isOneOf("F7", "M7")) return false

        return true
    }

    private fun isEnabled() = shouldShow() && config.springBootsNotification
}
