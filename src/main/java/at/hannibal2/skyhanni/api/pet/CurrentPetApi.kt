package at.hannibal2.skyhanni.api.pet

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CurrentPetApi {
    val patternGroup = RepoPattern.Companion.group("misc.pet")

    /**
     * REGEX-TEST: §aYou summoned your §r§dRabbit§r§9 ✦§r§a!
     * REGEX-TEST: §aYou summoned your §r§6Golden Dragon§r§a!
     */
    private val chatSummonPattern by patternGroup.pattern(
        "chat.summon",
        "§aYou summoned your §r§(?<rarity>.)(?<pet>[^§]+)(?:§r(?<skin>§. ✦))?§r§a!"
    )

    var nonUuidPetOverride: PetData? = null
    val currentPet: PetData?
        get() = ProfileStorageData.profileSpecific?.currentPetUuid?.let { currentUuid ->
            ProfileStorageData.petProfiles?.pets?.firstOrNull { it.uuid == currentUuid }
        } ?: nonUuidPetOverride

    fun isCurrentPet(petInternalName: NeuInternalName) = currentPet?.petInternalName == petInternalName
    fun isCurrentPet(petName: String): Boolean = currentPet?.coloredName?.contains(petName) ?: false

    fun assertFoundCurrentData(petData: PetData) {
        if (petData.uuid == null) {
            nonUuidPetOverride = petData
            return
        }
        nonUuidPetOverride = null
        ProfileStorageData.profileSpecific?.currentPetUuid = petData.uuid
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        chatSummonPattern.matchMatcher(event.message) {
            val petName = group("pet")
            val rarity = LorenzRarity.getByColorCode(group("rarity")[0]) ?: return
            val skinTag = groupOrNull("skin")?.replace(" ", "")

            val resolvedPet = PetStorageApi.resolvePetDataOrNull(
                uncoloredPetName = petName,
                rarity = rarity,
                skinTag = skinTag,
            )?.takeIf { it.uuid != null } ?: return

            ProfileStorageData.profileSpecific?.currentPetUuid = resolvedPet.uuid
        }
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("CurrentPetApi")
        if (currentPet == null) {
            event.addIrrelevant("no pet equipped")
            return
        }
        event.addIrrelevant {
            add("petName: '${currentPet?.petInternalName ?: ""}'")
            add("petSkin: '${currentPet?.skinInternalName ?: ""}'")
            add("petRarity: '${currentPet?.rarity?.rawName.orEmpty()}'")
            add("petItem: '${currentPet?.heldItemInternalName ?: ""}'")
            add("petLevel: '${currentPet?.level ?: 0}'")
            add("petXP: '${currentPet?.exp ?: 0.0}'")
            add("petUUID: '${currentPet?.uuid ?: ""}'")
        }
    }
}
