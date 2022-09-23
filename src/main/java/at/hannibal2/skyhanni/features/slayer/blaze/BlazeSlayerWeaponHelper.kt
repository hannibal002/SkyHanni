package at.hannibal2.skyhanni.features.slayer.blaze

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.util.render.TextRenderUtils
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.server.S45PacketTitle
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class BlazeSlayerWeaponHelper {

    var textToRenderA = ""
    var textToRenderB = ""
    var clientSideClicked = false

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        val sword = getSwordInHand()
        if (sword != null) {
            textToRenderA = getSwordText(sword)
            textToRenderB = getSwordText(sword.other())
            return
        }

        textToRenderA = ""
        textToRenderB = ""
    }

    private fun getSwordInHand(): Sword? {
        val player = Minecraft.getMinecraft().thePlayer
        val itemName = getName(player)
        for (sword in Sword.values()) {
            if (itemName.contains(sword.swordName)) {
                return sword
            }
        }

        return null
    }

    private fun getSwordText(sword: Sword): String {
        var activeAbility = ""
        var inactiveAbility = ""
        for (mode in sword.modes) {
            if (mode.active) {
                activeAbility = mode.chatColor + "§l" + mode
            } else {
                inactiveAbility = " §7/ " + mode.chatColor + mode.toString().lowercase()
            }
        }
        if (activeAbility == "") return ""
        return "$activeAbility$inactiveAbility"

    }

    private fun getName(player: EntityPlayerSP): String {
        val itemStack = player.inventory.mainInventory[player.inventory.currentItem] ?: return ""
        return itemStack.name ?: ""
    }

    @SubscribeEvent
    fun onReceiveCurrentMode(event: PacketEvent.ReceiveEvent) {
        if (!isEnabled()) return

        val packet = event.packet

        if (packet !is S45PacketTitle) return
        val message = packet.message ?: return
        val formattedText = message.formattedText

        for (swordMode in SwordMode.values()) {
            if (swordMode.formattedName + "§r" == formattedText) {
                Sword.values().filter { swordMode in it.modes }.forEach {
                    it.modes.forEach { mode -> mode.active = false }
                }
                swordMode.active = true
                event.isCanceled = true
                clientSideClicked = false
                return
            }
        }
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && SkyHanniMod.feature.slayer.blazeDaggers
    }

    @SubscribeEvent
    fun onRightClick(event: PacketEvent.SendEvent) {
        if (!isEnabled()) return

        if (clientSideClicked) return

        val packet = event.packet

        if (packet is C07PacketPlayerDigging) {
            val status = packet.status
            if (status == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                val sword = getSwordInHand()
                sword?.modes?.forEach { mode -> mode.active = !mode.active }
                clientSideClicked = true
            }
        }
    }

    enum class Sword(val swordName: String, vararg val modes: SwordMode) {
        TWILIGHT("Twilight Dagger", SwordMode.SPIRIT, SwordMode.CRYSTAL),
        FIREDUST("Firedust Dagger", SwordMode.ASHEN, SwordMode.AURIC),
        ;

        fun other(): Sword = if (this == TWILIGHT) {
            FIREDUST
        } else {
            TWILIGHT
        }
    }

    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        if (!isEnabled()) return

        if (Minecraft.getMinecraft().currentScreen != null) return

        val scaledResolution = ScaledResolution(Minecraft.getMinecraft())
        val width = scaledResolution.scaledWidth
        val height = scaledResolution.scaledHeight

        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        val renderer = Minecraft.getMinecraft().fontRendererObj

        GlStateManager.pushMatrix()
        GlStateManager.translate((width / 2).toFloat(), (height / 3.8).toFloat(), 0.0f)
        GlStateManager.scale(4.0f, 4.0f, 4.0f)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(textToRenderA, renderer, 0f, 0f, false, 55, 0)
        GlStateManager.popMatrix()

        GlStateManager.pushMatrix()
        GlStateManager.translate((width / 2).toFloat(), (height / 3.2).toFloat(), 0.0f)
        GlStateManager.scale(4.0f, 4.0f, 4.0f)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(textToRenderB, renderer, 0f, 0f, false, 40, 0)
        GlStateManager.popMatrix()
    }
}