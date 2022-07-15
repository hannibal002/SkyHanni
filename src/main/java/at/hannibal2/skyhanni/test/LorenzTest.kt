package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.GuiRender.renderString
import at.hannibal2.skyhanni.utils.ItemUtil
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class LorenzTest {

    var log = LorenzLogger("debug/packets")

    companion object {
        var enabled = false
        var text = ""

        val debugLogger = LorenzLogger("debug/test")

        fun printLore() {
            val itemStack = Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem()!!
            print("===")
            print("ITEM LORE")
            print("display name: '" + itemStack.displayName.toString() + "'")
            val itemID = ItemUtil.getSkyBlockItemID(itemStack)
            print("itemID: '$itemID'")
//            val rarity: ItemRarityOld = ItemUtils.getRarity(itemStack)
//            print("rarity: '$rarity'")
            print("")
            for (line in itemStack.getLore()) {
                print("'$line'")
                println(line)
            }
            print("")
            print("getTagCompound")
            if (itemStack.hasTagCompound()) {
                val tagCompound = itemStack.tagCompound
                for (s in tagCompound.keySet) {
                    print("  '$s'")
                }
                if (tagCompound.hasKey("ExtraAttributes")) {
                    print("")
                    print("ExtraAttributes")
                    val extraAttributes = tagCompound.getCompoundTag("ExtraAttributes")
//                    for (s in extraAttributes.keySet) {
//                        print("  '$s'")
//                    }
//                    if (extraAttributes.hasKey("enchantments")) {
//                        print("")
//                        print("enchantments")
//                        val enchantments = extraAttributes.getCompoundTag("enchantments")
//                        for (s in enchantments.keySet) {
//                            val level = enchantments.getInteger(s)
//                            print("  '$s' = $level")
//                        }
//                    }
//                    if (extraAttributes.hasKey("modifier")) {
//                        print("")
//                        print("modifier")
//                        val enchantments = extraAttributes.getCompoundTag("modifier")
//                        for (s in enchantments.keySet) {
//                            print("  '$s'")
//                        }
//                    }

                    runn(extraAttributes, "  .  ")
                }
            }
            print("")
            print("===")
            LorenzUtils.debug("item info printed!")
        }

        fun runn(compound: NBTTagCompound, text: String) {
            print("$text'$compound'")
            for (s in compound.keySet) {
                val element = compound.getCompoundTag(s)
                runn(element, "$text  ")
            }
        }

        private fun print(text: String) {
            LorenzDebug.log(text)
        }
    }

    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (!SkyHanniMod.feature.debug.enabled) return

        if (enabled) {
            SkyHanniMod.feature.debug.testPos.renderString(text)
        }
    }
}