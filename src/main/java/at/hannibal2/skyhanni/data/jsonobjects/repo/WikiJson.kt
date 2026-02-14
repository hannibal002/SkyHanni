package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class WikiJson(
    @Expose val official: Wiki,
    @Expose val unofficial: Wiki,
)

data class Wiki(
    @Expose val name: String,
    @Expose val urlPrefix: String,
    @Expose val searchPrefix: String,
)
