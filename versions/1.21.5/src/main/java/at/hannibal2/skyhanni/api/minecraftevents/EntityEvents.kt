package at.hannibal2.skyhanni.api.minecraftevents

import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.data.mob.MobDetection
import at.hannibal2.skyhanni.events.entity.EntityHurtEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.entity.Entity
import net.minecraft.entity.damage.DamageSource

@SkyHanniModule
object EntityEvents {

    @JvmStatic
    fun postEntityHurt(entity: Entity, source: DamageSource, amount: Float) {
        EntityHurtEvent(entity, source, amount).post()
        val skyblockMob = MobData.entityToMob[entity] ?: return
        MobDetection.postMobHurtEvent(skyblockMob, source, amount)
    }
}
