package at.lorenz.mod.dungeon.damageindicator

import at.lorenz.mod.utils.LorenzColor
import net.minecraft.entity.EntityLivingBase

class EntityData(val entity: EntityLivingBase, val text: String, val color: LorenzColor, val time: Long, val ignoreBlocks: Boolean, val delayedStart: Long)