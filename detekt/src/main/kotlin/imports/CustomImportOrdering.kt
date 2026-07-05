package imports

import PreprocessingPattern.Companion.containsPreprocessingPattern
import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportList

/**
 * This rule enforces correct import ordering, while ignoring preprocessed comments and imports that are in a preprocessed block.
 */
class CustomImportOrdering(config: Config) : SkyHanniRule(config, "Enforces correct import ordering, taking into account preprocessed imports.") {

    private fun isLineBlockSeparator(line: String): Boolean {
        val trimmed = line.trim()
        // Supports custom patterns as well as standard Stonecutter & preprocessor directives
        return trimmed.containsPreprocessingPattern() ||
            trimmed.startsWith("//?") ||
            trimmed.startsWith("//~") ||
            trimmed.startsWith("//#")
    }

    private fun isImportsCorrectlyOrdered(imports: List<KtImportDirective>, rawText: List<String>): Boolean {
        // 1. Group contiguous imports into logical blocks
        val blocks = mutableListOf<List<String>>()
        var currentBlock = mutableListOf<String>()

        for (line in rawText) {
            val trimmed = line.trim()

            // A preprocessor comment or an empty line safely terminates the current block
            if (trimmed.isEmpty() || isLineBlockSeparator(line)) {
                if (currentBlock.isNotEmpty()) {
                    blocks.add(currentBlock)
                    currentBlock = mutableListOf()
                }
            } else if (trimmed.startsWith("import ")) {
                currentBlock.add(line)
            }
        }
        if (currentBlock.isNotEmpty()) {
            blocks.add(currentBlock)
        }

        // 2. Ensure each block is internally lexicographically sorted
        var importIndex = 0
        for (blockText in blocks) {
            val blockImports = mutableListOf<KtImportDirective>()
            for (i in blockText.indices) {
                if (importIndex < imports.size) {
                    blockImports.add(imports[importIndex])
                }
                importIndex++
            }

            val expectedBlockImports = blockImports
                .sortedWith(ImportOrdering.getOrdering())
                .map { "import ${it.importPath}" }

            val formattedOriginal = blockText.joinToString("\n")
            val formattedExpected = expectedBlockImports.joinToString("\n")

            if (formattedOriginal != formattedExpected) {
                return false
            }
        }

        // 3. Prevent arbitrary empty lines between standard imports
        // (Empty lines are only legal if they separate pre-processed blocks)
        var previousLineWasImport = false
        var seenEmptyLine = false

        for (line in rawText) {
            val trimmed = line.trim()
            if (trimmed.startsWith("import ")) {
                if (previousLineWasImport && seenEmptyLine) {
                    return false // Illegal empty line splitting standard imports
                }
                previousLineWasImport = true
                seenEmptyLine = false
            } else if (trimmed.isEmpty()) {
                seenEmptyLine = true
            } else if (isLineBlockSeparator(line)) {
                // Hitting a valid separator resets the rule, allowing the prior empty line
                previousLineWasImport = false
                seenEmptyLine = false
            }
        }

        return true
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
                    "with \"java\", \"javax\", \"kotlin\" and aliases in the end. This should then be followed by " +
                    "pre-processed imports.",
            )
        }
        super.visitImportList(importList)
    }
}
