package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.entity.LivingEntity

@PrimaryFunction("onEntityMaxHealthUpdate")
class EntityMaxHealthUpdateEvent(val entity: LivingEntity, val maxHealth: Int) : SkyHanniEvent()
