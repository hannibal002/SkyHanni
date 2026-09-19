package at.hannibal2.skyhanni.api.pet

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.PetDataStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands

@SkyHanniModule
object PetStorageCommands {

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetpetstorage") {
            description = "Removes all pets from SkyHanni's storage"
            category = CommandCategory.USERS_RESET
            simpleCallback {
                ProfileStorageData.petProfiles = PetDataStorage.ProfileSpecific()
                ChatUtils.clickableChat(
                    "Cleared all pets from storage. Re-open the §b/pet §emenu to re-populate it.",
                    onClick = { HypixelCommands.pet() },
                    hover = "Click to re-open the pet menu",
                )
            }
        }
    }
}
