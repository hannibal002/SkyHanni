package at.hannibal2.skyhanni.events.fishing

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi
import net.minecraft.world.item.ItemStack

class BaitUpdateEvent(val baitType: FishingApi.BaitType?, val amount: Int, val itemStack: ItemStack) : SkyHanniEvent()
