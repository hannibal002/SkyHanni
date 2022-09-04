package at.hannibal2.skyhanni.events

import net.minecraft.entity.EntityLivingBase

class RenderMobColoredEvent(val entity: EntityLivingBase, var color: Int) : LorenzEvent()