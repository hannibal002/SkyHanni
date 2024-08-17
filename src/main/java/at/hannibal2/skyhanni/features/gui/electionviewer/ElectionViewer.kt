package at.hannibal2.skyhanni.features.gui.electionviewer

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.FakePlayer
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import com.google.gson.JsonParser
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import java.util.Base64

@SkyHanniModule
object ElectionViewer : GuiScreen() {

    private val scaledResolution get() = ScaledResolution(Minecraft.getMinecraft())
    private val windowWidth get() = scaledResolution.scaledWidth
    private val windowHeight get() = scaledResolution.scaledHeight

    private val guiWidth = (windowWidth / (3 / 4f)).toInt()
    private val guiHeight = (windowHeight / (3 / 4f)).toInt()

    var display: Renderable? = null

    val aatroxSkin by lazy {
        getSkinFromMayorName("AATROX")
    }

    private fun getSkinFromMayorName(mayorName: String): String? {
        val base64Texture = "${mayorName}_MAYOR_MONSTER".asInternalName().getItemStack().getSkullTexture()
        val decodedTextureJson = String(Base64.getDecoder().decode(base64Texture), Charsets.UTF_8)
        val decodedJsonObject = JsonParser().parse(decodedTextureJson).asJsonObject
        val textures = decodedJsonObject.getAsJsonObject("textures")
        val skin = textures.getAsJsonObject("SKIN")
        return skin["url"].asString
    }

    @SubscribeEvent
    fun onOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isInGui()) return

        val position = Position(windowWidth / 2 - guiWidth / 2, windowHeight / 2 - guiHeight / 2)

        display?.let {
            position.renderRenderable(
                it,
                posLabel = "Election Viewer",
                addToGuiManager = false,
            )
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isInGui()) return

        display = Renderable.drawInsideRoundedRect(
            Renderable.doubleLayered(
                Renderable.placeholder(guiWidth, guiHeight),
                Renderable.horizontalContainer(
                    listOf(
                        Renderable.clickable(Renderable.string("im stupid"), bypassChecks = true, onClick = { ChatUtils.chat("balls") }),
                        Renderable.fakePlayer(
                            FakePlayer.getFakePlayer(aatroxSkin),
                            followMouse = true,
                        ),
                    ),
                    spacing = 10,
                    verticalAlign = VerticalAlignment.CENTER,
                    horizontalAlign = HorizontalAlignment.CENTER,
                ),
            ),
            Color.BLACK.addAlpha(100),
        )
    }

    fun isInGui() = Minecraft.getMinecraft().currentScreen is ElectionViewer
}
