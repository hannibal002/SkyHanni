package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ItemBlink
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.SoundUtils
import io.github.moulberry.notenoughupdates.util.MinecraftExecutor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class WildStrawberryDyeNotification {
    var lastCloseTime = 0L

    val item by lazy { "DYE_WILD_STRAWBERRY".asInternalName() }

    @SubscribeEvent
    fun onCloseWindow(event: GuiContainerEvent.CloseWindowEvent) {
        lastCloseTime = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onOwnInventoryItemUpdate(event: OwnInventoryItemUpdateEvent) {
        if (!GardenAPI.inGarden()) return
        if (!SkyHanniMod.feature.garden.wildStrawberryDyeNotification) return

        MinecraftExecutor.OnThread.execute {

            // Prevent false positives when buying the item in ah or moving it from a storage
            val diff = System.currentTimeMillis() - lastCloseTime
            if (diff < 1_000) return@execute

            val internalName = event.itemStack.getInternalName()
            if (internalName == item) {
                val name = event.itemStack.name!!
                LorenzUtils.sendTitle(name, 5.seconds)
                LorenzUtils.chat("§e[SkyHanni] You found a $name§e!")
                SoundUtils.playBeepSound()
                ItemBlink.setBlink(NEUItems.getItemStackOrNull("DYE_WILD_STRAWBERRY"), 5_000)
            }
        }
    }
}