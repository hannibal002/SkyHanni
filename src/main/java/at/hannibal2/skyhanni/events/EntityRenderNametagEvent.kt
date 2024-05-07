package at.hannibal2.skyhanni.events

import net.minecraft.entity.Entity
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
class EntityRenderNametagEvent(val entity: Entity, var chatComponent: IChatComponent) : LorenzEvent()
