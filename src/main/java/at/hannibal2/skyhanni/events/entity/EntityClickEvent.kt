package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.events.WorldClickEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.SafeItemStack
import net.minecraft.world.entity.Entity

/**
 * Fired when the player clicks on an entity in the world.
 *
 * @param clickType The type of click (left or right).
 * @param action The interaction action performed on the entity.
 * @param clickedEntity The entity that was clicked.
 * @param itemInHand The item held by the player at the time of the click, or null if empty.
 */
@PrimaryFunction("onEntityClick")
class EntityClickEvent(clickType: InteractClickType, val action: ActionType, val clickedEntity: Entity, itemInHand: SafeItemStack?) :
    WorldClickEvent(itemInHand, clickType) {

    enum class ActionType {
        INTERACT,
        ATTACK,
        INTERACT_AT,
    }
}
