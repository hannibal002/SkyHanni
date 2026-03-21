package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.WorldClickEvent
import at.hannibal2.skyhanni.mixins.hooks.MinecraftInputHook
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack

class EntityClickEvent(clickType: ClickType, val action: ActionType, val clickedEntity: Entity, itemInHand: ItemStack?) :
    WorldClickEvent(itemInHand, clickType) {


    enum class ActionType {
        INTERACT,
        ATTACK,
        INTERACT_AT,
    }
}
