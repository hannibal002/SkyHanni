package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CaptureFarmingGear {



    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        //update tools here
        // it runs when you switch to a non tool as well so be careful
        val resultList = mutableListOf<String>()

        val itemStack = Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem()
        val itemID = itemStack.getInternalName()
        resultList.add(itemStack.displayName.toString())
        resultList.add(itemID)

        for (line in itemStack.getLore()) {
            resultList.add("'$line'")
        }
        val currentCrop = CropType.values().firstOrNull { itemID.startsWith(it.toolName) }
            ?:
            // idk if this elvis operator will work
            //test to see if its a universal tool, eg advanced, basic or rookie
            return





        // update armor here

    }
}