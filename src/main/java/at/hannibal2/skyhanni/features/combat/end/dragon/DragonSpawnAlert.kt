package at.hannibal2.skyhanni.features.combat.end.dragon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.EndBoss
import at.hannibal2.skyhanni.events.EndBossSpawnEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import kotlin.time.Duration.Companion.seconds

/**
 * Interrupts the screen when a Superior Dragon spawns. It is the one dragon worth dropping
 * everything for, so it gets a title while the others are left to Hypixel's own chat line.
 */
@SkyHanniModule
object DragonSpawnAlert {

    private val config get() = SkyHanniMod.feature.combat.endIsland.dragon

    private val TITLE_DURATION = 1.5.seconds

    @HandleEvent
    private fun onEndBossSpawn(event: EndBossSpawnEvent) {
        if (!config.superiorNotify) return
        if (event.boss != EndBoss.DRAGON) return
        if (DragonFightState.dragonType != DragonType.SUPERIOR) return

        TitleManager.sendTitle("§6Superior Dragon Spawned!", duration = TITLE_DURATION)
    }
}
