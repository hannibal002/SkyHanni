package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object GoblinRaidFeatures {

    val config get() = SkyHanniMod.feature.mining.miningEvent.goblinRaidConfig

    @HandleEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!config.superprotectronHighlight) return

        if (event.mob.name != "Superprotectron") return

        event.mob.highlight(config.superprotectronHighlightColor.getEffectiveColour())
    }
}
