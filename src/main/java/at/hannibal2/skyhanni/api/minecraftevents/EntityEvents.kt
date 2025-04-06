package at.hannibal2.skyhanni.api.minecraftevents

import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.data.mob.MobDetection
import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent
import at.hannibal2.skyhanni.events.entity.EndermanTeleportEvent
import at.hannibal2.skyhanni.events.entity.EntityDeathEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.events.entity.EntityHurtEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.event.entity.living.EnderTeleportEvent
import net.minecraftforge.event.entity.living.LivingAttackEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object EntityEvents {

    @SubscribeEvent
    fun onEntityJoinWorld(event: EntityJoinWorldEvent) {
        EntityEnterWorldEvent(event.entity).post()
    }

    @SubscribeEvent
    fun onEntityHurt(event: LivingAttackEvent) {
        val entity = event.entity
        val source = event.source
        val amount = event.ammount
        EntityHurtEvent(entity, source, amount).post()
        val skyblockMob = MobData.entityToMob[entity] ?: return
        MobDetection.postMobHurtEvent(skyblockMob, source, amount)
    }

    @SubscribeEvent
    fun onEnderTeleport(event: EnderTeleportEvent) {
        if (EndermanTeleportEvent().post()) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onLivingDeath(event: LivingDeathEvent) {
        EntityDeathEvent(event.entity).post()
    }

    @SubscribeEvent
    fun onEntityRenderPre(event: RenderLivingEvent.Pre<*>) {
        if (SkyHanniRenderEntityEvent.Pre(event.entity, event.renderer, event.x, event.y, event.z).post()) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onEntityRenderPost(event: RenderLivingEvent.Post<*>) {
        SkyHanniRenderEntityEvent.Post(event.entity, event.renderer, event.x, event.y, event.z).post()
    }

    @SubscribeEvent
    fun onEntityRenderSpecialsPre(event: RenderLivingEvent.Specials.Pre<*>) {
        if (SkyHanniRenderEntityEvent.Specials.Pre(event.entity, event.renderer, event.x, event.y, event.z).post()) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onEntityRenderSpecialsPost(event: RenderLivingEvent.Specials.Post<*>) {
        SkyHanniRenderEntityEvent.Specials.Post(event.entity, event.renderer, event.x, event.y, event.z).post()
    }
}
