package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.events.WorldClickEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.SafeItemStack
import net.minecraft.world.entity.Entity

@PrimaryFunction("onEntityClick")
class EntityClickEvent(clickType: InteractClickType, val action: ActionType, val clickedEntity: Entity, itemInHand: SafeItemStack?) :
    WorldClickEvent(itemInHand, clickType) {

    enum class ActionType {
        INTERACT,
        ATTACK,
        INTERACT_AT,
    }
}
