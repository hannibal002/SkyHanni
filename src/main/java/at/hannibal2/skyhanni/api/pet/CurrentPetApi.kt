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
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CurrentPetApi {
    val patternGroup = RepoPattern.group("misc.pet")

    /**
     * REGEX-TEST: §aYou summoned your §r§dRabbit§r§9 ✦§r§a!
     * REGEX-TEST: §aYou summoned your §r§6Golden Dragon§r§a!
     */
    private val chatSummonPattern by patternGroup.pattern(
        "chat.summon",
        "§aYou summoned your §r§(?<rarity>.)(?<pet>[^§]+)(?:§r(?<skin>§. ✦))?§r§a!",
    )

    val currentPet: PetData?
        get() = ProfileStorageData.profileSpecific?.currentPetUuid?.let { currentUuid ->
            ProfileStorageData.petProfiles?.pets?.firstOrNull { it.uuid == currentUuid }
        }

    fun isCurrentPet(petInternalName: NeuInternalName) = currentPet?.fauxInternalName == petInternalName
    fun isCurrentPet(petName: String): Boolean = currentPet?.coloredName?.contains(petName) ?: false
    fun isCurrentPetOrHigherRarity(petInternalName: NeuInternalName): Boolean {
        val currentPet = currentPet ?: return false
        val comparisonResult = PetUtils.comparePets(
            refPetInternalName = petInternalName,
            opPetInternalName = currentPet.fauxInternalName,
        ) ?: return false
        return comparisonResult >= 0
    }

    enum class PetDataAssertionSource {
        TAB,
        AUTOPET,
        MENU,
    }

    private val lastAssertion: MutableMap<PetDataAssertionSource, SimpleTimeMark> = enumMapOf()

    fun assertFoundCurrentData(petData: PetData, source: PetDataAssertionSource) {
        if (source == PetDataAssertionSource.TAB) {
            val lastApAssertion = lastAssertion[PetDataAssertionSource.AUTOPET]
            val cancelledByAp = lastApAssertion != null && lastApAssertion.passedSince() <= 5.seconds

            val lastMenAssertion = lastAssertion[PetDataAssertionSource.MENU]
            val cancelledByMenu = lastMenAssertion != null && lastMenAssertion.passedSince() >= 5.seconds

            if (cancelledByMenu || cancelledByAp) return
        }
        lastAssertion[source] = SimpleTimeMark.now()

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
        event.addIrrelevant {
            val petInfo = when (currentPet) {
                null -> "no pet equipped"
                else -> "currentPet:\n\n$currentPet"
            }
            add(petInfo)
        }
    }
}
