package at.hannibal2.skyhanni.events.garden

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.SafeItemStack

/**
 * Fired when the player changes the tool they are holding in their hand.
 *
 * Can be from both networking and render threads.
 */
@PrimaryFunction("onGardenToolChange")
class GardenToolChangeEvent(val crop: CropType?, val toolItem: SafeItemStack?, val toolInHand: String?) : SkyHanniEvent()
