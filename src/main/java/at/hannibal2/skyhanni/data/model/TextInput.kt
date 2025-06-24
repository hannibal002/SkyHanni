package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.insert
import at.hannibal2.skyhanni.utils.StringUtils.removeWordsAtEnd
import kotlinx.coroutines.runBlocking
import net.minecraft.client.settings.KeyBinding
import org.apache.commons.lang3.SystemUtils
import org.lwjgl.input.Keyboard
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

open class TextInput {

    var textBox: String = ""
    private var carriage: Int? = null

    fun editText(textColor: LorenzColor = LorenzColor.WHITE, carriageColor: LorenzColor = LorenzColor.GREEN) = textBox.let {
        with(carriage) {
            if (this == null) it
            else it.insert(this, "${carriageColor.getChatColor()}|${textColor.getChatColor()}")
        }
    }.replace("(?<!§.\\|)§(?!.\\|§.)".toRegex(), "&&")

    fun editTextWithAlwaysCarriage() = textBox.let {
        with(carriage) {
            if (this == null) it.plus('|')
            else it.insert(this, '|')
        }
    }.replace("§", "&&")

    fun finalText() = textBox.replace("&&", "§")

    fun makeActive() = if (!isActive) activate(this) else Unit
    fun disable() = if (isActive) Companion.disable() else Unit
    fun handle() =
        //#if MC < 1.21
        handleTextInput()
    //#else
    //$$ handleTextInput(null)
    //#endif

    fun clear() {
        textBox = ""
        carriage = null
    }

    val isActive get() = Companion.activeInstance == this

    private val updateEvents = mutableMapOf<Int, (TextInput) -> Unit>()

    protected fun update() {
        updateEvents.forEach { (_, it) -> it(this) }
    }

    fun registerToEvent(key: Int, event: (TextInput) -> Unit) {
        updateEvents[key] = event
    }

    fun removeFromEvent(key: Int) {
        updateEvents.remove(key)
    }

    // Skyhanni Module is only for 1.21
    @SkyHanniModule
    companion object {
        private var activeInstance: TextInput? = null

        fun isActive() = activeInstance != null

        fun activate(instance: TextInput) {
            activeInstance = instance
            //#if MC < 1.21
            timeSinceKeyEvent = Keyboard.getEventNanoseconds()
            //#endif
        }

        fun disable() {
            activeInstance = null
        }

        @Suppress("UnusedParameter")
        fun onMinecraftInput(keyBinding: KeyBinding, cir: CallbackInfoReturnable<Boolean>) {
            if (activeInstance != null) {
                cir.returnValue = false
                return
            }
        }

        fun onGuiInput(
            //#if MC < 1.21
            ci: CallbackInfo
            //#else
            //$$ ci: CallbackInfoReturnable<Boolean>
            //#endif
        ) {
            if (activeInstance != null) {
                if (Keyboard.KEY_ESCAPE.isKeyHeld()) {
                    disable()
                } else {
                    //#if MC < 1.21
                    ci.cancel()
                    //#else
                    //$$ ci.setReturnValue(false)
                    //#endif
                }
                return
            }
        }

        private var timeSinceKeyEvent = 0L

        private var carriage
            get() = activeInstance?.carriage
            set(value) {
                activeInstance?.carriage = value
            }

        private var textBox
            get() = activeInstance?.textBox.orEmpty()
            set(value) {
                activeInstance?.textBox = value
            }

        private fun updated() {
            with(activeInstance) {
                if (this == null) return
                update()
            }
        }

        //#if MC > 1.21
        //$$ @at.hannibal2.skyhanni.api.event.HandleEvent
        //$$ fun onChar(event: at.hannibal2.skyhanni.events.minecraft.CharEvent) {
        //$$     handleTextInput(event.keyCode.toChar())
        //$$ }
        //#endif

        private fun handleTextInput(
            //#if MC > 1.21
            //$$ char: Char?,
            //#endif
        ) {
            if (KeyboardManager.isCopyingKeysDown()) {
                OSUtils.copyToClipboard(textBox)
                return
            }
            if (KeyboardManager.isPastingKeysDown()) {
                runBlocking {
                    textBox = OSUtils.readFromClipboard()?.take(2024) ?: return@runBlocking
                    updated()
                }
                return
            }
            val carriage = carriage

            if (Keyboard.KEY_LEFT.isKeyClicked()) {
                this.carriage = carriage?.moveCarriageLeft() ?: (textBox.length - 1)
                return
            }
            if (Keyboard.KEY_RIGHT.isKeyClicked()) {
                this.carriage = when {
                    carriage == null -> null
                    (carriage >= textBox.length - 1) -> null
                    else -> moveCarriageRight(carriage)
                }
                return
            }
            //#if MC > 1.21
            //$$ if (GLFW.GLFW_KEY_BACKSPACE.isKeyClicked() || (SystemUtils.IS_OS_MAC && GLFW.GLFW_KEY_DELETE.isKeyClicked())) {
            //$$     textBox = onRemove()
            //$$     updated()
            //$$     return
            //$$ }
            //#endif

            //#if MC < 1.21
            if (timeSinceKeyEvent == Keyboard.getEventNanoseconds()) return
            timeSinceKeyEvent = Keyboard.getEventNanoseconds()
            val char: Char? = Keyboard.getEventCharacter()
            //#endif
            textBox = when (char) {
                Char(0) -> return
                '\b' -> onRemove()
                Char(127) -> if (SystemUtils.IS_OS_MAC) {
                    onRemove()
                } else {
                    textBox
                }

                null -> textBox

                else -> if (carriage != null) {
                    this.carriage = carriage + 1
                    textBox.insert(carriage, char)
                } else {
                    textBox + char
                }
            }
            updated()
        }

        private fun onRemove(): String = carriage?.let {
            if (it == 0) {
                textBox.substring(1)
            } else {
                this.carriage = it.minus(1)
                textBox.removeRange(it - 1, it)
            }
        } ?: if (KeyboardManager.isModifierKeyDown()) {
            textBox.removeWordsAtEnd(1)
        } else {
            textBox.dropLast(1)
        }

        private fun moveCarriageRight(carriage: Int) = carriage + 1

        private fun Int.moveCarriageLeft(): Int = when {
            this > 0 -> this - 1
            else -> 0
        }
    }
}
