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
import org.lwjgl.input.Keyboard

class BlazeSlayerDaggerHelper {

    var textToRenderMain = ""
    var textToRenderOther = ""
    var clientSideClicked = false

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        val dagger = getDaggerInHand()
        if (dagger != null) {
            textToRenderMain = getDaggerText(dagger)
            textToRenderOther = getDaggerText(dagger.other())
            return
        }

        textToRenderMain = ""
        textToRenderOther = ""
    }

    private fun getDaggerInHand(): Dagger? {
        val player = Minecraft.getMinecraft().thePlayer
        val itemName = getName(player)
        for (dagger in Dagger.values()) {
            if (dagger.daggerNames.any { itemName.contains(it) }) {
                return dagger
            }
        }

        return null
    }

    private fun getDaggerText(dagger: Dagger): String {
        var activeAbility = ""
        var inactiveAbility = ""
        for (shield in dagger.shields) {
            if (shield.active) {
                activeAbility = shield.chatColor + "§l" + shield
            } else {
                inactiveAbility = " §7/ " + shield.chatColor + shield.toString().lowercase()
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
    fun onReceiveCurrentShield(event: PacketEvent.ReceiveEvent) {
        if (!isEnabled()) return

        val packet = event.packet

        if (packet !is S45PacketTitle) return
        val message = packet.message ?: return
        val formattedText = message.formattedText

        for (shield in HellionShield.values()) {
            if (shield.formattedName + "§r" == formattedText) {
                Dagger.values().filter { shield in it.shields }.forEach {
                    it.shields.forEach { shield -> shield.active = false }
                }
                shield.active = true
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
                val dagger = getDaggerInHand()
                dagger?.shields?.forEach { shield -> shield.active = !shield.active }
                clientSideClicked = true
            }
        }
    }

    enum class Dagger(val daggerNames: List<String>, vararg val shields: HellionShield) {
        TWILIGHT(
            listOf("Twilight Dagger", "Mawdredge Dagger", "Deathripper Dagger"),
            HellionShield.SPIRIT,
            HellionShield.CRYSTAL
        ),
        FIREDUST(
            listOf("Firedust Dagger", "Kindlebane Dagger", "Pyrochaos Dagger"),
            HellionShield.ASHEN,
            HellionShield.AURIC
        ),
        ;

        fun other(): Dagger = if (this == TWILIGHT) {
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
        if (Keyboard.isKeyDown(Minecraft.getMinecraft().gameSettings.keyBindPlayerList.keyCode)) return


        val scaledResolution = ScaledResolution(Minecraft.getMinecraft())
        val width = scaledResolution.scaledWidth
        val height = scaledResolution.scaledHeight

        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        val renderer = Minecraft.getMinecraft().fontRendererObj

        GlStateManager.pushMatrix()
        GlStateManager.translate((width / 2).toFloat(), (height / 3.8).toFloat(), 0.0f)
        GlStateManager.scale(4.0f, 4.0f, 4.0f)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(textToRenderMain, renderer, 0f, 0f, false, 55, 0)
        GlStateManager.popMatrix()

        GlStateManager.pushMatrix()
        GlStateManager.translate((width / 2).toFloat(), (height / 3.2).toFloat(), 0.0f)
        GlStateManager.scale(4.0f, 4.0f, 4.0f)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(textToRenderOther, renderer, 0f, 0f, false, 40, 0)
        GlStateManager.popMatrix()
    }
}