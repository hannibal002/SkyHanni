package at.hannibal2.skyhanni.features.gui.electionviewer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.Mayor
import at.hannibal2.skyhanni.data.MayorAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.FakePlayer
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import com.google.gson.JsonParser
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import java.util.Base64

@SkyHanniModule
object CurrentMayor : GuiScreen() {

    private val scaledResolution get() = ScaledResolution(Minecraft.getMinecraft())
    private val windowWidth get() = scaledResolution.scaledWidth
    private val windowHeight get() = scaledResolution.scaledHeight

    private val guiWidth = (windowWidth / (3 / 4f)).toInt()
    private val guiHeight = (windowHeight / (3 / 4f)).toInt()

    var display: Renderable? = null

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

        display?.let {
            val position = Position(windowWidth / 2 - guiWidth / 2, windowHeight / 2 - guiHeight / 2)

            position.renderRenderable(
                it,
                posLabel = "Election Viewer - Current Mayor",
                addToGuiManager = false,
            )
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isInGui()) return
        val mayor = MayorAPI.currentMayor ?: return
        val minister = MayorAPI.currentMinister ?: return

        val mainContent = Renderable.verticalContainer(
            listOf(
                Renderable.string("Current Mayor & Minister", horizontalAlign = HorizontalAlignment.CENTER),
                Renderable.string(
                    "Next election in §e${MayorAPI.nextMayorTimestamp.timeUntil().format(showMilliSeconds = false)}",
                    horizontalAlign = HorizontalAlignment.CENTER,
                ),
                Renderable.horizontalContainer(
                    listOf(
                        getMayorRenderable(mayor, "Mayor"),
                        getMayorRenderable(minister, "Minister"),
                    ),
                    spacing = 50,
                ),
                Renderable.clickable(
                    Renderable.string(
                        "§7Go Back",
                        horizontalAlign = HorizontalAlignment.CENTER,
                        verticalAlign = VerticalAlignment.BOTTOM,
                    ).let { Renderable.hoverable(hovered = Renderable.underlined(it), unhovered = it) },
                    onClick = {
                        SkyHanniMod.screenToOpen = ElectionViewer
                    },
                    bypassChecks = true,
                ),
            ),
            spacing = 20,
            verticalAlign = VerticalAlignment.CENTER,
            horizontalAlign = HorizontalAlignment.CENTER,
        )

        display = Renderable.drawInsideRoundedRect(
            Renderable.doubleLayered(
                Renderable.placeholder(guiWidth, guiHeight),
                mainContent,
            ),
            Color.BLACK.addAlpha(180),
        )
    }

    private fun getMayorRenderable(mayor: Mayor, type: String): Renderable {
        val fakePlayer = Renderable.fakePlayer(
            FakePlayer.getFakePlayer(getSkinFromMayorName(mayor.name)),
            followMouse = true,
            entityScale = 50,
        )

        val mayorDescription = getMayorDescription(mayor, type)

        return if (type == "Mayor") {
            Renderable.horizontalContainer(
                listOf(fakePlayer, mayorDescription),
                spacing = 5,
            )
        } else {
            Renderable.horizontalContainer(
                listOf(mayorDescription, fakePlayer),
                spacing = 5,
            )
        }
    }

    private fun getMayorDescription(mayor: Mayor, type: String): Renderable {
        val color = MayorAPI.mayorNameToColorCode(mayor.mayorName)
        return Renderable.verticalContainer(
            buildList {
                add("$color$type ${mayor.mayorName}")
                add("")
                mayor.activePerks.forEach {
                    add(color + it.perkName)
                    add("§7${it.description}")
                    add("")
                }
            }.map { Renderable.wrappedString(it, 150) },
        )
    }

    fun isInGui() = Minecraft.getMinecraft().currentScreen is CurrentMayor
}
