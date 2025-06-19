package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

/**
 * At the moment this event isnt fired on 1.8 but
 * it could be a nice wrapper if someone wanted to impl it
 * On 1.21 its posted in MixinKeyboard
 * On 1.8 we get chars from raw lwjgl functions which dont exist anymore
 */
class CharEvent(val keyCode: Int) : SkyHanniEvent()
