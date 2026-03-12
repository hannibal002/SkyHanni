package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.data.WorldClickType
import at.hannibal2.skyhanni.events.WorldClickEvent
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack

class EntityClickEvent(clickType: WorldClickType, val action: ServerboundInteractPacket.ActionType, val clickedEntity: Entity, itemInHand: ItemStack?) :
    WorldClickEvent(itemInHand, clickType)
