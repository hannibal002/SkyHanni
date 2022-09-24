package at.hannibal2.skyhanni.features

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.S04PacketEntityEquipment
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.network.play.server.S30PacketWindowItems
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class HideArmor {

    private var invOpen = false
    private val laterCheck = mutableListOf<Int>()

    @SubscribeEvent
    fun onGuiInventoryToggle(event: TickEvent.ClientTickEvent) {
        if (!LorenzUtils.inSkyblock) return // TODO test this

        fixOtherArmor()

        if (!SkyHanniMod.feature.misc.hideArmorEnabled) return

        val currentScreen = Minecraft.getMinecraft().currentScreen
        if (currentScreen == null || currentScreen !is GuiInventory) {
            if (invOpen) {
                invOpen = false
                changeArmor(Minecraft.getMinecraft().thePlayer, null)
            }
        } else {
            if (!invOpen) {
                invOpen = true
                val thePlayer = Minecraft.getMinecraft().thePlayer
                val entityId = thePlayer.entityId
                changeArmor(thePlayer, getCachedArmor(entityId))
            }
        }
    }

    // Since S04PacketEntityEquipment gets sent before the entity is fully loaded, I need to remove the armor later
    private fun fixOtherArmor() {
        for (entity in Minecraft.getMinecraft().theWorld.loadedEntityList) {
            if (entity !is EntityOtherPlayerMP) continue

            val entityId = entity.entityId
            if (entityId !in laterCheck) continue

            laterCheck.remove(entityId)
            if (SkyHanniMod.feature.misc.hideArmorEnabled) {
                val armorInventory = entity.inventory.armorInventory
                for ((equipmentSlot, _) in armorInventory.withIndex()) {
                    if (!SkyHanniMod.feature.misc.hideArmorOnlyHelmet || equipmentSlot == 3) {
                        armorInventory[equipmentSlot] = null
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onPacketReceive(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet

        //own player world switch
        if (packet is S30PacketWindowItems) {

            // check window id
            if (packet.func_148911_c() != 0) return

            for ((slot, itemStack) in packet.itemStacks.withIndex()) {

                if (slot !in 5..8) continue

                val armorSlot = (slot - 5) * -1 + 3
                val armor = getCachedArmor(Minecraft.getMinecraft().thePlayer.entityId)
                armor[armorSlot] = itemStack

                val currentScreen = Minecraft.getMinecraft().currentScreen
                if (currentScreen == null || currentScreen !is GuiInventory) {
                    if (SkyHanniMod.feature.misc.hideArmorEnabled) {
                        if (SkyHanniMod.feature.misc.hideArmorOwn) {
                            if (!SkyHanniMod.feature.misc.hideArmorOnlyHelmet || armorSlot == 3) {
                                packet.itemStacks[slot] = null
                            }
                        }
                    }
                }
            }
            return
        }


        //own player armor change
        if (packet is S2FPacketSetSlot) {
            val slot = packet.func_149173_d()

            // check window id
            if (packet.func_149175_c() != 0) return
            if (slot !in 5..8) return

            val armorSlot = (slot - 5) * -1 + 3
            val armor = getCachedArmor(Minecraft.getMinecraft().thePlayer.entityId)
            // set item in cache
            armor[armorSlot] = packet.func_149174_e()

            val currentScreen = Minecraft.getMinecraft().currentScreen
            if (currentScreen == null || currentScreen !is GuiInventory) {
                if (SkyHanniMod.feature.misc.hideArmorEnabled) {
                    if (SkyHanniMod.feature.misc.hideArmorOwn) {
                        if (!SkyHanniMod.feature.misc.hideArmorOnlyHelmet || armorSlot == 3) {
                            event.isCanceled = true
                        }
                    }
                }
            }
            return
        }


        //other player armor switch
        if (packet is S04PacketEntityEquipment) {
            val entityID = packet.entityID
            val equipmentSlot = packet.equipmentSlot - 1
            if (equipmentSlot == -1) return

            val entity = Minecraft.getMinecraft().theWorld?.getEntityByID(entityID)
            if (entity == null) {
                laterCheck.add(entityID)
                return
            }

            if (entity !is EntityOtherPlayerMP) return

            val armor = getCachedArmor(entityID)

            // set item in cache
            armor[equipmentSlot] = packet.itemStack

            if (SkyHanniMod.feature.misc.hideArmorEnabled) {
                if (!SkyHanniMod.feature.misc.hideArmorOnlyHelmet || equipmentSlot == 3) {
                    event.isCanceled = true
                }
            }
        }
    }

    private fun getCachedArmor(entityID: Int): Array<ItemStack?> {
        val armor: Array<ItemStack?> = if (armorCache.containsKey(entityID)) {
            armorCache[entityID]!!
        } else {
            val new = arrayOf<ItemStack?>(null, null, null, null)
            armorCache[entityID] = new
            new
        }
        return armor
    }

    companion object {
        var armorCache: MutableMap<Int, Array<ItemStack?>> = mutableMapOf()

        fun updateArmor() {
            for (entity in Minecraft.getMinecraft().theWorld.loadedEntityList) {
                if (entity !is EntityPlayer) continue

                val entityId = entity.entityId
                armorCache[entityId]?.let {
                    changeArmor(entity, it)
                }

                if (SkyHanniMod.feature.misc.hideArmorEnabled) {
                    changeArmor(entity, null)
                }
            }
        }

        private fun changeArmor(entity: EntityPlayer, new: Array<ItemStack?>?) {
            if (!LorenzUtils.inSkyblock) return

            val current = entity.inventory.armorInventory
            if (new != null) {
                current[0] = new[0]
                current[1] = new[1]
                current[2] = new[2]
                current[3] = new[3]
                return
            }

            if (!SkyHanniMod.feature.misc.hideArmorOwn) {
                if (entity is EntityPlayerSP) {
                    return
                }
            }

            if (!SkyHanniMod.feature.misc.hideArmorOnlyHelmet) {
                current[0] = null
                current[1] = null
                current[2] = null
            }
            current[3] = null
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        armorCache.clear()
        laterCheck.clear()
    }
}