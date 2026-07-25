package at.hannibal2.skyhanni.api.pet

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import java.util.UUID

@SkyHanniModule
object PetStorageDebug {

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Pet Storage API")
        event.addIrrelevant {
            val petStorage = ProfileStorageData.petProfiles ?: run {
                add("petStorage is null")
                return@addIrrelevant
            }
            val currentPetUuid = ProfileStorageData.profileSpecific?.currentPetUuid
            add("currentPetUuid: $currentPetUuid")
            add("petWidgetState: ${PetStorageApi.debugPetWidgetState}")
            add("recentExactPetMenuClick: ${PetStorageApi.hasRecentExactPetMenuClick()}")
            add("petCount: ${petStorage.pets.size}")
            LorenzRarity.entries.reversed().forEach { rarity ->
                val pets = petStorage.pets.filter { it.rarity == rarity }.takeIfNotEmpty() ?: return@forEach
                add("pets (${rarity.name}):")
                pets.forEach { add(it.formatForDebug(currentPetUuid)) }
                add("")
            }
            add("expSharePets: " + petStorage.expSharePets.joinToString(", ") { it?.toString() ?: "<empty>" })
        }
    }

    private fun PetData.formatForDebug(currentPetUuid: UUID?) = buildString {
        append(if (uuid == currentPetUuid) "* " else "- ")
        append(fauxInternalName.asString())
        append(" lvl=$level")
        append(" rarity=$rarity")
        append(" uuid=$uuid")
        append(" exp=${exp?.toLong() ?: 0L}")
        append(" held=${heldItemInternalName?.asString() ?: "<none>"}")
        append(" skin=${skinInternalName?.asString() ?: "<none>"}")
        append(" skinTag=${skinTag ?: "<none>"}")
        append(" variant=${skinVariantIndex ?: "<none>"}")
    }
}
