package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.events.entity.abstract.SkyHanniLivingEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.entity.LivingEntity

@PrimaryFunction("onEntityMaxHealthUpdate")
class EntityMaxHealthUpdateEvent(
    override val entity: LivingEntity,
    val maxHealth: Int,
) : SkyHanniEvent(), SkyHanniLivingEntityEvent<LivingEntity>
