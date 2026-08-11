package at.hannibal2.skyhanni.config.core.elements

import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import com.mojang.blaze3d.platform.InputConstants
import io.github.notenoughupdates.moulconfig.GuiTextures
import io.github.notenoughupdates.moulconfig.common.IMinecraft
import io.github.notenoughupdates.moulconfig.common.KeyboardConstants.escape
import io.github.notenoughupdates.moulconfig.common.KeyboardConstants.none
import io.github.notenoughupdates.moulconfig.common.text.StructuredText
import io.github.notenoughupdates.moulconfig.gui.GuiComponent
import io.github.notenoughupdates.moulconfig.gui.GuiImmediateContext
import io.github.notenoughupdates.moulconfig.gui.KeyboardEvent
import io.github.notenoughupdates.moulconfig.gui.KeyboardEvent.KeyPressed
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import io.github.notenoughupdates.moulconfig.gui.MouseEvent.Click
import io.github.notenoughupdates.moulconfig.gui.editors.ComponentEditor
import io.github.notenoughupdates.moulconfig.platform.MoulConfigPlatform
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

// Refrence: https://github.com/AzureAaron/Dandelion/blob/master/src/main/java/net/azureaaron/dandelion/impl/moulconfig/editor/DandelionKeyMappingEditor.java
class GuiOptionEditorKeyMapping(option: ProcessedOption, keyMapping: KeyMapping) : ComponentEditor(option) {
    private val component: GuiComponent = this.wrapComponent(KeyMappingComponent(keyMapping))

    override fun getDelegate(): GuiComponent {
        return this.component
    }

    private class KeyMappingComponent(private val keyMapping: KeyMapping) : GuiComponent() {
        private var editingKeycode = false

        override fun getWidth(): Int {
            return 0
        }

        override fun getHeight(): Int {
            return 30
        }

        override fun render(context: GuiImmediateContext) {
            val renderContext = context.renderContext
            val width = this.width
            val height = this.height

            renderContext.drawTexturedRect(GuiTextures.BUTTON, (width / 6 - 24).toFloat(), (height - 7 - 14).toFloat(), 48f, 16f)

            val keyName = MoulConfigPlatform.wrap(this.keyMapping.translatedKeyMessage)
            val text = if (this.editingKeycode) StructuredText.of("> ").append(keyName).append(" <") else keyName
            renderContext.drawStringCenteredScaledMaxWidth(
                text,
                IMinecraft.INSTANCE.defaultFontRenderer,
                (width / 6).toFloat(),
                (height - 7 - 6).toFloat(),
                false,
                38,
                -0xcfcfd0,
            )

            val resetX = width / 6 - 24 + 48 + 3
            val resetY = height - 7 - 14 + 3
            renderContext.drawTexturedRect(GuiTextures.RESET, resetX.toFloat(), resetY.toFloat(), 10f, 11f)

            val mouseX = context.mouseX
            val mouseY = context.mouseY

            if (mouseX >= resetX && mouseX < resetX + 10 && mouseY >= resetY && mouseY < resetY + 11) {
                val tooltip = listOf<StructuredText>(
                    MoulConfigPlatform.wrap("Reset to Default".asComponent()).red(),
                )
                renderContext.scheduleDrawTooltip(mouseX, mouseY, tooltip)
            }
        }

        override fun mouseEvent(mouseEvent: MouseEvent, context: GuiImmediateContext): Boolean {
            if (mouseEvent is Click) {
                if (mouseEvent.mouseState && mouseEvent.mouseButton != -1 && this.editingKeycode) {
                    this.editingKeycode = false
                    val mouseButton = mouseEvent.mouseButton
                    this.setKey(InputConstants.Type.MOUSE.getOrCreate(mouseButton))
                    return true
                }

                if (mouseEvent.mouseState && mouseEvent.mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    val height = getHeight()
                    val width = getHeight()
                    val mouseX = context.mouseX
                    val mouseY = context.mouseY

                    if (mouseX > width / 6 - 24 && mouseX < width / 6 + 16 && mouseY > height - 7 - 14 && mouseY < height - 7 + 2) {
                        this.editingKeycode = true
                        return true
                    }

                    if (mouseX > width / 6 - 24 + 48 - 3 && mouseX < width / 6 - 24 + 48 + 13 - 5 && mouseY > height - 7 - 14 + 3 && mouseY < height - 7 - 14 + 3 + 11) {
                        this.setKey(this.keyMapping.defaultKey)
                        return true
                    }
                }
            }

            return false
        }

        override fun keyboardEvent(keyboardEvent: KeyboardEvent, context: GuiImmediateContext): Boolean {
            if (keyboardEvent is KeyPressed) {
                if (this.editingKeycode) {
                    if (keyboardEvent.pressed) {
                        return true
                    }

                    this.editingKeycode = false
                    var keycode = keyboardEvent.keycode

                    if (keycode == escape || keycode == 0) {
                        keycode = none
                    }

                    this.setKey(InputConstants.Type.KEYSYM.getOrCreate(keycode))
                    return true
                } else {
                    return false
                }
            }

            return this.editingKeycode
        }

        /** Updates the [.keyMapping] to be the `key` and refreshes all key mappings. */
        private fun setKey(key: InputConstants.Key) {
            this.keyMapping.setKey(key)
            KeyMapping.resetMapping()
        }
    }
}
