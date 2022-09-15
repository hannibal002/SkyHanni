package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.PlayParticleEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzDebug
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.entity.living.EnderTeleportEvent
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class LorenzTest {

    companion object {
        private var shouldLogPackets = false
        var text = ""

        var a = 127.0
        var b = 60.0
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
            println("shouldLogPackets: $shouldLogPackets")
        }
    }

    @SubscribeEvent
    fun onItemTooltipLow(event: ItemTooltipEvent) {
//        val itemStack = event.itemStack
//        if (itemStack != null) {
//            val internalName = itemStack.getInternalName()
//            event.toolTip.add("internal name: $internalName")
//        }
    }

    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (!SkyHanniMod.feature.dev.debugEnabled) return

        SkyHanniMod.feature.dev.debugPos.renderString(text)
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onHypExplosions(event: PlayParticleEvent) {
//        if (!LorenzUtils.inSkyblock) return
//
//        if (event.type == EnumParticleTypes.EXPLOSION_LARGE) {
//            event.isCanceled = true
//        }
    }

    @SubscribeEvent
    fun onEnderTeleport(event: EnderTeleportEvent) {
//        event.isCanceled = true
    }

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        if (!shouldLogPackets) return

        val packet = event.packet
        val name = packet.javaClass.simpleName

        if (name == "C0FPacketConfirmTransaction") return
        if (name == "C03PacketPlayer") return
        if (name == "C05PacketPlayerLook") return
        if (name == "C00PacketKeepAlive") return

