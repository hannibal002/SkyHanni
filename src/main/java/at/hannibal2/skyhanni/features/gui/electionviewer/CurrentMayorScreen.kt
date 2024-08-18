package at.hannibal2.skyhanni.features.gui.electionviewer

import at.hannibal2.skyhanni.data.Mayor
import at.hannibal2.skyhanni.data.MayorAPI
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.gui.electionviewer.ElectionViewerUtils.getFakeMayor
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

@SkyHanniModule
object CurrentMayorScreen : ElectionViewerScreen() {

    override val posLabel = "Election Viewer - Current Mayor"

    @SubscribeEvent
    override fun onSecondPassed(event: SecondPassedEvent) {
        if (!isInGui()) return

        val mayor = MayorAPI.currentMayor
        val minister = MayorAPI.currentMinister

        val mainContent = /*if (mayor == null || minister == null) {
            Renderable.verticalContainer(
                listOf(
                    Renderable.string("§cNo/Unknown Mayor & Minister", horizontalAlign = HorizontalAlignment.CENTER),
                    Renderable.verticalContainer(
                        listOf(
                            Renderable.string("Try joining SkyBlock or restarting your game."),
                            Renderable.string("If the issue persists, contact support."),
                        ),
                        spacing = 5,
                        verticalAlign = VerticalAlignment.CENTER,
                        horizontalAlign = HorizontalAlignment.CENTER,
                    ),
                ),
                spacing = 20,
                verticalAlign = VerticalAlignment.CENTER,
                horizontalAlign = HorizontalAlignment.CENTER,
            )
        } else {*/
            Renderable.verticalContainer(
                listOf(
                    Renderable.string("Current Mayor & Minister", horizontalAlign = HorizontalAlignment.CENTER),
                    Renderable.string(
                        "Next election in §e${MayorAPI.nextMayorTimestamp.timeUntil().format(showMilliSeconds = false)}",
                        horizontalAlign = HorizontalAlignment.CENTER,
                    ),
                    Renderable.horizontalContainer(
                        listOf(
                            getMayorRenderable(mayor!!, "Mayor"),
                            getMayorRenderable(minister!!, "Minister"),
                        ),
                        spacing = 50,
                    ),
                ),
                spacing = 20,
                verticalAlign = VerticalAlignment.CENTER,
                horizontalAlign = HorizontalAlignment.CENTER,
            )
        //}

        display = Renderable.drawInsideRoundedRect(
            Renderable.doubleLayered(
                Renderable.placeholder(guiWidth, guiHeight),
                mainContent,
            ),
            Color.BLACK.addAlpha(180),
            padding = PADDING,
        )
    }

    private fun getMayorRenderable(mayor: Mayor, type: String): Renderable {
        val fakePlayer = getFakeMayor(mayor)

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

    override fun isInGui() = Minecraft.getMinecraft().currentScreen is CurrentMayorScreen
}
