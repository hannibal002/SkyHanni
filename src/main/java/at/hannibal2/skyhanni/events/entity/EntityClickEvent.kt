package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.WorldClickEvent
import at.hannibal2.skyhanni.events.entity.abstract.SkyHanniEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack

@PrimaryFunction("onEntityClick")
class EntityClickEvent(
    clickType: ClickType,
    val action: ServerboundInteractPacket.ActionType,
    override val entity: Entity,
    itemInHand: ItemStack?,
) : WorldClickEvent(itemInHand, clickType), SkyHanniEntityEvent<Entity>
