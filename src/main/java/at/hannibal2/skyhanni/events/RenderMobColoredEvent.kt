package at.hannibal2.skyhanni.events

import net.minecraft.entity.EntityLivingBase

// TODO remove
@Deprecated("use RenderLivingHelper .setEntityColor && .removeEntityColor")
class RenderMobColoredEvent(val entity: EntityLivingBase, var color: Int) : LorenzEvent()
