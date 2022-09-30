package at.hannibal2.skyhanni.events

import net.minecraft.entity.monster.EntityBlaze
import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
class BlazeParticleEvent(val blaze: EntityBlaze): LorenzEvent()