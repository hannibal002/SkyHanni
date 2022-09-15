package at.hannibal2.skyhanni.features.itemabilities.abilitycooldown

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ItemRenderBackground.Companion.background
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class ItemAbilityCooldown {

    var lastAbility = ""
    var tick = 0
    val items = mutableMapOf<ItemStack, ItemText>()

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet
        if (packet is S29PacketSoundEffect) {
            if (packet.soundName == "mob.zombie.remedy") {
                if (packet.pitch == 0.6984127f && packet.volume == 1f) {
                    ItemAbility.HYPERION.sound()
                }
            }
            if (packet.soundName == "mob.enderdragon.growl") {
                if (packet.pitch == 1f && packet.volume == 1f) {
                    ItemAbility.ICE_SPRAY_WAND.sound()
                }
            }
            if (packet.soundName == "mob.endermen.portal") {
                if (packet.pitch == 0.61904764f && packet.volume == 1f) {
                    ItemAbility.GYROKINETIC_WAND.sound()
                }
            }
            if (packet.soundName == "random.anvil_land") {
                if (packet.pitch == 0.4920635f && packet.volume == 1f) {
                    ItemAbility.GIANTS_SWORD.sound()
                }
            }
            if (packet.soundName == "mob.ghast.affectionate_scream") {
                if (packet.pitch == 0.4920635f && packet.volume == 0.15f) {
                    ItemAbility.ATOMSPLIT_KATANA.sound()
                }
            }
        }
    }

    @SubscribeEvent
    fun onItemClickSend(event: PacketEvent.SendEvent) {
        if (!LorenzUtils.inSkyblock) return

        val packet = event.packet
        if (packet is C07PacketPlayerDigging) {
            if (packet.status == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
                val heldItem = Minecraft.getMinecraft().thePlayer.heldItem ?: return
                val internalName = heldItem.getInternalName()
                ItemAbility.getByInternalName(internalName)?.newClick()
//                println("internalName: $internalName")
            }
        }
        if (packet is C08PacketPlayerBlockPlacement) {
            val heldItem = Minecraft.getMinecraft().thePlayer.heldItem ?: return
            val internalName = heldItem.getInternalName()
            ItemAbility.getByInternalName(internalName)?.newClick()
        }
    }

    @SubscribeEvent
    fun onActionBar(event: LorenzActionBarEvent) {
        if (!isEnabled()) return

        val message: String = event.message
        if (message.contains(" (§6")) {
            if (message.contains("§b) ")) {
                val name: String = message.between(" (§6", "§b) ")
                if (name == lastAbility) return
                lastAbility = name
                for (ability in ItemAbility.values()) {
                    if (ability.abilityName == name) {
                        click(ability)
                        return
                    }
                }
                return
            }
        }
        lastAbility = ""
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && SkyHanniMod.feature.itemAbilities.itemAbilityCooldown
    }

    private fun click(ability: ItemAbility) {
        if (ability.actionBarDetection) {
            ability.oldClick()
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        tick++
        if (tick % 2 == 0) {
            checkHotbar()
        }
    }

    private fun checkHotbar() {
        items.clear()
        for ((stack, _) in ItemUtils.getItemsInInventoryWithSlots(true)) {
            val ability = hasAbility(stack)
            if (ability != null) {

                if (ability.isOnCooldown()) {
                    val duration: Long = ability.lastClick + ability.getCooldown() - System.currentTimeMillis()
                    val color = if (duration < 600) LorenzColor.RED else LorenzColor.YELLOW
                    items[stack] = ItemText(color, ability.getDurationText(), true)
                } else {
                    items[stack] = ItemText(LorenzColor.GREEN, "R", false)
                }
            }
        }

    }

    @SubscribeEvent
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.Post) {
        if (!isEnabled()) return

        val item = event.stack ?: return
        if (item.stackSize != 1) return

        var stackTip = ""

        val guiOpen = Minecraft.getMinecraft().currentScreen != null
        val itemText = items.filter { it.key == item }
            .firstNotNullOfOrNull { it.value } ?: return
        if (guiOpen && !itemText.onCooldown) return

        val color = itemText.color
        stackTip = color.getChatColor() + itemText.text

        if (SkyHanniMod.feature.itemAbilities.itemAbilityCooldownBackground) {
            var opacity = 130
            if (color == LorenzColor.GREEN) {
                opacity = 80
            }
            item.background = color.addOpacity(opacity).rgb
        }

        if (stackTip.isNotEmpty()) {
            GlStateManager.disableLighting()
            GlStateManager.disableDepth()
            GlStateManager.disableBlend()
            event.fontRenderer.drawStringWithShadow(
                stackTip,
                (event.x + 17 - event.fontRenderer.getStringWidth(stackTip)).toFloat(),
                (event.y + 9).toFloat(),
                16777215
            )
            GlStateManager.enableLighting()
            GlStateManager.enableDepth()
        }
    }

    private fun hasAbility(stack: ItemStack): ItemAbility? {
        val itemName: String = stack.cleanName()
        val internalName = stack.getInternalName()

        for (ability in ItemAbility.values()) {
            if (ability.newVariant) {
                if (ability.internalNames.contains(internalName)) {
                    return ability
                }
            } else {
                for (name in ability.itemNames) {
                    if (itemName.contains(name)) {
                        return ability
                    }
                }
            }
        }
        return null
    }

    private fun ItemAbility.sound() {
        val lastNewClick = lastNewClick
        val ping = System.currentTimeMillis() - lastNewClick
//        println("$this click ($ping)")
        if (ping < 400) {
            oldClick()
        }
    }

    class ItemText(val color: LorenzColor, val text: String, val onCooldown: Boolean)
}
