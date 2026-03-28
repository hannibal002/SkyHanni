package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

@PrimaryFunction("onMinionOpen")
class MinionOpenEvent(
    val inventoryName: String,
    val inventoryItems: Map<Int, ItemStack>,
) : SkyHanniEvent()

@PrimaryFunction("onMinionClose")
class MinionCloseEvent : SkyHanniEvent()

@PrimaryFunction("onMinionStorageOpen")
class MinionStorageOpenEvent(
    val position: Vec3?,
    val inventoryItems: Map<Int, ItemStack>,
) : SkyHanniEvent()
