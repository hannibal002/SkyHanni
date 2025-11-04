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
import at.hannibal2.skyhanni.events.diana.InquisitorFoundEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.item.ItemStack

@SkyHanniModule
object DianaApi {

    private var spades = emptySet<NeuInternalName>()

    fun hasSpadeInHand() = InventoryUtils.itemInHandId in spades

    private fun isRitualActive() = (Perk.MYTHOLOGICAL_RITUAL.isActive || Perk.PERKPOCALYPSE.isActive) ||
        SkyHanniMod.feature.dev.debug.assumeMayor.get() == ElectionCandidate.DIANA

    fun hasGriffinPet() = CurrentPetApi.isCurrentPet("Griffin")

    fun isDoingDiana() = IslandType.HUB.isCurrent() && isRitualActive() && hasSpadeInInventory()

    val ItemStack.isDianaSpade get() = getInternalName() in spades

    private fun hasSpadeInInventory() = InventoryUtils.getItemsInOwnInventory().any { it.isDianaSpade }

    var mythologicalCreatures = emptyMap<String, MythologicalCreatureType>()
        private set

    fun getCreatureByTrackerName(name: String) = mythologicalCreatures.firstNotNullOfOrNull { (_, creature) ->
        if (creature.trackerId == name) creature else null
    }

    var sphinxQuestions = emptyMap<String, String>()
        private set

    @HandleEvent(onlyOnSkyblock = true)
    fun onJoinWorld(event: EntityEnterWorldEvent<EntityOtherPlayerMP>) {
        if (event.entity.name == "Minos Inquisitor") {
            InquisitorFoundEvent(event.entity).post()
        }
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val dianaJson = event.getConstant<DianaJson>("events/Diana")

        mythologicalCreatures = dianaJson.mythologicalCreatures
        sphinxQuestions = dianaJson.sphinxQuestions
        spades = dianaJson.spadeTypes.toSet()
    }
}
