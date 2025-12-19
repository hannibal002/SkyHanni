package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.WorldClickEvent
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.network.protocol.game.ServerboundInteractPacket

class EntityClickEvent(clickType: ClickType, val action: ServerboundInteractPacket.ActionType, val clickedEntity: Entity, itemInHand: ItemStack?) :
    WorldClickEvent(itemInHand, clickType)
