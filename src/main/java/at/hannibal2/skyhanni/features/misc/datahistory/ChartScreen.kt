import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType
import net.minecraftforge.fml.common.Mod.EventHandler
import org.jfree.chart.JFreeChart
import org.lwjgl.opengl.GL11


class ChartScreen(private val chart: JFreeChart) : GuiScreen() {

    private var chartTexture: DynamicTexture? = null
    private var prevScreenWidth = 0
    private var prevScreenHeight = 0
    private var chartWidth = 0
    private var chartHeight = 0

    override fun initGui() {
        generateChartTexture()
    }

    private fun generateChartTexture() {
        val screenWidth = Minecraft.getMinecraft().displayWidth
        val screenHeight = Minecraft.getMinecraft().displayHeight

        val borderWidth = (0.05 * screenWidth).toInt()
        val borderHeight = (0.05 * screenHeight).toInt()

        chartWidth = screenWidth - 2 * borderWidth
        chartHeight = screenHeight - 2 * borderHeight
//         chartWidth = SkyHanniDebugsAndTests.a.toInt()
//         chartHeight = SkyHanniDebugsAndTests.b.toInt()

        setGuiSize(screenWidth, screenHeight)

        val image = chart.createBufferedImage(chartWidth, chartHeight)

        chartTexture = DynamicTexture(image)
        Minecraft.getMinecraft().textureManager.loadTexture(
            Minecraft.getMinecraft().textureManager.getDynamicTextureLocation("skyhanni_chart", chartTexture),
            chartTexture,
        )
    }

    @EventHandler
    fun onRenderOverlay(event: RenderGameOverlayEvent) {
        if (event.type == ElementType.ALL) {
            chartTexture?.let {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, it.getGlTextureId())

                val xOffset = (event.resolution.scaledWidth - chartWidth) / 2
                val yOffset = (event.resolution.scaledHeight - chartHeight) / 2

//                 drawTexturedModalRect(xOffset, yOffset, 0, 0, chartWidth, chartHeight)
                drawModalRectWithCustomSizedTexture(
                    xOffset, yOffset,
                    0f, 0f,
                    chartWidth, chartHeight,
                    chartWidth.toFloat(), chartHeight.toFloat(),
                )
            }
        }
    }

    // Handle screen resizing
    override fun onResize(mcIn: Minecraft?, w: Int, h: Int) {
        if (w != prevScreenWidth || h != prevScreenHeight) {
            prevScreenWidth = w
            prevScreenHeight = h
            generateChartTexture() // Regenerate texture if dimensions change
        }
    }
}
