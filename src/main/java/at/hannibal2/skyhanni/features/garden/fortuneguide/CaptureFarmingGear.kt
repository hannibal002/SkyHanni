package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.ToolTooltipTweaks
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorDropStatistics
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.OSUtils
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CaptureFarmingGear {
    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        // it runs when you switch to a non tool as well so be careful
        val resultList = mutableListOf<String>()

        val itemStack = Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem() ?: return
        val itemID = itemStack.getInternalName()
        resultList.add(itemStack.displayName.toString())
        resultList.add(itemID)

        // TODO work with compact tool tips for farming gear
        for (line in itemStack.getLore()) {
            resultList.add(line)
        }

        val string = resultList.joinToString("\n")
        OSUtils.copyToClipboard(string)

        val currentCrop = CropType.values().firstOrNull { itemID.startsWith(it.toolName) }
        if (currentCrop == null) {
            // could save a generic tool here
        } else {
            val hidden = GardenAPI.config?.fortune?.farmingTools ?: return
            hidden[currentCrop.ordinal] = string
        }


        // update armor here

    }


    //save event and populate empty
    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val hidden = GardenAPI.config?.fortune ?: return
        while (hidden.farmingTools.size < 10) {
            hidden.farmingTools.add("")
        }
        //TODO
//        while (hidden.farmingArmor.size < 4) {
//            hidden.farmingArmor.add("")
//        }
//        while (hidden.farmingEquipment.size < 4) {
//            hidden.farmingEquipment.add("")
//        }
//        while (hidden.farmingPets.size < 4) {
//            hidden.farmingPets.add("")
//        }
    }
}