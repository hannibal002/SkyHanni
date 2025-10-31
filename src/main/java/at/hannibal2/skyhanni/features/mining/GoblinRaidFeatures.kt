package at.hannibal2.hanni.features.mining

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.MobEvent
import at.hannibal2.hanni.hannimodule.HanniModule

@HanniModule
object GoblinRaidFeatures {

    val config get() = HanniMod.feature.mining.miningEvent.goblinRaidConfig

    @HandleEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!config.superprotectronHighlight) return

        if (event.mob.name != "Superprotectron") return

        event.mob.highlight(config.superprotectronHighlightColor)
    }
}
