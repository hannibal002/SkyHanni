package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonBossHideDamageSplash {

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLiving(event: RenderLivingEvent.Specials.Pre<EntityLivingBase>) {
        if (!SkyHanniMod.feature.dungeon.damageSplashBoss) return
        if (!DungeonData.inBossRoom) return

        val entity = event.entity
        if (entity.ticksExisted > 300 || entity !is EntityArmorStand) return
        if (!entity.hasCustomName()) return
        if (entity.isDead) return
        val name = entity.customNameTag.removeColor().replace(",", "")
        if (!DamageIndicatorManager.damagePattern.matcher(name).matches()) return

        event.isCanceled = true
    }
}