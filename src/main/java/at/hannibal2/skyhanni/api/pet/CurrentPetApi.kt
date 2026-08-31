package at.hannibal2.skyhanni.api.pet

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.pets.PetChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
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

    fun isCurrentPet(petInternalName: NeuInternalName) = currentPet?.fauxInternalName == petInternalName
    fun isCurrentPet(petName: String): Boolean = currentPet?.coloredName?.contains(petName) ?: false
    fun isCurrentPetOrHigherRarity(petInternalName: NeuInternalName): Boolean {
        val currentPet = currentPet ?: return false
        val comparisonResult = PetUtils.comparePets(
            referencePetInternalName = petInternalName,
            otherPetInternalName = currentPet.fauxInternalName,
        ) ?: return false
        return comparisonResult >= 0
    }

    enum class PetDataAssertionSource {
        CHAT,
        TAB,
        AUTOPET,
        MENU,
    }

    private val lastAssertion: MutableMap<PetDataAssertionSource, SimpleTimeMark> = enumMapOf()
    private var currentPetSnapshot: PetData? = null
    private var pendingTabPetData: PetData? = null

    val currentPet: PetData?
        get() {
            val currentPetUuid = ProfileStorageData.profileSpecific?.currentPetUuid
            val storedPet = currentPetUuid?.let { uuid ->
                ProfileStorageData.petProfiles?.pets?.firstOrNull { it.uuid == uuid }
            }
            val snapshot = currentPetSnapshot
            return when {
                snapshot?.uuid == null -> snapshot ?: storedPet
                else -> storedPet ?: snapshot
            }
        }

    fun assertFoundCurrentData(petData: PetData, source: PetDataAssertionSource) {
        if (source == PetDataAssertionSource.TAB && shouldDelayTabAssertion()) {
            pendingTabPetData = petData
            return
        }
        pendingTabPetData = null
        lastAssertion[source] = SimpleTimeMark.now()

        currentPetSnapshot = petData
        ProfileStorageData.profileSpecific?.currentPetUuid = petData.uuid
        PetChangeEvent(petData).post()
    }

    fun updateCurrentPetExp(exp: Double): PetData? {
        val currentPet = currentPet ?: return null
        val currentExp = currentPet.exp ?: 0.0
        if (exp <= currentExp) return null

        currentPet.exp = exp
        currentPetSnapshot = currentPet
        return currentPet
    }

    private fun shouldDelayTabAssertion(): Boolean =
        wasRecentlyAsserted(PetDataAssertionSource.AUTOPET) || wasRecentlyAsserted(PetDataAssertionSource.MENU)

    private fun wasRecentlyAsserted(source: PetDataAssertionSource): Boolean =
        lastAssertion[source]?.passedSince()?.let { it <= 5.seconds } ?: false

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed() {
        val petData = pendingTabPetData ?: return
        if (shouldDelayTabAssertion()) return
        pendingTabPetData = null
        assertFoundCurrentData(petData, PetDataAssertionSource.TAB)
    }

    fun clearCurrentPet() {
        currentPetSnapshot = null
        pendingTabPetData = null
        ProfileStorageData.profileSpecific?.currentPetUuid = null
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        chatSummonPattern.matchMatcher(event.message) {
            val resolvedPet = PetStorageApi.resolvePetDataOrNull(
                name = group("pet"),
                rarity = LorenzRarity.getByColorCode(group("rarity")[0]) ?: return,
                skinTag = groupOrNull("skin")?.replace(" ", ""),
                skinTagKnown = true,
            ) ?: return

            assertFoundCurrentData(resolvedPet, PetDataAssertionSource.CHAT)
        }
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        fun PetData.formatForCurrentDebug() = buildString {
            append(getUserFriendlyName())
            append(" uuid=$uuid")
            append(" exp=${exp?.toLong() ?: 0L}")
        }
        event.title("CurrentPetApi")
        event.addIrrelevant {
            val petInfo = when (currentPet) {
                null -> "no pet equipped"
                else -> "currentPet:\n\n$currentPet"
            }
            add(petInfo)
            add(
                "lastAssertions: " + PetDataAssertionSource.entries.joinToString(", ") { source ->
                    "$source=${lastAssertion[source]?.passedSince() ?: "<never>"}"
                },
            )
            add("pendingTabPet: ${pendingTabPetData?.formatForCurrentDebug() ?: "<none>"}")
        }
    }
}
