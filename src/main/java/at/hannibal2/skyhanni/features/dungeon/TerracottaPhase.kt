package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TerracottaPhase {

    private val config get() = SkyHanniMod.feature.dungeon.terracottaPhase

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLiving(event: SkyHanniRenderEntityEvent.Specials.Pre<EntityLivingBase>) {
        if (isActive() && config.hideDamageSplash && DamageIndicatorManager.isDamageSplash(event.entity)) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (isActive() && config.hideParticles) {
            event.isCanceled = true
        }
    }

    private fun isActive() = isEnabled() && DungeonAPI.dungeonPhase == DungeonAPI.DungeonPhase.F6_TERRACOTTA

    private fun isEnabled() =
        DungeonAPI.inDungeon() && DungeonAPI.inBossRoom && DungeonAPI.getCurrentBoss() == DungeonFloor.F6
}
