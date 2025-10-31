package at.hannibal2.hanni.events.entity

import at.hannibal2.hanni.data.ClickType
import at.hannibal2.hanni.events.WorldClickEvent
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C02PacketUseEntity

class EntityClickEvent(clickType: ClickType, val action: C02PacketUseEntity.Action, val clickedEntity: Entity, itemInHand: ItemStack?) :
    WorldClickEvent(itemInHand, clickType)
