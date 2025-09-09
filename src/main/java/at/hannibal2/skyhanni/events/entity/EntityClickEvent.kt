package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.WorldClickEvent
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket

class EntityClickEvent(clickType: ClickType, val action: PlayerInteractEntityC2SPacket.InteractType, val clickedEntity: Entity, itemInHand: ItemStack?) :
    WorldClickEvent(itemInHand, clickType)
