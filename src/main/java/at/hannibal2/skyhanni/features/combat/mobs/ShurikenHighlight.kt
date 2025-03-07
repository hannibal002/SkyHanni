package at.hannibal2.skyhanni.features.combat.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColor

@SkyHanniModule
object ShurikenHighlight {
    private val config get() = SkyHanniMod.feature.combat.mobs

    @HandleEvent
    fun onMobHurt(event: MobEvent.Hurt.SkyblockMob) {
        if (!config.shurikenHighlight) return
        DelayedRun.runNextTick { // mob.shurikenUsed isn't updated yet, so wait a tick before checking
            if (config.shurikenHighlight && event.mob.shurikenUsed) {
                event.mob.highlight(config.shurikenHighlightColour.toSpecialColor())
            }
        }
    }

    @HandleEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!config.shurikenHighlight) return
        if (event.mob.shurikenUsed) {
            event.mob.highlight(config.shurikenHighlightColour.toSpecialColor())
        }
    }
}
