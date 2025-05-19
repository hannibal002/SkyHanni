package at.hannibal2.skyhanni.events.minecraft

/** Gets posted when a key is first pressed, use this for taps*/
class KeyPressEvent(override val keyCode: Int) : KeyEvent(keyCode)