//        println("SendEvent: $name")
    }

    @SubscribeEvent
    fun onSoundEvent(event: PlaySoundEvent) {
        if (!shouldLogPackets) return


        val location = event.location
        val distance = location.distance(LocationUtils.playerLocation())
        val soundName = event.soundName
        val pitch = event.pitch
        val volume = event.volume

        //background music
        if (soundName == "note.harp") {
//                if (distance < 2) {


            //Wilderness
            val list = mutableListOf<Float>()
//                list.add(0.4920635)
//                list.add(0.74603176)
//                list.add(0.8888889)
//                list.add(1.1746032)
//                list.add(1.7777778)
//                list.add(0.5873016)
//                list.add(1f)
//                list.add(1.4920635)
//                list.add(0.4920635)
//                list.add(1.8730159)
//                list.add(0.82539684)
//                list.add(1.1111112)
//                list.add(1.6666666)
//                list.add(0.5555556)
//                list.add(0.6984127)
//                list.add(0.93650794)
//                list.add(1.4126984)
//                list.add(1.3333334)
//                list.add(1.5873016)

            if (pitch in list) {
                if (Minecraft.getMinecraft().thePlayer.isSneaking) {
                    event.isCanceled = true
                }
                return
            }
        }


        //diana ancestral spade
        if (soundName == "note.harp") {
            val list = mutableListOf<Float>()
            list.add(0.52380955f)
            list.add(0.5555556f)
            list.add(0.6031746f)
            list.add(0.63492066f)
            list.add(0.6825397f)
            list.add(0.71428573f)
            list.add(0.7619048f)
            list.add(0.7936508f)
            list.add(0.84126985f)
            list.add(0.8888889f)
            list.add(0.9206349f)
            list.add(0.96825397f)
            list.add(1.476191f)
            list.add(1.476191f)
            list.add(0.50793654f)
            list.add(0.6507937f)
            list.add(0.6984127f)
            list.add(0.74603176f)
            list.add(0.93650794f)
            list.add(0.984127f)
            list.add(1.968254f)
            list.add(0.4920635f)
            list.add(1.1587307f)
            list.add(1.1587301f)
            list.add(1.2857143f)
            list.add(1.4126984f)
            list.add(1.6825397f)
            list.add(1.8095238f)
            list.add(1.9365079f)
            list.add(1.4920635f)
            list.add(1.5396825f)
            list.add(0.8730159f)
            list.add(1.2539682f)
            list.add(1.4285715f)
            list.add(1.6190476f)
            list.add(1.4920635f)
            list.add(0.9047619f)
            list.add(1.1111112f)
            list.add(1.3174603f)
            list.add(1.5238096f)
            list.add(1.7301587f)

            list.add(0.5873016f)
            list.add(0.61904764f)
            list.add(0.6666667f)
            list.add(0.73015875f)
            list.add(0.7777778f)
            list.add(0.8095238f)
            list.add(0.8095238f)
            list.add(0.82539684f)

            list.add(0.5714286f)
            list.add(0.85714287f)
            list.add(1.3174603f)
            list.add(1.9523809f)
            list.add(1.1428572f)
            list.add(1.2063493f)
            list.add(1.2698413f)
            list.add(1.6349206f)
            list.add(1.2380953f)
            list.add(1.7936507f)
            list.add(1.9841269f)
            list.add(1.1746032f)
            list.add(1.3492063f)
            list.add(1.6984127f)
            list.add(1.8571428f)

            if (pitch in list) {
                return
            }
        }

        //use ancestral spade
        if (soundName == "mob.zombie.infect") {
            if (pitch == 1.968254f) {
                if (volume == 0.3f) {
                    LorenzUtils.chat("used ancestral spade!")
                    return
                }
            }
        }

        //wither shield activated
        if (soundName == "mob.zombie.remedy") {
            if (pitch == 0.6984127f) {
                if (volume == 1f) {
                    return
                }
            }
        }

        //wither shield cooldown over
        if (soundName == "random.levelup") {
            if (pitch == 3f) {
                if (volume == 1f) {
                    return
                }
            }
        }

        //teleport (hyp or aote)
        if (soundName == "mob.endermen.portal") {
            if (pitch == 1f && volume == 1f) {
                return
            }
        }

        //hyp wither impact
        if (soundName == "random.explode") {
            if (pitch == 1f && volume == 1f) {
                return
            }
        }

        //pick coins up
        if (soundName == "random.orb") {
            if (pitch == 1.4920635f && volume == 1f) {
                return
            }
        }



        if (soundName == "game.player.hurt") return

//        println("")
//        println("PlaySoundEvent")
//        println("soundName: $soundName")
//        println("distance: $distance")
//        println("pitch: ${pitch}f")
//        println("volume: ${volume}f")
    }

    @SubscribeEvent
    fun onParticleEvent(event: PlayParticleEvent) {
        if (!shouldLogPackets) return

//        val particleType = event.type
//        val distance = LocationUtils.playerLocation().distance(event.location).round(2)
//
//        println("")
//        println("particleType: $particleType")
//
//        val particleCount = event.count
//
//        println("distance: $distance")
//
//        val particleArgs = event.particleArgs
//        println("args: " + particleArgs.size)
//        for ((i, particleArg) in particleArgs.withIndex()) {
//            println("$i $particleArg")
//        }
//
//        val particleSpeed = event.speed
//        val offset = event.offset
//        println("particleCount: $particleCount")
//        println("particleSpeed: $particleSpeed")
//        println("offset: $offset")
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        if (!shouldLogPackets) return

        val packet = event.packet

        val name = packet.javaClass.simpleName
//
//        if (name == "S2APacketParticles") return
//        if (name == "S3BPacketScoreboardObjective") return
//        if (name == "S18PacketEntityTeleport") return
//        if (name == "S38PacketPlayerListItem") return
//        if (name == "S17PacketEntityLookMove") return
//        if (name == "S00PacketKeepAlive") return
//        if (name == "S23PacketBlockChange") return
//        if (name == "S32PacketConfirmTransaction") return
//        if (name == "S47PacketPlayerListHeaderFooter") return
//        if (name == "S29PacketSoundEffect") return
//        if (name == "S04PacketEntityEquipment") return
//        if (name == "S16PacketEntityLook") return
//
//        println(name)


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
//
//        val state = event.state
//
//        if (event.state != null && event.pos != null) {
////            if ((event.pos as BlockPos).y <= 76) {
////            val block = (state as IBlockState).block
////
////            if (block == Blocks.wool || block == Blocks.stained_hardened_clay || block == Blocks.bedrock ||
////                block == Blocks.netherrack || block == Blocks.nether_brick || block == Blocks.coal_block) {
////                event.state = Blocks.stained_hardened_clay.blockState.block.defaultState
////            }
//
////            if (block === Blocks.flowing_lava) {
////                event.state = Blocks.flowing_water.blockState.block.defaultState
////            }
//
////            if (block === Blocks.lava) {
////                event.state = Blocks.water.blockState.block.defaultState
////            }
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