package at.hannibal2.skyhanni.events.minecraft

/** Gets posted when a key is released, counterpart to [KeyPressEvent]*/
class KeyReleaseEvent(override val keyCode: Int) : KeyEvent(keyCode)
