package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class EssenceShopsJson(
    @Expose val shops: List<EssenceShopJson>
)

data class EssenceShopJson(
    @Expose val type: String,
    @Expose val upgrades: List<EssenceShopUpgrade>,
    @Expose val essenceId: String? = null,
)

data class EssenceShopUpgrade(
    @Expose val name: String,
    @Expose val cost: Int? = null,
    @Expose val costs: List<Int>? = null
)
