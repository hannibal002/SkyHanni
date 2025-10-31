package at.hannibal2.hanni.features.event.diana

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.api.pet.CurrentPetApi
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.Perk
import at.hannibal2.hanni.events.diana.InquisitorFoundEvent
import at.hannibal2.hanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.item.ItemStack

@HanniModule
object DianaApi {

    private val spade = "ANCESTRAL_SPADE".toInternalName()

    fun hasSpadeInHand() = InventoryUtils.itemInHandId == spade

    private fun isRitualActive() = Perk.MYTHOLOGICAL_RITUAL.isActive ||
        Perk.PERKPOCALYPSE.isActive

    fun hasGriffinPet() = CurrentPetApi.isCurrentPet("Griffin")

    fun isDoingDiana() = IslandType.HUB.isCurrent() && isRitualActive() && hasSpadeInInventory()

    val ItemStack.isDianaSpade get() = getInternalName() == spade

    private fun hasSpadeInInventory() = InventoryUtils.getItemsInOwnInventory().any { it.isDianaSpade }

    @HandleEvent(onlyOnSkyblock = true)
    fun onJoinWorld(event: EntityEnterWorldEvent<EntityOtherPlayerMP>) {
        if (event.entity.name == "Minos Inquisitor") {
            InquisitorFoundEvent(event.entity).post()
        }
    }
}
