package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getCachedIngredients
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.recipes.CraftingRecipe

object FirstMinionTier {

    fun firstMinionTier(
        otherItems: Map<NEUInternalName, Int>,
        minions: MutableMap<String, NEUInternalName>,
        tierOneMinions: MutableList<NEUInternalName>,
        tierOneMinionsDone: MutableList<String>,
    ) {
        val help = helpMap(otherItems)
        val tierOneMinionsFiltered = getTierOneMinionsFiltered(tierOneMinions, tierOneMinionsDone)
        addMinion(tierOneMinionsFiltered, minions, tierOneMinionsDone)
        addMoreMinions(tierOneMinionsFiltered, help, minions)
    }

    private fun addMoreMinions(
        tierOneMinionsFiltered: List<NEUInternalName>,
        help: Map<NEUInternalName, Int>,
        minions: MutableMap<String, NEUInternalName>
    ) {
        for (minionId in tierOneMinionsFiltered) {
            for (recipe in NEUItems.getRecipes(minionId)) {
                if (recipe !is CraftingRecipe) continue
                checkOne(recipe, help, minions, minionId)
            }
        }
    }

    private fun checkOne(
        recipe: CraftingRecipe,
        help: Map<NEUInternalName, Int>,
        minions: MutableMap<String, NEUInternalName>,
        minionId: NEUInternalName
    ) {
        if (recipe.getCachedIngredients().any { help.contains(it.internalItemId.asInternalName()) }) {
            val name = recipe.output.itemStack.name!!.removeColor()
            val abc = name.replace(" I", " 0")
            minions[abc] = minionId.replace("_1", "_0")
        }
    }

    private fun addMinion(
        tierOneMinionsFiltered: List<NEUInternalName>,
        minions: MutableMap<String, NEUInternalName>,
        tierOneMinionsDone: MutableList<String>
    ) {
        for (minionId in tierOneMinionsFiltered) {
            val prefix = minionId.asString().dropLast(1)
            if (minions.any { it.value.startsWith(prefix) }) {
                tierOneMinionsDone.add(minionId.toString())
            }
        }
    }

    private fun getTierOneMinionsFiltered(
        tierOneMinions: MutableList<NEUInternalName>,
        tierOneMinionsDone: MutableList<String>
    ) = tierOneMinions.filter { it.asString() !in tierOneMinionsDone }

    private fun helpMap(otherItems: Map<NEUInternalName, Int>) =
        otherItems.filter { !it.key.startsWith("WOOD_") }
}
