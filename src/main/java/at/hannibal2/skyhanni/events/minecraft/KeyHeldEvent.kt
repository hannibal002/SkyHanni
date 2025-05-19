package at.hannibal2.skyhanni.events.minecraft

/** Gets posted repeatedly while a key is held down, also on initial key press, use this for holding keys*/
class KeyHeldEvent(override val keyCode: Int) : KeyEvent(keyCode)
