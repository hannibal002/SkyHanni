package at.hannibal2.skyhanni.events.item

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.SafeItemStack

/**
 * When should an item get hidden by marked with a gray background in an inventory and also be prevented to click on it?
 *
 * if [hideReasons] is empty, we do nothing.
 **/
@PrimaryFunction("onItemNotClickable")
class ItemNotClickableEvent(
    val chestName: String,
    val stack: SafeItemStack,
    var hideReasons: MutableList<String> = mutableListOf<String>(),
    var showGreenLine: Boolean = false,
    var allowBypass: Boolean = true,
) : SkyHanniEvent() {

    var hideReason: String = ""
        set(string) {
            hideReasons.add(string)
            field = string
        }
}
