package at.hannibal2.skyhanni.features.gui.electionviewer

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.MayorAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.gui.electionviewer.ElectionViewerUtils.getFakeCandidate
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

// §6✯

@SkyHanniModule
object CurrentElectionScreen : GuiScreen() {
    private val scaledResolution get() = ScaledResolution(Minecraft.getMinecraft())
    private val windowWidth get() = scaledResolution.scaledWidth
    private val windowHeight get() = scaledResolution.scaledHeight

    private val guiWidth = (windowWidth / (3 / 4f)).toInt()
    private val guiHeight = (windowHeight / (3 / 4f)).toInt()

    private const val PADDING = 10

    private var display: Renderable? = null

    @SubscribeEvent
    fun onOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isInGui()) return

        val position = Position(windowWidth / 2 - guiWidth / 2 - PADDING, windowHeight / 2 - guiHeight / 2 - PADDING)

        display?.let {
            position.renderRenderable(
                it,
                posLabel = "Election Viewer - Current Election",
                addToGuiManager = false,
            )
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isInGui()) return

        val currentElection = MayorAPI.rawMayorData?.current ?: return

        val sortedCandidates = currentElection.candidates.sortedByDescending { it.votes }

        val candidateWithRank = sortedCandidates.mapIndexed { index, candidate ->
            candidate to when (index) {
                0 -> Color.YELLOW
                1 -> Color.LIGHT_GRAY
                else -> null
            }
        }

        val candidateRenderables = currentElection.candidates.map { candidate ->
            val (_, rankColor) = candidateWithRank.first { it.first == candidate }
            val color = MayorAPI.mayorNameToColorCode(candidate.name)

            val candidateContent = Renderable.verticalContainer(
                listOf(
                    Renderable.string(color + candidate.name),
                    Renderable.string(candidate.votes.addSeparators()),
                ),
                spacing = 10,
                verticalAlign = VerticalAlignment.CENTER,
                horizontalAlign = HorizontalAlignment.CENTER,
            )

            val candidateContainer = Renderable.horizontalContainer(
                listOf(
                    getFakeCandidate(candidate),
                    candidateContent,
                ),
            )

            val perksContainer = Renderable.verticalContainer(
                candidate.perks.map {
                    Renderable.hoverTips(
                        Renderable.wrappedString(
                            buildString {
                                if (it.minister) append("§6✯ ")
                                append(color + it.name)
                            },
                            candidateContainer.width,
                            horizontalAlign = HorizontalAlignment.CENTER,
                            internalAlign = HorizontalAlignment.CENTER,
                        ),
                        listOf(Renderable.wrappedString("§7" + it.description, 200)),
                        bypassChecks = true,
                    )
                },
                spacing = 5,
                verticalAlign = VerticalAlignment.CENTER,
                horizontalAlign = HorizontalAlignment.CENTER,
            )

            val fullCandidateContainer = Renderable.verticalContainer(
                listOf(
                    candidateContainer,
                    perksContainer,
                ),
                spacing = 10,
                verticalAlign = VerticalAlignment.TOP,
                horizontalAlign = HorizontalAlignment.CENTER,
            )

            rankColor?.withAlpha(200).let {
                Renderable.drawInsideRoundedRectWithOutline(
                    fullCandidateContainer,
                    Color.BLACK.addAlpha(0),
                    topOutlineColor = it ?: Color.BLACK.withAlpha(0),
                    bottomOutlineColor = it ?: Color.BLACK.withAlpha(0),
                    borderOutlineThickness = 4,
                )
            }
        }

        val mainContent = Renderable.verticalContainer(
            listOf(
                Renderable.string("Current Election", horizontalAlign = HorizontalAlignment.CENTER),
                Renderable.horizontalContainer(candidateRenderables, spacing = 10),
            ),
            verticalAlign = VerticalAlignment.CENTER,
            horizontalAlign = HorizontalAlignment.CENTER,
            spacing = 20,
        )

        display = Renderable.drawInsideRoundedRect(
            Renderable.doubleLayered(
                Renderable.placeholder(guiWidth, guiHeight),
                mainContent,
            ),
            padding = PADDING,
            color = Color.BLACK.addAlpha(180),
        )
    }


    fun isInGui() = Minecraft.getMinecraft().currentScreen is CurrentElectionScreen
}
