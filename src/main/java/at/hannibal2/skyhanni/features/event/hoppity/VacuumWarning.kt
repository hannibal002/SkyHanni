package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.features.HoppityEggsManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.item.ItemStack
import chocolatefactory.api.ChocolateFactoryAPI

object VacuumWarning {

    private val mc: Minecraft = Minecraft.getMinecraft()

    fun check() {
        if (!HoppityEggsManager.config.vacuumWarning) return
        if (!ChocolateFactoryAPI.isHoppityEvent()) return
        if (LocationUtils.isInGarden()) return

        val player: EntityPlayerSP = mc.player ?: return
        val heldItem: ItemStack? = player.heldItemMainhand

        if (heldItem != null && isVacuumItem(heldItem)) {
            warn()
        }
    }

    private fun isVacuumItem(item: ItemStack): Boolean {
        val itemName = item.displayName.lowercase()
        return "vacuum" in itemName
    }

    private fun warn() {
        ChatUtils.chat("Wrong tool! You're looking for eggs, not pests!")
        LorenzUtils.sendTitle("Try using an Egglocator!", 3.seconds)
    }
}
