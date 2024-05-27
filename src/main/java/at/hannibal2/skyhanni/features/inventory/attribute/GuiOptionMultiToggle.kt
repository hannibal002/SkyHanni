package at.hannibal2.skyhanni.features.inventory.attribute

import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import java.lang.reflect.ParameterizedType

class GuiOptionMultiToggle(option: ProcessedOption) : GuiOptionEditor(option) {

    private var exampleText = mutableMapOf<Any, String>()

    private var activeElements: Map<Any, Any>? = null
    private var enumConstants: Array<Enum<*>>? = null

    fun GuiOptionMultiToggle(
        option: ProcessedOption,
        exampleText: Array<String?>,
    ) {
        this.activeElements = option.get() as Map<Any, Any>

        val elementType = (option.type as ParameterizedType).actualTypeArguments[0]

        if (Enum::class.java.isAssignableFrom(elementType as Class<*>)) {
            val enumType = (option.type as ParameterizedType).actualTypeArguments[0] as Class<Enum<*>?>
            enumConstants = ((enumType.enumConstants) as Array<Enum<*>>? ?: error("No enum constants for $enumType"))
                .also {
                    for (i in it.indices) {
                        this.exampleText[it[i]] = it[i].toString()
                    }
                }
        } else {
            for (i in exampleText.indices) {
                this.exampleText[i] = exampleText[i] ?: ""
            }
        }
    }

    private fun getExampleText(forObject: Any): String {
        return exampleText[forObject] ?: error("No example text for $forObject")
    }

    private fun saveChanges() = option.explicitNotifyChange()

    override fun getHeight(): Int {
        var height = super.getHeight() + 13

        for (text in exampleText) {
            val a = text
            val str = getExampleText(text)
            height += 10 * str.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size
        }

        exampleText.entries.toList().chunked(2).forEach { line ->
            height += line.maxOfOrNull { element ->
                val str = getExampleText(element)
                10 * str.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size
            } ?: 0
        }

        return height
    }

    override fun render(x: Int, y: Int, width: Int) {
        super.render(x, y, width)
        val height = getHeight()

        val fontRenderer = Minecraft.getMinecraft().fontRendererObj

        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat() + 10, y.toFloat(), 1F)
    }

    override fun mouseInput(x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun keyboardInput() = false
}
