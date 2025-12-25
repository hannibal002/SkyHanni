package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MuteBanshee {

    val bansheePitches = listOf(
        0.1904762f,
        0.20634921f,
        0.22222222f,
        0.23809524f,
        0.25396827f,
        0.26984128f,
        0.2857143f,
        0.3015873f,
        0.31746033f,
        0.33333334f,
        0.34920636f,
        0.36507937f,
        0.3809524f,
        0.3968254f,
        0.41269842f,
        0.42857143f,
        0.44444445f,
        0.46031746f,
        0.47619048f,
        0.4920635f
    )

    @HandleEvent(onlyOnIsland = IslandType.BACKWATER_BAYOU)
    fun onSound(event: PlaySoundEvent) {
        if (!SkyHanniMod.feature.fishing.muteBanshee) return
        if (event.pitch in bansheePitches && event.soundName == "entity.ghast.warn") event.cancel()
    }
}
