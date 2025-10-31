package at.hannibal2.hanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class MaxwellPowersJson(
    @Expose val powers: MutableList<String>,
)
