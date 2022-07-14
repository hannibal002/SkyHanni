package at.hannibal2.skyhanni.dungeon.damageindicator

import at.hannibal2.skyhanni.utils.LorenzColor
import net.minecraft.entity.EntityLivingBase

class EntityData(val entity: EntityLivingBase, val text: String, val color: LorenzColor, val time: Long, val ignoreBlocks: Boolean, val delayedStart: Long)