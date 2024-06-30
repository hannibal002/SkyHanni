package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
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
            if (Keyboard.KEY_DELETE.isKeyClicked()) { // Does not work for some reason
                if (carriage != null) {
                    textBox.removeRange(carriage, carriage + 1)
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
                '\b' -> if (carriage != null) {
                    if (carriage == 0) {
                        textBox.substring(1)
                    } else {
                        this.carriage = carriage.minus(1)
                        textBox.removeRange(carriage - 1, carriage)
                    }
                } else {
                    textBox.dropLast(1)
                }

                else -> if (carriage != null) {
                    this.carriage = carriage + 1
                    textBox.insert(carriage, char)
                } else {
                    textBox + char
                }
            }
        }

        private fun moveCarriageRight(carriage: Int) = carriage + 1

        private fun Int.moveCarriageLeft(): Int = when {
            this > 0 -> this - 1
            else -> 0
        }
    }
}
