package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.OSUtils
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CaptureFarmingGear {
    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        // it runs when you switch to a non tool as well so be careful
        val resultList = mutableListOf<String>()

        val itemStack = Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem()
        val itemID = itemStack.getInternalName()
        resultList.add(itemStack.displayName.toString())
        resultList.add(itemID)

        // TODO work with compact tool tips for farming gear
        CropPageMath.texttt.clear()
        for (line in itemStack.getLore()) {
            CropPageMath.texttt.add(line)
        }

        val string = resultList.joinToString("\n")
        OSUtils.copyToClipboard(string)

        val currentCrop = CropType.values().firstOrNull { itemID.startsWith(it.toolName) }

        if (currentCrop == null) {

        } else {
            val a = currentCrop.ordinal

        }


        println(currentCrop)

//            ?:
//            // idk if this elvis operator will work
//            //test to see if its a universal tool, eg advanced, basic or rookie
//            return









        // update armor here

    }
}