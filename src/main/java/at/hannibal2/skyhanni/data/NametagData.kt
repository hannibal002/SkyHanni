package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.EntityRenderNametagEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.entity.Entity
import net.minecraft.util.IChatComponent

object NametagData {

    fun getDisplayName(entity: Entity, name: IChatComponent): IChatComponent {
        try {
            if (EntityRenderNametagEvent(entity, name).postAndCatch()) {
                return name
            }
        } catch (e: Throwable) {
            ErrorManager.logErrorWithData(
                e, "Error in entity nametag rendering detected",
                "entity" to entity,
                "name" to name,
            )
        }
        return name
    }

}
