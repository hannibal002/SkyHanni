package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.WorldClickEvent
import at.hannibal2.skyhanni.mixins.hooks.MinecraftInputHook
import at.hannibal2.skyhanni.utils.SafeItemStack
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.entity.Entity

class EntityClickEvent(clickType: ClickType, val action: ActionType, val clickedEntity: Entity, itemInHand: SafeItemStack?) :
    WorldClickEvent(itemInHand, clickType) {


    enum class ActionType {
        INTERACT,
        ATTACK,
        INTERACT_AT,
    }
}
