enum class PreprocessingPattern(val text: String) {
    IF("//? if"),
    ELSEIF("//?} elseif"),
    ELSE("//?} else"),
    ENDIF("//?}"),
    ;

    fun matches(line: String): Boolean =
        line.trim().startsWith(text)

    companion object {
        fun String.containsPreprocessingPattern(): Boolean =
            entries.any { it.matches(this) }
    }
}
