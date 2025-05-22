package at.hannibal2.skyhanni.events.pet

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * This event fires when a pet change occurs and when joining SkyBlock for the first time in a session.
 * The XP value in the PetData might not be accurate.
 *
 * @property oldData The previous pet before the change.
 * @property newData The new pet after the change.
 */
@PrimaryFunction("onPetChange")
open class PetChangeEvent(
    open val oldData: PetData?,
    open val newData: PetData?
) : SkyHanniEvent()

/**
 * This fires when a change occurs -to- a pet, rather than a pet change.
 * I.e., if a pet's EXP changes, or if a new item is equipped, this will fire.
 *
 * @property oldData The previous data of the pet before the change.
 * @property newData The new data of the pet after the change.
 */
@PrimaryFunction("onPetDataChange")
class PetDataChangeEvent(
    override val oldData: PetData,
    override val newData: PetData,
) : PetChangeEvent(oldData, newData)
