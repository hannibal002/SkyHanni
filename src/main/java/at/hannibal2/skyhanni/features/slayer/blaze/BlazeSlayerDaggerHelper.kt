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
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S45PacketTitle
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard

class BlazeSlayerDaggerHelper {

    var clientSideClicked = false
    var textTopLeft = ""
    var textTopRight = ""
    var textBottomLeft = ""
    var textBottomRight = ""

    var lastCheck = 0L

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        val dagger = getDaggerInHand()
        if (dagger != null) {
            setDaggerText(dagger)
            return
        }

        textTopLeft = ""
        textTopRight = ""
        textBottomLeft = ""
        textBottomRight = ""
    }

    private fun setDaggerText(holding: Dagger) {
        checkActiveDagger()

        val first = Dagger.TWILIGHT
        val second = Dagger.FIREDUST

        textTopLeft = format(holding, true, first)
        textTopRight = format(holding, true, second)
        textBottomLeft = format(holding, false, first)
        textBottomRight = format(holding, false, second)
    }

    private fun format(dagger: Dagger, active: Boolean, compareInHand: Dagger): String {
        var daggerInHand = dagger
        val inHand = dagger == compareInHand

        if (!inHand) {
            daggerInHand = daggerInHand.other()
        }

        var shield = daggerInHand.getActive()
        if (!active) {
            shield = shield.other()
        }

        return if (inHand && active) {
            "§7[" + shield.chatColor + shield.cleanName + "§7]"
        } else {
            shield.chatColor + shield.cleanName
        }

    }

    private fun checkActiveDagger() {
        if (lastCheck + 1_000 > System.currentTimeMillis()) return
        lastCheck = System.currentTimeMillis()

        for (dagger in Dagger.values()) {
            val first = dagger.shields[0]
            if (!first.active && !dagger.shields[1].active) {
                first.active = true
            }
        }
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

        if (packet is C08PacketPlayerBlockPlacement) {
            val dagger = getDaggerInHand()
            dagger?.shields?.forEach { shield -> shield.active = !shield.active }
            clientSideClicked = true
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

        fun getActive(): HellionShield {
            for (shield in shields) {
                if (shield.active) {
                    return shield
                }
            }
            throw RuntimeException("no active shield found for dagger $this")
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
        GlStateManager.translate(((width / 2) / 1.18).toFloat(), (height / 3.8).toFloat(), 0.0f)
        GlStateManager.scale(4.0f, 4.0f, 4.0f)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(textTopLeft, renderer, 0f, 0f, false, 60, 0)
        GlStateManager.popMatrix()

        GlStateManager.pushMatrix()
        GlStateManager.translate(((width / 2) * 1.18).toFloat(), (height / 3.8).toFloat(), 0.0f)
        GlStateManager.scale(4.0f, 4.0f, 4.0f)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(textTopRight, renderer, 0f, 0f, false, 60, 0)
        GlStateManager.popMatrix()

        GlStateManager.pushMatrix()
        GlStateManager.translate(((width / 2) / 1.18).toFloat(), (height / 3.0).toFloat(), 0.0f)
        GlStateManager.scale(4.0f, 4.0f, 4.0f)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(textBottomLeft, renderer, 0f, 0f, false, 20, 0)
        GlStateManager.popMatrix()

        GlStateManager.pushMatrix()
        GlStateManager.translate(((width / 2) * 1.18).toFloat(), (height / 3.0).toFloat(), 0.0f)
        GlStateManager.scale(4.0f, 4.0f, 4.0f)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(textBottomRight, renderer, 0f, 0f, false, 20, 0)
        GlStateManager.popMatrix()
    }
}

private fun HellionShield.other(): HellionShield {
    for (dagger in BlazeSlayerDaggerHelper.Dagger.values()) {
        if (this in dagger.shields) {
            for (shield in dagger.shields) {
                if (shield != this) {
                    return shield
                }
            }
        }
    }

    throw RuntimeException("Found no other shield for $this")
}
