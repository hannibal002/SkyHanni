package at.hannibal2.skyhanni.config.core.elements

import at.hannibal2.skyhanni.config.ConfigEditorKeyMapping
import at.hannibal2.skyhanni.utils.ConfigUtils.asStructuredText
import at.hannibal2.skyhanni.utils.InputKeys
import at.hannibal2.skyhanni.utils.KeyboardManager.fullDisplayName
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
import io.github.notenoughupdates.moulconfig.internal.Warnings
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption

class GuiOptionEditorInputConstantsKey(option: ProcessedOption, defaultKey: InputConstants.Key) : ComponentEditor(option) {
    private var editingKeycode = false
    var component: GuiComponent

    init {
        if (option.type !== InputConstants.Key::class.java)
            Warnings.warn(ConfigEditorKeyMapping::class.java.toString() + " can only be applied to InputCode properties.")

        component = wrapComponent(
            object : GuiComponent() {
                override fun getWidth(): Int {
                    return 0
                }

                override fun getHeight(): Int {
                    return 30
                }

                override fun render(context: GuiImmediateContext) {
                    val height = getHeight()
                    val renderContext = context.renderContext
                    val width = getWidth()

                    renderContext.drawTexturedRect(GuiTextures.BUTTON, (width / 6 - 24).toFloat(), (height - 7 - 14).toFloat(), 48f, 16f)

                    val key = option.get() as InputConstants.Key
                    val keyName = key.fullDisplayName.asStructuredText()
                    val text = if (editingKeycode) StructuredText.of("> ").append(keyName).append(" <") else keyName
                    renderContext.drawStringCenteredScaledMaxWidth(
                        text,
                        IMinecraft.INSTANCE.defaultFontRenderer,
                        (width / 6).toFloat(), (height - 7 - 6).toFloat(),
                        false, 38, -0xcfcfd0,
                    )

                    val resetX = width / 6 - 24 + 48 + 3
                    val resetY = height - 7 - 14 + 3

                    renderContext.drawTexturedRect(GuiTextures.RESET, resetX.toFloat(), resetY.toFloat(), 10f, 11f)
                    val mouseX = context.mouseX
                    val mouseY = context.mouseY
                    if (mouseX >= resetX && mouseX < resetX + 10 && mouseY >= resetY && mouseY < resetY + 11) {
                        renderContext.scheduleDrawTooltip(
                            context.mouseX, context.mouseY,
                            mutableListOf<StructuredText>(StructuredText.of("Reset to Default").red()),
                        )
                    }
                }

                override fun mouseEvent(mouseEvent: MouseEvent, context: GuiImmediateContext): Boolean {
                    if (mouseEvent !is Click) return false
                    if (mouseEvent.mouseState && mouseEvent.mouseButton != InputKeys.KEY_UNKNOWN && editingKeycode) {
                        editingKeycode = false
                        val mouseButton = mouseEvent.mouseButton
                        option.set(InputKeys.mouse(mouseButton))
                        return true
                    }

                    if (mouseEvent.mouseState && mouseEvent.mouseButton == InputConstants.MOUSE_BUTTON_LEFT) {
                        val height = getHeight()
                        val width = getHeight()
                        val mouseX = context.mouseX
                        val mouseY = context.mouseY
                        if (mouseX > width / 6 - 24 && mouseX < width / 6 + 16 && mouseY > height - 7 - 14 && mouseY < height - 7 + 2) {
                            editingKeycode = true
                            return true
                        }
                        if (mouseX > width / 6 - 24 + 48 - 3 && mouseX < width / 6 - 24 + 48 + 13 - 5 &&
                            mouseY > height - 7 - 14 + 3 && mouseY < height - 7 - 14 + 3 + 11
                        ) {
                            option.set(defaultKey)
                            return true
                        }
                    }

                    return false
                }

                override fun keyboardEvent(keyboardEvent: KeyboardEvent, context: GuiImmediateContext): Boolean {
                    if (keyboardEvent is KeyPressed) {
                        if (editingKeycode) {
                            if (keyboardEvent.pressed) return true
                            editingKeycode = false
                            var keycode = keyboardEvent.keycode
                            if (keycode == escape || keycode == 0) {
                                keycode = none
                            }
                            option.set(InputKeys.keyboard(keycode))
                            return true
                        } else {
                            return false
                        }
                    }

                    return editingKeycode
                }
            },
        )
    }

    override fun getDelegate(): GuiComponent {
        return component
    }
}
