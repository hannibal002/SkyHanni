package at.hannibal2.skyhanni.events.inventory

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeType
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.SafeItemStack

// TODO: support loadouts too?
/**
 * This event is fired when the currently equipped wardrobe is updated.
 * This is for both equipment and armor wardrobes.
 */
@PrimaryFunction("onWardrobeUpdate")
class WardrobeUpdateEvent(
    val type: WardrobeType,
    val items: List<SafeItemStack?>,
) : SkyHanniEvent()
