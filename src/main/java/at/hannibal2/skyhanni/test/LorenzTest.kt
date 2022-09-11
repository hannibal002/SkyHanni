package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.play.server.S0EPacketSpawnObject
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.entity.living.EnderTeleportEvent
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class LorenzTest {

    var packetLog = LorenzLogger("debug/packets")

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
    fun onHypExplosions(event: PacketEvent.ReceiveEvent) {
//        val packet = event.packet
//        if (packet !is S2APacketParticles) return
//        if (packet.particleType == EnumParticleTypes.EXPLOSION_LARGE) {
//            event.isCanceled = true
//        }
    }

    @SubscribeEvent
    fun onEnderTeleport(event: EnderTeleportEvent) {
//        event.isCanceled = true
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet

//        if (Minecraft.getMinecraft().thePlayer.isSneaking) {

//        if (packet is S2APacketParticles) {
//            val particleType = packet.particleType
//
//            // FIREWORKS_SPARK ENCHANTMENT_TABLE DRIP_LAVA
//            if (particleType != EnumParticleTypes.FIREWORKS_SPARK) return
//            val loc = packet.toLorenzVec()
//            val distance = loc.distance(LocationUtils.playerLocation())
//            if (distance > 15) return
//            list.add(loc)
//            while (list.size > 3) {
//                list.removeFirst()
//            }
//
//            println("")
//            println("particleType: $particleType")
//
//            val particleCount = packet.particleCount
//
//            println("distance: $distance")
//
//            val particleArgs = packet.particleArgs
//            println("args: " + particleArgs.size)
//            for ((i, particleArg) in particleArgs.withIndex()) {
//                println("$i $particleArg")
//            }
//
//            val particleSpeed = packet.particleSpeed
//            val x = packet.xOffset
//            val y = packet.yOffset
//            val z = packet.zOffset
//            println("particleCount: $particleCount")
//            println("particleSpeed: $particleSpeed")
//            println("xOffset: $x")
//            println("yOffset: $y")
//            println("zOffset: $z")
////            }
//        }


        if (!shouldLogPackets) return


        if (packet is S29PacketSoundEffect) {
            val x = packet.x
            val y = packet.y
            val z = packet.z
            val location = LorenzVec(x, y, z)
            val distance = LocationUtils.playerLocation().distance(location).round(2)
            val soundName = packet.soundName
            val pitch = packet.pitch.toDouble()
            val volume = packet.volume.toDouble()


            //background music
            if (soundName == "note.harp") {
//                if (distance < 2) {


                //Wilderness
                val list = mutableListOf<Double>()
//                list.add(0.4920635)
//                list.add(0.74603176)
//                list.add(0.8888889)
//                list.add(1.1746032)
//                list.add(1.7777778)
//                list.add(0.5873016)
//                list.add(1.0)
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
                val list = mutableListOf<Double>()
                list.add(0.523809552192688)
                list.add(0.5555555820465088)
                list.add(0.60317462682724)
                list.add(0.6349206566810608)
                list.add(0.682539701461792)
                list.add(0.7142857313156128)
                list.add(0.761904776096344)
                list.add(0.7936508059501648)
                list.add(0.841269850730896)
                list.add(0.8888888955116272)
                list.add(0.920634925365448)
                list.add(0.9682539701461792)
                list.add(1.047619104385376)
                list.add(1.047619104385376)
                list.add(0.5079365372657776)
                list.add(0.6507936716079712)
                list.add(0.6984127163887024)
                list.add(0.7460317611694336)
                list.add(0.9365079402923584)
                list.add(0.9841269850730896)
                list.add(1.9682539701461792)
                list.add(0.4920634925365448)
                list.add(1.0158730745315552)
                list.add(1.158730149269104)
                list.add(1.2857142686843872)
                list.add(1.4126983880996704)
                list.add(1.682539701461792)
                list.add(1.8095238208770752)
                list.add(1.9365079402923584)
                list.add(1.4920635223388672)
                list.add(1.5396825075149536)
                list.add(0.8730158805847168)
                list.add(1.2539682388305664)
                list.add(1.4285714626312256)
                list.add(1.6190476417541504)
                list.add(1.4920635223388672)
                list.add(0.9047619104385376)
                list.add(1.1111111640930176)
                list.add(1.317460298538208)
                list.add(1.523809552192688)
                list.add(1.7301586866378784)

                list.add(0.5873016119003296)
                list.add(0.6190476417541504)
                list.add(0.6666666865348816)
                list.add(0.7301587462425232)
                list.add(0.7777777910232544)
                list.add(0.8095238208770752)
                list.add(0.8095238208770752)
                list.add(0.8253968358039856)

                list.add(0.5714285969734192)
                list.add(0.8571428656578064)
                list.add(1.0317460298538208)
                list.add(1.0952380895614624)
                list.add(1.1428571939468384)
                list.add(1.20634925365448)
                list.add(1.2698413133621216)
                list.add(1.0634920597076416)
                list.add(1.2380952835083008)
                list.add(1.79365074634552)
                list.add(1.9841269254684448)
                list.add(1.1746032238006592)
                list.add(1.3492063283920288)
                list.add(1.6984126567840576)
                list.add(1.8571428060531616)

                if (pitch in list) {
                    return
                }
            }

            //use ancestral spade
            if (soundName == "mob.zombie.infect") {
                if (pitch == 1.9682539701461792) {
                    if (volume == 0.30000001192092896) {
                        LorenzUtils.chat("used ancestral spade!")
                        return
                    }
                }
            }

            //wither shield activated
            if (soundName == "mob.zombie.remedy") {
                if (pitch == 0.6984127163887024) {
                    if (volume == 1.0) {
                        LorenzUtils.chat("use wither shield!")
                        return
                    }
                }
            }

            //wither shield cooldown over
            if (soundName == "random.levelup") {
                if (pitch == 3.0) {
                    if (volume == 1.0) {
                        return
                    }
                }
            }

            //teleport (hyp or aote)
            if (soundName == "mob.endermen.portal") {
                if (pitch == 1.0 && volume == 1.0) {
                    return
                }
            }

            //hyp wither impact
            if (soundName == "random.explode") {
                if (pitch == 1.0 && volume == 1.0) {
                    return
                }
            }

            //pick coins up
            if (soundName == "random.orb") {
                if (pitch == 1.4920635223388672 && volume == 1.0) {
                    return
                }
            }



            if (soundName == "game.player.hurt") return

            println("")
            println("S29PacketSoundEffect")
            println("soundName: $soundName")
            println("distance: $distance")
            println("pitch: $pitch")
            println("volume: $volume")


//            println("x: '$x'")
//            println("y: '$y'")
//            println("z: '$z'")

        }


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

//        }


//        packetLog.log(name)

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