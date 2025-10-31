package at.hannibal2.hanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class BingoRanksJson(
    @Expose val ranks: Map<String, Int>,
)
