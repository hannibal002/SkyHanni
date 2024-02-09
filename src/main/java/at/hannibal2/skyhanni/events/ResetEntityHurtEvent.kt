package at.hannibal2.skyhanni.events

import net.minecraft.entity.EntityLivingBase
import java.awt.Color

// TODO remove
class ResetEntityHurtEvent(val entity: EntityLivingBase, var shouldReset: Boolean) : LorenzEvent()

fun Color.withAlpha(alpha: Int): Int = (alpha.coerceIn(0, 255) shl 24) or (this.rgb and 0x00ffffff)
