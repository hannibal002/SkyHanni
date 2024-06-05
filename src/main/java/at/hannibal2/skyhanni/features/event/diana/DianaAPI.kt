package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.PetAPI
import at.hannibal2.skyhanni.events.diana.InquisitorFoundEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object DianaAPI {

    private val spade by lazy { "ANCESTRAL_SPADE".asInternalName() }

    fun hasSpadeInHand() = InventoryUtils.itemInHandId == spade

    private fun isRitualActive() = Perk.MYTHOLOGICAL_RITUAL.isActive ||
        Perk.PERKPOCALYPSE.isActive

    fun hasGriffinPet() = PetAPI.isCurrentPet("Griffin")

    fun isDoingDiana() = IslandType.HUB.isInIsland() && isRitualActive() && hasSpadeInInventory()

    val ItemStack.isDianaSpade get() = getInternalName() == spade

    private fun hasSpadeInInventory() = InventoryUtils.getItemsInOwnInventory().any { it.isDianaSpade }

    @SubscribeEvent
    fun onJoinWorld(event: EntityEnterWorldEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val entity = event.entity
        if (entity is EntityOtherPlayerMP && entity.name == "Minos Inquisitor") {
            InquisitorFoundEvent(entity).postAndCatch()
        }
    }
}
