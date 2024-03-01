package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzRenderLivingEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonBossHideDamageSplash {

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLiving(event: LorenzRenderLivingEvent.Specials.Pre<EntityLivingBase>) {
        if (!LorenzUtils.inDungeons) return
        if (!SkyHanniMod.feature.dungeon.damageSplashBoss) return
        if (!DungeonAPI.inBossRoom) return

        if (DamageIndicatorManager.isDamageSplash(event.entity)) {
            event.isCanceled = true
        }
    }
}
