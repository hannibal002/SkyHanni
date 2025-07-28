package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose

data class ErrorManagerJson(
    @Expose val breakAfter: List<String>,
    @Expose val replacements: Map<String, String>,
    @Expose val entireReplacements: Map<String, String>,
    @Expose val ignored: List<String>,
)
