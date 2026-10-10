package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.entity.LivingEntity

@PrimaryFunction("onEntityHealthUpdate")
class EntityHealthUpdateEvent(val entity: LivingEntity, val health: Int) : SkyHanniEvent()
