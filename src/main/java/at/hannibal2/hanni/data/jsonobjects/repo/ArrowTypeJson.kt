package at.hannibal2.hanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class ArrowTypeJson(
    @Expose val arrows: Map<String, ArrowAttributes>,
)

data class ArrowAttributes(
    @Expose val arrow: String,
)
