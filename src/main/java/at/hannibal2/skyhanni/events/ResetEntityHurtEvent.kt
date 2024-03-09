package at.hannibal2.skyhanni.events

import net.minecraft.entity.EntityLivingBase

@Deprecated("use RenderLivingHelper .setNoHurtTime && .removeNoHurtTime")
class ResetEntityHurtEvent(val entity: EntityLivingBase, var shouldReset: Boolean) : LorenzEvent()

