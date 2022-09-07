package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.play.server.S0EPacketSpawnObject
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class LorenzTest {

    var packetLog = LorenzLogger("debug/packets")

    companion object {
        private var shouldLogPackets = false
        var text = ""

        //        var a = 127.0
        var a = 2.0
        var b = 0.0
        var c = 0.0

        val debugLogger = LorenzLogger("debug/test")

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

        fun testCommand(args: Array<String>) {

            a = args[0].toDouble()
            b = args[1].toDouble()
            c = args[2].toDouble()

//            togglePacketLog = !togglePacketLog

//            for (line in (Minecraft.getMinecraft().ingameGUI.tabList as AccessorGuiPlayerTabOverlay).footer.unformattedText
//                .split("\n")) {
//                println("footer: '$line'")
//            }
//
//
//            for (line in TabListUtils.getTabList()) {
//                println("tablist: '$line'")
//            }
        }

        fun togglePacketLog() {
            shouldLogPackets = !shouldLogPackets
        }
    }

    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (!SkyHanniMod.feature.debug.enabled) return

        SkyHanniMod.feature.debug.testPos.renderString(text)
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet
        val name = packet.javaClass.simpleName
        if (!shouldLogPackets) return

        packetLog.log(name)

//        if (packet is S18PacketEntityTeleport) {
//            val entityId = packet.entityId
//            packetLog.log("entityId: $entityId")
//            val entity = Minecraft.getMinecraft().theWorld.loadedEntityList.find { it.entityId == entityId }
//            val className = entity?.javaClass?.name ?: "null"
//            packetLog.log("className: $className")
//
//            if (Minecraft.getMinecraft().thePlayer.isSneaking) {
//                if (entity is EntityArmorStand) {
//                    event.isCanceled = true
//                }
//            }
//        }


//        if (packet is S0FPacketSpawnMob) {
//            packetLog.log("")
//            packetLog.log("Spawn Mob!")
//            for (watchableObject in packet.func_149027_c()) {
//                val any = watchableObject.`object`
//                val simpleName = any.javaClass.simpleName
//
//                packetLog.log("javaClass: $simpleName")
//                packetLog.log("object: $any")
//                packetLog.log(" ")
//            }
//            packetLog.log(" ")
//        }


//        if (packet is S1CPacketEntityMetadata) {
//            packetLog.log("")
//            packetLog.log("Entity Metadata")
//            for (watchableObject in packet.func_149376_c()) {
//                val any = watchableObject.`object`
//                val simpleName = any.javaClass.simpleName
//
//                packetLog.log("javaClass: $simpleName")
//                packetLog.log("object: $any")
//                packetLog.log(" ")
//            }
//            packetLog.log(" ")
//        }


//        if (packet is S20PacketEntityProperties) {
//            packetLog.log("")
//            packetLog.log("Entity Properties")
//            for (watchableObject in packet.func_149441_d()) {
//                val any = watchableObject.`object`
//                val simpleName = any.javaClass.simpleName
//
//                packetLog.log("javaClass: $simpleName")
//                packetLog.log("object: $any")
//                packetLog.log(" ")
//            }
//            packetLog.log(" ")
//
//
//        }
        if (packet is S0EPacketSpawnObject) {
            packetLog.log("Spawn Object")
        }


//        if (packet is S2CPacketSpawnGlobalEntity) {
//
//        }
//        if (packet is S2CPacketSpawnGlobalEntity) {
//
//        }
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