package at.hannibal2.skyhanni.data.jsonobjects.repo.neu.recipe

enum class NeuRecipeType(
    val neuRepoId: String? = null,
    val useForCraftCost: Boolean = true,
    val castClazz: Class<out NeuAbstractRecipe> = NeuAbstractRecipe::class.java,
) {
    FORGE("forge", castClazz = NeuForgeRecipeJson::class.java),
    TRADE("trade", false, castClazz = NeuTradeRecipeJson::class.java),
    MOB_DROP("drops", false, castClazz = NeuMobDropsRecipeJson::class.java),
    NPC_SHOP("npc_shop", castClazz = NeuNpcShopRecipeJson::class.java),
    KAT_UPGRADE("katgrade", false, castClazz = NeuKatUpgradeRecipeJson::class.java),
    CRAFTING("crafting", castClazz = NeuCraftingRecipeJson::class.java),
    ;

    companion object {
        fun fromNeuIdOrNull(neuId: String): NeuRecipeType? = entries.firstOrNull { it.neuRepoId == neuId }
        fun fromNeuId(neuId: String) = entries.first { it.neuRepoId == neuId }
    }
}
