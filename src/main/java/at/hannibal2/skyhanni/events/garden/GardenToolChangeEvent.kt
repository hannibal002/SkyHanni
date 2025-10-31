package at.hannibal2.hanni.events.garden

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.garden.CropType
import net.minecraft.item.ItemStack

class GardenToolChangeEvent(val crop: CropType?, val toolItem: ItemStack?) : HanniEvent()
