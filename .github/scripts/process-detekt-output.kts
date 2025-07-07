#!/usr/bin/env kotlin

import java.io.File

val prSha = System.getenv("PR_SHA")
    ?: error("PR_SHA environment variable not set")

val githubRepo = System.getenv("GITHUB_REPOSITORY")
    ?: error("GITHUB_REPOSITORY environment variable not set")

val detektFile = File("detekt_output.txt")
val detektFileHash = detektFile.hashCode()
val detektOutput = detektFile.readText()
val lines = detektOutput.split('\n')

/**
 * REGEX-TEST: ::warning file=src/main/java/at/hannibal2/skyhanni/config/features/dev/DevConfig.kt,line=24,title=detekt.FormattingRules.StorageVarOrVal,col=5,endColumn=61::NeuRepositoryConfig `neuRepo` should be a val
 * REGEX-TEST: ::warning file=src/main/java/at/hannibal2/skyhanni/config/features/dev/NeuRepositoryConfig.kt,line=23,title=detekt.FormattingRules.StorageVarOrVal,col=5,endColumn=73::Runnable `updateRepo` should be a val
 * REGEX-TEST: ::warning file=src/main/java/at/hannibal2/skyhanni/config/features/dev/NeuRepositoryConfig.kt,line=27,title=detekt.FormattingRules.StorageVarOrVal,col=5,endColumn=60::RepositoryLocation `location` should be a val
 * REGEX-TEST: ::warning file=src/main/java/at/hannibal2/skyhanni/config/features/dev/NeuRepositoryConfig.kt,line=33,title=detekt.FormattingRules.StorageVarOrVal,col=9,endColumn=93::Runnable `resetRepoLocation` should be a val
 * REGEX-TEST: ::warning file=src/main/java/at/hannibal2/skyhanni/features/foraging/ForagingTrackerLegacy.kt,line=148,title=detekt.RepoRules.RepoPatternRegexTestFailed,col=28,endColumn=6::Repo pattern `hoverRewardPattern` failed regex test: `§2Forest Essence §8x4` pattern: `(?:§.)+(?<item>.*) (?:§.)+§8x(?<amount>[\d,-]+)`.
 */
val sarifRegex = Regex("^::warning file=(?<filePath>src\\/[^,]*\\/(?<file>[^,]+)),line=(?<line>\\d+),title=(?<wholeRule>(?<provider>[^.]+)\\.(?:(?:\\w+)\\.)+(?<rule>[^.]+)),col=(?<col>\\d+),endColumn=(?<endcol>\\d+)::(?<message>(?:.|)*\\n*)\$")
val sarifPattern = sarifRegex.toPattern()

val urlBase = "https://github.com/$githubRepo/blob/$prSha/"

val rulesBroken: MutableMap<String, Int> = mutableMapOf()
val violatingFiles: MutableMap<String, Int> = mutableMapOf()
val flaggedFileUrls: MutableMap<String, String> = mutableMapOf()

val pathToNameCache: MutableMap<String, String> = mutableMapOf()
val wholeRuleToNameCache: MutableMap<String, String> = mutableMapOf()

val formatLines = lines.filter { it.isNotBlank() }.mapNotNull { raw ->
    sarifPattern.matcher(raw).takeIf { it.matches() }?.let {
        val filePath = it.group("filePath")
        val fileName = it.group("file")
        val lineNum = it.group("line")
        val rule = it.group("rule")
        val wholeRule = it.group("wholeRule")
        val message = it.group("message")

        val cleanedFilePath = filePath.substringAfter("src/")

        rulesBroken[wholeRule] = rulesBroken.getOrDefault(wholeRule, 0) + 1
        violatingFiles[cleanedFilePath] = violatingFiles.getOrDefault(cleanedFilePath, 0) + 1
        pathToNameCache[cleanedFilePath] = fileName
        wholeRuleToNameCache[wholeRule] = rule

        val urlFormat = "$urlBase$cleanedFilePath#L$lineNum"
        flaggedFileUrls[cleanedFilePath] = urlFormat

        cleanedFilePath to "- [${fileName}#L${lineNum}]($urlFormat) `$rule`: $message"
    }
}

val totalRulesBroken = rulesBroken.values.sum()
val shouldCollapseComment = totalRulesBroken > 5

fun buildCollapsedComment(): String = buildString {
    appendLine("<details>")
    appendLine("<summary>Rule violations</summary>")
    appendLine()
    formatLines.forEach { (_, line) ->
        appendLine(line)
    }
    appendLine("</details>")
}

val sb = StringBuilder().apply {
    append("### $totalRulesBroken Detekt Failure")
    if (totalRulesBroken != 1) append("s")
    append("\n")

    if (rulesBroken.size > 1) {
        val ceilingedKeys = rulesBroken.keys.take(6)
        val xMoreFormat = when (ceilingedKeys.size) {
            rulesBroken.keys.size -> ""
            else -> " (+ ${rulesBroken.size - ceilingedKeys.size} more)"
        }
        val ruleViolationsFormat = ceilingedKeys.joinToString(", ") {
            val ruleName = wholeRuleToNameCache[it] ?: it
            "`$ruleName`"
        }
        append ("**<ins>Rules flagged</ins>** (${rulesBroken.size}): $ruleViolationsFormat$xMoreFormat\n")
    }

    if (violatingFiles.size > 1) {
        val ceilingedFiles = violatingFiles.entries.take(6)
        val xMoreFormat = when (ceilingedFiles.size) {
            violatingFiles.keys.size -> ""
            else -> " (+ ${violatingFiles.size - ceilingedFiles.size} more)"
        }
        val fileViolationsFormat = ceilingedFiles.joinToString(", ") { (cleanedFilePath, _) ->
            val url = flaggedFileUrls[cleanedFilePath]
            val fileName = pathToNameCache[cleanedFilePath] ?: cleanedFilePath.substringAfter("src/")
            when (url) {
                null -> "`$fileName`"
                else -> "[$fileName]($url)"
            }
        }
        append("**<ins>Files flagged</ins>** (${violatingFiles.size}): $fileViolationsFormat$xMoreFormat\n")
    }

    appendLine()
    if (shouldCollapseComment) {
        appendLine(buildCollapsedComment())
    } else {
        formatLines.forEach { (_, line) ->
            appendLine(line)
        }
    }

    appendLine()
    appendLine("<!-- detekt-sarif-hash:$detektFileHash -->")
}

File("detekt_comment.txt").writeText(sb.toString())
