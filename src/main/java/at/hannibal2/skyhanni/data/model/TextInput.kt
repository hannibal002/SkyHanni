package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.insert
import kotlinx.coroutines.runBlocking
import net.minecraft.client.settings.KeyBinding
import org.lwjgl.input.Keyboard
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

class TextInput {

    var textBox: String = ""
    private var carriage: Int? = null

    fun editText() = textBox.let {
        with(carriage) {
            if (this == null) it
            else it.insert(this, '|')
        }
    }.replace("§", "&&")

    fun finalText() = textBox.replace("&&", "§")

    fun makeActive() = Companion.activate(this)
    fun disable() = Companion.disable()
    fun handle() = Companion.handleTextInput()
    fun clear() {
        textBox = ""
        carriage = null
    }

    companion object {
        private var activeInstance: TextInput? = null

        fun activate(instance: TextInput) {
            activeInstance = instance
            timeSinceKeyEvent = Keyboard.getEventNanoseconds()
        }

        fun disable() {
            activeInstance = null
        }

        fun onMinecraftInput(keyBinding: KeyBinding, cir: CallbackInfoReturnable<Boolean>) {
            if (activeInstance != null) {
                cir.returnValue = false
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
            get() = activeInstance?.textBox ?: ""
            set(value) {
                activeInstance?.textBox = value
            }

        private fun handleTextInput() {
            if (KeyboardManager.isCopyingKeysDown()) {
                OSUtils.copyToClipboard(textBox)
                return
            }
            if (KeyboardManager.isPastingKeysDown()) {
                runBlocking {
                    textBox = OSUtils.readFromClipboard() ?: return@runBlocking
                }
                return
            }
            val tcarriage = carriage

            if (Keyboard.KEY_LEFT.isKeyHeld()) {
                carriage = tcarriage?.moveCarriageLeft() ?: (textBox.length - 1)
                return
            }
            if (Keyboard.KEY_RIGHT.isKeyHeld()) {
                carriage = when {
                    tcarriage == null -> null
                    (tcarriage >= textBox.length - 1) -> null
                    else -> moveCarriageRight(tcarriage)
                }
                return
            }
            if (Keyboard.KEY_DELETE.isKeyClicked()) { // Does not work for some reason
                if (tcarriage != null) {
                    textBox.removeRange(tcarriage, tcarriage + 1)
                } else {
                    textBox.dropLast(1)
                }
                return
            }

            if (timeSinceKeyEvent == Keyboard.getEventNanoseconds()) return
            timeSinceKeyEvent = Keyboard.getEventNanoseconds()
            val char = Keyboard.getEventCharacter()
            textBox = when (char) {
                Char(0) -> return
                '\b' -> if (tcarriage != null) {
                    if (tcarriage == 0) {
                        textBox.substring(1)
                    } else {
                        carriage = tcarriage.minus(1)
                        textBox.removeRange(tcarriage - 1, tcarriage)
                    }
                } else {
                    textBox.dropLast(1)
                }

                else -> if (tcarriage != null) {
                    carriage = tcarriage + 1
                    textBox.insert(tcarriage, char)
                } else {
                    textBox + char
                }
            }
        }

        private fun moveCarriageRight(tcarriage: Int) = tcarriage + 1

        private fun Int.moveCarriageLeft(): Int = when {
            this > 0 -> this - 1
            else -> 0
        }
    }
}
