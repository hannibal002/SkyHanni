import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.texture.DynamicTexture
import org.jfree.chart.JFreeChart
import org.lwjgl.opengl.GL11


class ChartScreen(private val chart: JFreeChart) : GuiScreen() {

    private var chartTexture: DynamicTexture? = null
    private var prevScreenWidth = 0
    private var prevScreenHeight = 0

    private var chartWidth = 0
    private var chartHeight = 0

    private var xOffset = 0
    private var yOffset = 0

    override fun initGui() {
        generateChartTexture()
    }

    override fun onResize(mcIn: Minecraft?, w: Int, h: Int) {
        if (w != prevScreenWidth || h != prevScreenHeight) {
            prevScreenWidth = w
            prevScreenHeight = h
            generateChartTexture()
        }
    }

    private fun generateChartTexture() {
        val scaledResolution = ScaledResolution(Minecraft.getMinecraft())
        val screenWidth = scaledResolution.scaledWidth
        val screenHeight = scaledResolution.scaledHeight

        val borderWidth = (0.05 * screenWidth).toInt()
        val borderHeight = (0.05 * screenHeight).toInt()

        chartWidth = screenWidth - 2 * borderWidth
        chartHeight = screenHeight - 2 * borderHeight

        setGuiSize(screenWidth, screenHeight)

        val image = chart.createBufferedImage(Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight)

        chartTexture = DynamicTexture(image)
        Minecraft.getMinecraft().textureManager.loadTexture(
            Minecraft.getMinecraft().textureManager.getDynamicTextureLocation("skyhanni_chart", chartTexture),
            chartTexture,
        )

        xOffset = borderWidth
        yOffset = borderHeight
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)

        val chartTexture = chartTexture ?: return

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, chartTexture.getGlTextureId())

        drawModalRectWithCustomSizedTexture(
            xOffset, yOffset,
            0f, 0f,
            chartWidth, chartHeight,
            chartWidth.toFloat(), chartHeight.toFloat(),
        )
    }
}
