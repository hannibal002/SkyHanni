package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

object Wardrobe {

    private val group = RepoPattern.group("inventory.wardrobe")
    private val inventoryPattern by group.pattern("inventory", "Wardrobe.*")

    fun inWardrobe() = inventoryPattern.matches(InventoryUtils.openInventoryName())
}
