package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.GuiRender.renderString
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class LorenzTest {

    var packetLog = LorenzLogger("debug/packets")

    companion object {
        var enabled = false
        var text = ""

        var a = -9.0
        var b = 3.9

        val debugLogger = LorenzLogger("debug/test")

        fun printLore() {
            try {
                val itemStack = Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem()!!
                print("===")
                print("ITEM LORE")
                print("display name: '" + itemStack.displayName.toString() + "'")
                val itemID = itemStack.getInternalName()
                print("internalName: '$itemID'")
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
            } catch (_: Throwable) {
                LorenzUtils.error("Hold an item in the hand to see its item infos!")
            }
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

        fun testCommand() {

        }
    }

    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (!SkyHanniMod.feature.debug.enabled) return

        if (enabled) {
            SkyHanniMod.feature.debug.testPos.renderString(text)
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
//        packetLog.log(event.packet.toString())
    }

//    @SubscribeEvent
//    fun onGetBlockModel(event: RenderBlockInWorldEvent) {
//        if (!LorenzUtils.inSkyblock || !SkyHanniMod.feature.debug.enabled) return
//        val state = event.state
//
//        if (event.state != null && event.pos != null) {
////            if ((event.pos as BlockPos).y <= 76) {
//            val block = (state as IBlockState).block
//
//
//            if (block === Blocks.flowing_lava) {
//                event.state = Blocks.flowing_water.blockState.block.defaultState
//            }
//
//            if (block === Blocks.lava) {
//                event.state = Blocks.water.blockState.block.defaultState
//            }
//
//
//
////            if (block === Blocks.redstone_lamp) {
////                val blockState = Blocks.redstone_lamp.blockState
////                event.state = blockState.block.defaultState
////            }
////                if (block === Blocks.flowing_lava &&
////                    (state as IBlockState).getValue(BlockStainedGlass.COLOR) == EnumDyeColor.WHITE
////                ) {
////                    event.state = state.withProperty(BlockStainedGlass.COLOR, EnumDyeColor.GRAY)
////                }
////                if (block === Blocks.carpet && (state as IBlockState).getValue(BlockCarpet.COLOR) == EnumDyeColor.WHITE) {
////                    event.state = state.withProperty(BlockCarpet.COLOR, EnumDyeColor.GRAY)
////                }
////            }
//        }
//    }
}