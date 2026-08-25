package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.data.ElectionCandidate
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.jsonobjects.repo.DianaJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.MythologicalCreatureType
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.diana.RareDianaMobFoundEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.player.RemotePlayer

@SkyHanniModule
object DianaApi {

    private var spades = emptySet<NeuInternalName>()

    private var dianaFoundOverride: Boolean? = null

    fun hasSpadeInHand() = InventoryUtils.itemInHandId in spades

    fun isRitualActive(): Boolean {
        dianaFoundOverride?.let { return it }
        return (Perk.MYTHOLOGICAL_RITUAL.isActive || Perk.PERKPOCALYPSE.isActive) ||
            SkyHanniMod.feature.dev.debug.assumeMayor.get() == ElectionCandidate.DIANA
    }

    fun hasGriffinPet() = CurrentPetApi.isCurrentPet("Griffin")

    // This is an OR rather than an AND due to mayor perk being unreliable sometimes
    fun isDoingDiana() = IslandType.HUB.isInIsland() && isRitualActive() && hasSpadeInHotbar()

    val SafeItemStack.isDianaSpade get() = getInternalName() in spades

    val NeuInternalName.isDianaSpade get() = this in spades

    private fun hasSpadeInHotbar() = InventoryUtils.getItemsInHotbar().any { it.isDianaSpade }

    var mythologicalCreatures = emptyMap<String, MythologicalCreatureType>()
        private set

    fun getCreatureByTrackerName(name: String) = mythologicalCreatures.firstNotNullOfOrNull { (_, creature) ->
        if (creature.trackerId == name) creature else null
    }

    var sphinxQuestions = emptyMap<String, String>()
        private set

    private val group = RepoPattern.group("event-diana")

    /**
     * REGEX-TEST: Minos Inquisitor
     * REGEX-TEST: Sphinx
     * REGEX-TEST: King Minos
     * REGEX-TEST: Manticore
     */
    private val rareDianaMobNamePattern by group.pattern(
        "rare-mob-name",
        "(?:Minos Inquisitor|Sphinx|King Minos|Manticore)\\s*",
    )

    /**
     * REGEX-TSET: The mythological ritual isn't active
     */
    private val ritualNotActivePattern by group.pattern(
        "ritual-not-active",
        "The mythological ritual isn't active",
    )

    @HandleEvent(onlyOnIsland = HUB)
    private fun onJoinWorld(event: EntityEnterWorldEvent<RemotePlayer>) {
        val entity = event.entity
        // TODO: fetch rare mobs from repo instead
        if (rareDianaMobNamePattern.matches(entity.name.string.trim())) {
            dianaFoundOverride = true
            RareDianaMobFoundEvent(entity).post()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onWorldChange() {
        dianaFoundOverride = null
    }

    @HandleEvent(onlyOnIsland = HUB)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (ritualNotActivePattern.matches(event.message)) {
            dianaFoundOverride = false
        }
    }

    @HandleEvent
    private fun onRepoReload(event: RepositoryReloadEvent) {
        val dianaJson = event.getConstant<DianaJson>("events/Diana")

        mythologicalCreatures = dianaJson.mythologicalCreatures
        sphinxQuestions = dianaJson.sphinxQuestions
        spades = dianaJson.spadeTypes.toSet()
    }

    fun overrideDianaActive() {
        dianaFoundOverride = true
    }
}
