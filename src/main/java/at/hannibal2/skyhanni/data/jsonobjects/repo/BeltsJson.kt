package at.hannibal2.hanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class BeltsJson(
    @Expose val belts: Map<String, Int>,
)
