package imports

import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportList

/**
 * This rule enforces correct import ordering, while ignoring preprocessed comments and imports that are in a preprocessed block.
 */
class CustomImportOrdering(config: Config) :
    SkyHanniRule(config, "Enforces correct import ordering, taking into account preprocessed imports.") {

    private fun isImportsCorrectlyOrdered(imports: List<KtImportDirective>, rawText: List<String>): Boolean {
        if (rawText.any(String::isBlank)) {
            return false
        }

        val ordering = ImportOrdering.getOrdering()
        val importIterator = imports.iterator()
        val currentBlock = mutableListOf<String>()

        fun flushBlock(): Boolean {
            if (currentBlock.isEmpty()) {
                return true
            }

            val expected = buildList {
                repeat(currentBlock.size) {
                    if (!importIterator.hasNext()) {
                        return false
                    }
                    add(importIterator.next())
                }
            }
                .sortedWith(ordering)
                .map { "import ${it.importPath}" }

            val matches = currentBlock == expected
            currentBlock.clear()
            return matches
        }

        for (line in rawText) {
            val trimmed = line.trim()

            when {
                PreprocessingPattern.IF.matches(trimmed) ||
                    PreprocessingPattern.ELSEIF.matches(trimmed) ||
                    PreprocessingPattern.ELSE.matches(trimmed) ||
                    PreprocessingPattern.ENDIF.matches(trimmed) -> {
                    if (!flushBlock()) {
                        return false
                    }
                }

                trimmed.startsWith("import ") -> {
                    currentBlock += trimmed
                }
            }
        }

        if (!flushBlock()) {
            return false
        }

        return !importIterator.hasNext()
    }

    override fun visitImportList(importList: KtImportList) {
        val rawText = importList.text.trim()
        if (rawText.isBlank()) {
            return
        }

        val importsCorrect = isImportsCorrectlyOrdered(importList.imports, rawText.lines())

        if (!importsCorrect) {
            importList.reportIssue(
                "Imports must be ordered in lexicographic order without any empty lines in-between " +
                    "with \"java\", \"javax\", \"kotlin\", \"kotlinx\" and aliases in the end. This should then be followed by " +
                    "pre-processed imports.",
            )
        }
        super.visitImportList(importList)
    }
}
