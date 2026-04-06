package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.events.entity.abstract.SkyHanniLivingEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.entity.LivingEntity

@PrimaryFunction("onEntityHealthUpdate")
class EntityHealthUpdateEvent(
    override val entity: LivingEntity,
    val health: Int,
) : GenericSkyHanniEvent<LivingEntity>(entity.javaClass), SkyHanniLivingEntityEvent<LivingEntity>
