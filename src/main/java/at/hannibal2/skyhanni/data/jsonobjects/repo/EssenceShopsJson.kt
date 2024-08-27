package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class EssenceShopsJson(
    @Expose val shops: Map<String, EssenceShopJson>
)

data class EssenceShopJson(
    @Expose val upgrades: Map<String, EssenceShopUpgrade>
)

data class EssenceShopUpgrade(
    @Expose val name: String,
    @Expose val costs: List<Int>
)
