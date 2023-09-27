package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName_old
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import net.minecraft.item.Item
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation

object CopyItemCommand {

    fun command(args: Array<String>) {
        try {
            val resultList = mutableListOf<String>()
            val itemStack = InventoryUtils.getItemInHand() ?: return
            resultList.add("ITEM LORE")
            resultList.add("display name: '" + itemStack.displayName.toString() + "'")
            val itemID = itemStack.getInternalName_old()
            resultList.add("internalName: '$itemID'")
            resultList.add("minecraft id: '" + (Item.itemRegistry.getNameForObject(itemStack.item) as ResourceLocation) + "'")
            resultList.add("")
            for (line in itemStack.getLore()) {
                resultList.add("'$line'")
            }
            resultList.add("")
            resultList.add("getTagCompound")
            if (itemStack.hasTagCompound()) {
                val tagCompound = itemStack.tagCompound
                recurseTag(tagCompound, "  ", resultList)
            }

            val string = resultList.joinToString("\n")
            OSUtils.copyToClipboard(string)
            LorenzUtils.chat("§e[SkyHanni] Item info copied into the clipboard!")
        } catch (_: Throwable) {
            LorenzUtils.chat("§c[SkyHanni] No item in hand!")
        }
    }

    private fun recurseTag(compound: NBTTagCompound, text: String, list: MutableList<String>) {
        for (s in compound.keySet) {
            val tag = compound.getTag(s)

            if (tag !is NBTTagCompound) {
                list.add("${text}${s}: $tag")
            } else {
                val element = compound.getCompoundTag(s)
                list.add("${text}${s}:")
                recurseTag(element, "$text  ", list)
            }
        }
    }

}