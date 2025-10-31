package at.hannibal2.hanni.features.dungeon

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.HanniRenderEntityEvent
import at.hannibal2.hanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.hanni.hannimodule.HanniModule
import net.minecraft.entity.item.EntityArmorStand

@HanniModule
object DungeonBossHideDamageSplash {

    @HandleEvent(priority = HandleEvent.HIGH, onlyOnIsland = IslandType.CATACOMBS)
    fun onRenderLiving(event: HanniRenderEntityEvent.Specials.Pre<EntityArmorStand>) {
        if (!HanniMod.feature.dungeon.damageSplashBoss) return
        if (!DungeonApi.inBossRoom) return

        if (DamageIndicatorManager.isDamageSplash(event.entity)) {
            event.cancel()
        }
    }
}
