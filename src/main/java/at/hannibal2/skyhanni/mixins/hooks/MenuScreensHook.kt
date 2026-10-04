package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.inventory.loadout.CustomLoadout
import at.hannibal2.skyhanni.features.inventory.wardrobe.CustomEquipmentWardrobe
import at.hannibal2.skyhanni.features.inventory.wardrobe.CustomWardrobe
import at.hannibal2.skyhanni.utils.AbstractCustomMenu
import at.hannibal2.skyhanni.utils.CustomMenuEditScreen
import at.hannibal2.skyhanni.utils.CustomMenuScreen
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.unformattedTextCompat
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.inventory.MenuType

// Reference: https://github.com/SkyblockerMod/Skyblocker/blob/main/src/main/java/de/hysky/skyblocker/mixins/MenuScreensConstructorMixin.java
object MenuScreensHook {
    @JvmStatic
    fun <T : AbstractContainerMenu> openCustomMenu(
        name: Component,
        type: MenuType<T>,
        client: Minecraft,
        id: Int,
    ): Boolean {
        if (!SkyBlockUtils.inSkyBlock) return false

        val player = client.player ?: return false
        val inventory = player.inventory
        val inventoryName = name.unformattedTextCompat()

        for (gui in listOf(CustomWardrobe, CustomEquipmentWardrobe, CustomLoadout)) {
            if (openCustomGui(gui, inventoryName, name, type, client, id, inventory)) {
                return true
            }
        }

        return false
    }

    private fun <T : AbstractContainerMenu> openCustomGui(
        gui: AbstractCustomMenu,
        inventoryName: String,
        name: Component,
        type: MenuType<T>,
        client: Minecraft,
        id: Int,
        inventory: Inventory,
    ): Boolean {
        if (!gui.shouldReplace(inventoryName)) return false

        val menu = type.create(id, inventory) as? ChestMenu ?: return false

        client.player?.containerMenu = menu

        when (val screen = MinecraftCompat.screen) {
            is CustomMenuScreen if screen.gui === gui -> screen.changeHandler(menu, name)
            is CustomMenuEditScreen if screen.gui === gui ->
                MinecraftCompat.screen = CustomMenuEditScreen(menu, inventory, name, gui)

            else -> MinecraftCompat.screen = CustomMenuScreen(menu, name, gui)
        }

        return true
    }
}
