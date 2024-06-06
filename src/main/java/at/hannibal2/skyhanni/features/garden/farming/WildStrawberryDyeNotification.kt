package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemBlink
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class WildStrawberryDyeNotification {

    private var lastCloseTime = SimpleTimeMark.farPast()

    val item by lazy { "DYE_WILD_STRAWBERRY".asInternalName() }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        lastCloseTime = SimpleTimeMark.now()
    }

    @SubscribeEvent
    fun onOwnInventoryItemUpdate(event: OwnInventoryItemUpdateEvent) {
        if (!GardenAPI.inGarden()) return
        if (!GardenAPI.config.wildStrawberryDyeNotification) return
        // Prevent false positives when buying the item in ah or moving it from a storage
        if (lastCloseTime.passedSince() < 1.seconds) return

        val itemStack = event.itemStack

        val internalName = itemStack.getInternalName()
        if (internalName == item) {
            val name = itemStack.name
            LorenzUtils.sendTitle(name, 5.seconds)
            ChatUtils.chat("You found a $name§e!")
            SoundUtils.playBeepSound()
            ItemBlink.setBlink(itemStack, 5_000)
        }
    }
}
