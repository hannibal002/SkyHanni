package imports

import PreprocessingPattern
import SkyHanniRule
import dev.detekt.api.Config
import org.jetbrains.kotlin.KtPsiSourceFileLinesMapping
import org.jetbrains.kotlin.psi.KtFile

/**
 * This rule enforces correct import ordering, while taking preprocessed imports into account.
 */
class CustomImportOrdering(config: Config) :
    SkyHanniRule(config, "Enforces correct import ordering, taking into account preprocessed imports.") {

    private data class ImportLine(
        val text: String,
        val lineIndex: Int,
    )

    private data class ImportBlock(
        val inPreprocessingBlock: Boolean,
        val importLines: List<ImportLine>,
    )

    private fun createImportBlocks(file: KtFile): List<ImportBlock> {
        val blocks = mutableListOf<ImportBlock>()
        var currentImports = mutableListOf<ImportLine>()
        var inPreprocessingBlock = false
        var inImportSection = false

        fun flush() {
            if (currentImports.isNotEmpty()) {
                blocks += ImportBlock(inPreprocessingBlock, currentImports)
                currentImports = mutableListOf()
            }
        }

        fun isImportLine(line: String): Boolean =
            line.startsWith("import ") ||
                line.startsWith("/*import ")

        for ((index, rawLine) in file.text.lineSequence().withIndex()) {
            val line = rawLine.trim()

            when {
                line.startsWith("package ") -> continue

                !inImportSection && line.isBlank() -> continue

                PreprocessingPattern.IF.matches(line) -> {
                    inImportSection = true
                    flush()
                    inPreprocessingBlock = true
                }

                PreprocessingPattern.ELSEIF.matches(line) ||
                    PreprocessingPattern.ELSE.matches(line) -> {
                    flush()
                    inPreprocessingBlock = true
                }

                PreprocessingPattern.ENDIF.matches(line) -> {
                    flush()
                    inPreprocessingBlock = false
                }

                isImportLine(line) -> {
                    inImportSection = true

                    currentImports += ImportLine(
                        line.removePrefix("/*"),
                        index,
                    )
                }

                line.isBlank() && inImportSection -> {
                    // Keep blank lines because spacing validation needs them.
                }

                inImportSection -> {
                    flush()
                    break
                }
            }
        }

        flush()

        return blocks
    }

    // Preprocessed blocks must be at the end of the import list.
    private fun findPreprocessingBlockOrderViolation(
        blocks: List<ImportBlock>,
    ): ImportLine? {
        var preprocessingStarted = false

        for (block in blocks) {
            if (block.inPreprocessingBlock) {
                preprocessingStarted = true
            } else if (preprocessingStarted) {
                return block.importLines.first()
            }
        }

        return null
    }

    // Must have empty lines between preprocessed and non-preprocessed blocks.
    private fun findSpacingBetweenBlocksViolations(
        fileLines: List<String>,
        blocks: List<ImportBlock>,
    ): List<ImportLine> =
        blocks.zipWithNext().mapNotNull { (current, next) ->
            if (
                current.inPreprocessingBlock != next.inPreprocessingBlock &&
                fileLines
                    .subList(
                        current.importLines.last().lineIndex + 1,
                        next.importLines.first().lineIndex,
                    )
                    .none(String::isBlank)
            ) {
                next.importLines.first()
            } else {
                null
            }
        }

    // Must not have empty lines inside a block.
    private fun findSpacingInsideBlockViolations(
        fileLines: List<String>,
        blocks: List<ImportBlock>,
    ): List<ImportLine> =
        blocks.flatMap { block ->
            block.importLines.zipWithNext().mapNotNull { (current, next) ->
                if (
                    fileLines
                        .subList(
                            current.lineIndex + 1,
                            next.lineIndex,
                        )
                        .any(String::isBlank)
                ) {
                    next
                } else {
                    null
                }
            }
        }

    // Each block must be ordered according to the ordering defined in ImportOrdering.
    // Returns the first out of order import in order to not spam a lot of issues for the same block.
    private fun findOrderingViolations(
        blocks: List<ImportBlock>,
    ): List<ImportLine> =
        blocks.mapNotNull { block ->
            val imports = block.importLines.map { it.text }
            val sortedImports = imports.sortedWith(ImportOrdering.getOrdering())

            block.importLines.zip(sortedImports)
                .firstOrNull { (actual, expected) ->
                    actual.text != expected
                }
                ?.first
        }

    override fun visitKtFile(file: KtFile) {
        val blocks = createImportBlocks(file)
        if (blocks.isEmpty()) {
            return
        }

        val fileLines by lazy { file.text.lines() }
        val sourceFileLinesMapping by lazy { KtPsiSourceFileLinesMapping(file) }

        findPreprocessingBlockOrderViolation(blocks)?.let {
            reportIssue(
                "Preprocessed import blocks must be at the end of the import list.",
                it.lineIndex,
                fileLines[it.lineIndex],
                file,
                sourceFileLinesMapping
            )
        }

        findSpacingInsideBlockViolations(fileLines, blocks).forEach {
            reportIssue(
                "Import blocks must not contain empty lines between imports.",
                it.lineIndex,
                fileLines[it.lineIndex],
                file,
                sourceFileLinesMapping
            )
        }

        findSpacingBetweenBlocksViolations(fileLines, blocks).forEach {
            reportIssue(
                "Preprocessed and non-preprocessed import blocks must be separated by an empty line.",
                it.lineIndex,
                fileLines[it.lineIndex],
                file,
                sourceFileLinesMapping
            )
        }

        findOrderingViolations(blocks).forEach {
            reportIssue(
                "Imports must be ordered in lexicographic order with \"java\", \"javax\", \"kotlin\", \"kotlinx\" and aliases in the end.",
                it.lineIndex,
                fileLines[it.lineIndex],
                file,
                sourceFileLinesMapping
            )
        }

        super.visitKtFile(file)
    }
}
