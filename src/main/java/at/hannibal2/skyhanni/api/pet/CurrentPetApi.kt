package at.hannibal2.skyhanni.api.pet

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CurrentPetApi {
    val patternGroup = RepoPattern.group("misc.pet")

    /**
     * REGEX-TEST: §aYou summoned your §r§dRabbit§r§9 ✦§r§a!
     * REGEX-TEST: §aYou summoned your §r§6Golden Dragon§r§a!
     */
    private val chatSummonPattern by patternGroup.pattern(
        "chat.summon",
        "§aYou summoned your §r§(?<rarity>.)(?<pet>[^§]+)(?:§r(?<skin>§. ✦))?§r§a!"
    )

    val currentPet: PetData?
        get() = ProfileStorageData.profileSpecific?.currentPetUuid?.let { currentUuid ->
            ProfileStorageData.petProfiles?.pets?.firstOrNull { it.uuid == currentUuid }
        }

    fun isCurrentPet(petInternalName: NeuInternalName) = currentPet?.petInternalName == petInternalName
    fun isCurrentPet(petName: String): Boolean = currentPet?.coloredName?.contains(petName) ?: false
    fun isCurrentPetOrHigherRarity(petInternalName: NeuInternalName): Boolean {
        val (properPetName, startingRarity) = PetUtils.internalNameToPetWithRarity(petInternalName) ?: return false
        val currentPet = currentPet ?: return false
        return currentPet.cleanInternalName == properPetName && currentPet.rarity >= startingRarity
    }

    fun assertFoundCurrentData(petData: PetData) {
        if (petData.uuid == null) {
            ErrorManager.skyHanniError("Tried to assert a non-UUID having pet!")
        }
        ProfileStorageData.profileSpecific?.currentPetUuid = petData.uuid
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        chatSummonPattern.matchMatcher(event.message) {
            val resolvedPet = PetStorageApi.resolvePetDataOrNull(
                name = group("pet"),
                rarity = LorenzRarity.getByColorCode(group("rarity")[0]) ?: return,
                skinTag = groupOrNull("skin")?.replace(" ", ""),
            )?.takeIf {
                it.uuid != null
            } ?: return

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
