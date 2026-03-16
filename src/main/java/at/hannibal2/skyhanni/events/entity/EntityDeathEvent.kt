package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.events.entity.abstract.SkyHanniEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.entity.Entity

@PrimaryFunction("onEntityDeath")
open class EntityDeathEvent<T : Entity>(
    override val entity: T,
) : GenericSkyHanniEvent<T>(entity.javaClass), SkyHanniEntityEvent<T>
