@file:DependsOn("com.google.code.gson:gson:2.10.1")
// Execution context: base branch
// called from detekt-review.yml, build-review.yml, label-merge-conflict.yml, changelog-review.yml, and check_dependencies.yml

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.system.exitProcess

val workflowFailedMarker = "<!-- workflow-failed -->"

val detektLabel = "Detekt"
val detektMarker = "<!-- detekt-review -->"
val detektStaleMarker = "<!-- detekt-review-stale -->"

val buildLabel = "Fails Multi-Version"
val buildMarker = "<!-- build-failure-review -->"
val buildStaleMarker = "<!-- build-failure-review-stale -->"

val conflictLabel = "Merge Conflicts"
val conflictMarker = "<!-- merge-conflict-review -->"
val conflictStaleMarker = "<!-- merge-conflict-review-stale -->"

val changelogLabel = "Wrong Title/Changelog"
val changelogMarker = "<!-- changelog-check-review -->"
val changelogStaleMarker = "<!-- changelog-check-review-stale -->"

val dependencyLabel = "Waiting on Dependency PR"

val warningIcon = "⚠\uFE0F"

val maxDirectFindings = 8
val maxLogChars = 10_000

val repo: String = System.getenv("GITHUB_REPOSITORY") ?: error("GITHUB_REPOSITORY not set")
val token: String = System.getenv("GH_TOKEN") ?: error("GH_TOKEN not set")
val mode: String = System.getenv("MODE") ?: error("MODE not set")

val httpClient: HttpClient = HttpClient.newHttpClient()
val gson = Gson()

var errorCommentPosted = false

fun error(message: String, commentError: Boolean = true): Nothing {
    System.err.println(message)
    if (commentError && !errorCommentPosted) {
        val (postStatus, _) = ghRequest("POST", "/repos/$repo/issues/$prNumber/comments", mapOf("body" to buildErrorComment(message)))
        postStatus.requireSuccess("Error: could not post workflow error as comment (HTTP $postStatus)", commentError = false)
        errorCommentPosted = true
    }
    exitProcess(1)
}

fun buildErrorComment(message: String): String = buildString {
    appendLine(workflowFailedMarker)

    appendLine("❌ Workflow failed ❌")
    appendLine()

    appendLine("Error message:")
    appendLine(message)
    appendLine()

    appendLine("mode:")
    appendLine(mode)
    appendLine()

    appendLine("Most likely fix: merge beta into this PR.")
    appendLine("If the issue persists, ping @hannibal002 or another maintainer.")
    appendLine()

    val runId = System.getenv("GITHUB_RUN_ID")
    if (runId != null) {
        val runLink = " \\[[workflow run](https://github.com/$repo/actions/runs/$runId)\\]"
        appendLine("For investigating this error, see $runLink")
    } else {
        appendLine("GITHUB_RUN_ID is null, good luck finding the issue")
    }

}

val Int.isHttpError: Boolean get() = this !in 200..299

fun Int.requireSuccess(message: String, commentError: Boolean = true) {
    if (isHttpError) error(message, commentError)
}

data class Finding(val path: String, val line: Int, val ruleId: String, val message: String)

data class Dependency(val owner: String, val repoName: String, val pullNumber: Int)

fun ghRequest(method: String, path: String, payload: Any? = null): Pair<Int, JsonElement> {
    val bodyPublisher = if (payload != null)
        HttpRequest.BodyPublishers.ofString(gson.toJson(payload))
    else
        HttpRequest.BodyPublishers.noBody()

    val request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.github.com$path"))
        .header("Authorization", "Bearer $token")
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .header("Content-Type", "application/json")
        .method(method, bodyPublisher)
        .build()

    val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    val body = runCatching { JsonParser.parseString(response.body()) }.getOrDefault(JsonNull.INSTANCE)
    return response.statusCode() to body
}

fun setLabel(prNumber: String, label: String, hasFindings: Boolean) {
    if (hasFindings) {
        val (status, _) = ghRequest("POST", "/repos/$repo/issues/$prNumber/labels", mapOf("labels" to listOf(label)))
        if (status.isHttpError) System.err.println("Warning: could not add $label label (HTTP $status)")
    } else {
        val encoded = URLEncoder.encode(label, StandardCharsets.UTF_8).replace("+", "%20")
        val (status, _) = ghRequest("DELETE", "/repos/$repo/issues/$prNumber/labels/$encoded")
        if (status.isHttpError && status != 404) System.err.println("Warning: could not remove $label label (HTTP $status)")
    }
}

fun getPrLabels(prNumber: String): Set<String> {
    val (status, body) = ghRequest("GET", "/repos/$repo/issues/$prNumber/labels")
    if (status.isHttpError) return emptySet()
    val array = body as? JsonArray ?: return emptySet()
    return array.mapNotNull {
        (it as? JsonObject)?.get("name")?.takeIf { it.isJsonPrimitive }?.asString
    }.toSet()
}

fun normalizePath(uri: String, workspace: String): String {
    val path = runCatching { URI.create(uri).path }.getOrNull() ?: uri.removePrefix("file://")
    if (workspace.isNotEmpty() && path.startsWith(workspace)) return path.removePrefix(workspace).trimStart('/')
    val repoName = repo.substringAfter("/")
    if (repoName.isNotEmpty() && "$repoName/" in path) return path.substringAfter("$repoName/")
    return path
}

fun sanitize(text: String, maxLen: Int = 300): String = text
    .take(maxLen)
    .replace("\\", "\\\\")
    .replace("`", "\\`")
    .replace("*", "\\*")
    .replace("_", "\\_")
    .replace("[", "\\[")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("@", "&#64;")

fun StringBuilder.appendWarningTitle(title: String) {
    appendLine("### $warningIcon $title $warningIcon")
}

fun buildDetektBody(findings: List<Finding>): String = buildString {
    appendLine(detektMarker)
    appendWarningTitle("Detekt found ${findings.size} ${if (findings.size == 1) "issue" else "issues"}")
    appendLine("")
    val direct = findings.take(maxDirectFindings)
    val overflow = findings.drop(maxDirectFindings)
    appendAll(direct)
    if (overflow.isNotEmpty()) {
        appendLine("\n<details><summary>${overflow.size} more ${if (overflow.size == 1) "issue" else "issues"}</summary>\n")
        appendAll(overflow)
        appendLine("\n</details>")
    }
}

fun StringBuilder.appendAll(findings: List<Finding>) {
    for (finding in findings) {
        val fileName = finding.path.substringAfterLast('/')
        val ruleId = sanitize(finding.ruleId)
        val message = sanitize(finding.message)
        val className = sanitize(fileName)
        val line = finding.line
        val path = sanitize(finding.path)
        appendLine("- ```$className``` at line $line: $message")
        appendLine("  rule: `$ruleId`, path: `$path`")
    }
}

fun findExistingComment(prNumber: String, searchMarker: String): Long? {
    var page = 1
    while (true) {
        val (status, body) = ghRequest("GET", "/repos/$repo/issues/$prNumber/comments?per_page=100&page=$page")
        status.requireSuccess("Error: could not fetch PR comments (HTTP $status), aborting")
        val array = body as? JsonArray ?: error("Error: unexpected response format for PR comments, aborting")
        if (array.size() == 0) return null
        for (element in array) {
            if (!element.isJsonObject) continue
            val bodyText = element.asJsonObject.get("body")?.takeIf { it.isJsonPrimitive }?.asString ?: continue
            if (searchMarker in bodyText) return element.asJsonObject.get("id")?.takeIf { it.isJsonPrimitive }?.asLong
        }
        if (array.size() < 100) return null
        page++
    }
}

fun getCommentBody(commentId: Long): String? {
    val (status, body) = ghRequest("GET", "/repos/$repo/issues/comments/$commentId")
    status.requireSuccess("Error: could not fetch comment body (HTTP $status), aborting")
    return (body as? JsonObject)?.get("body")?.takeIf { it.isJsonPrimitive }?.asString
}

fun markCommentAsStale(
    commentId: Long,
    activeMarker: String,
    staleMarker: String,
    expandLabel: String,
) {
    val oldBody = getCommentBody(commentId)
        ?: error("Error: comment body was null for comment $commentId, aborting")

    val cleanedOld = oldBody
        .replace(activeMarker, "")
        .trim()

    val header = cleanedOld
        .lineSequence()
        .firstOrNull { it.startsWith("### ") }
        ?.removePrefix("###")
        ?.replace(warningIcon, "")
        ?.trim()
        ?: "Unknown"

    val staleBody = buildString {
        appendLine(staleMarker)
        appendLine("### ~~$header~~")
        appendLine()
        appendLine("<details><summary>$expandLabel</summary>")
        appendLine()
        appendLine(cleanedOld)
        appendLine()
        appendLine("</details>")
    }

    val (status, _) = ghRequest(
        "PATCH",
        "/repos/$repo/issues/comments/$commentId",
        mapOf("body" to staleBody)
    )

    status.requireSuccess("Error: could not mark comment as stale (HTTP $status), aborting")
}

fun readBuildLog(artifactDirPath: String?): String? {
    if (artifactDirPath == null) return null
    return runCatching {
        Path(artifactDirPath).listDirectoryEntries("*.log").firstOrNull()?.readText()
    }.getOrNull()
}

fun parseOneLiner(logContent: String): String? {
    val workspace = System.getenv("GITHUB_WORKSPACE")
    val lines = logContent.lines()
    val line = lines.firstOrNull { it.trimStart().startsWith("e: ") && "warnings found and -Werror specified" !in it }
        ?: lines.firstOrNull { it.trimStart().startsWith("e: ") }
        ?: lines.firstOrNull { "Received status code" in it }
        ?: lines.firstOrNull { it.trimStart().startsWith("> Could not resolve ") }
        ?: lines.firstOrNull { it.contains("> Task :") && it.trimEnd().endsWith("FAILED") }
    if (line == null || workspace.isNullOrEmpty()) return line
    return line.replace("file://$workspace/", "")
}

fun parseStackTrace(logContent: String): String {
    val startIndex = logContent.indexOf("FAILURE: Build failed with an exception")
    val failureBlock = if (startIndex >= 0) logContent.substring(startIndex) else logContent
    val compilerLines = if (startIndex >= 0) {
        logContent.substring(0, startIndex).lines()
            .filter {
                (it.trimStart().startsWith("e: ") || it.trimStart().startsWith("w: ")) && "warnings found and -Werror specified" !in
                    it
            }
            .joinToString("\n")
    } else ""
    val raw = if (compilerLines.isNotEmpty()) "$compilerLines\n\n$failureBlock" else failureBlock
    return if (raw.length > maxLogChars) raw.take(maxLogChars) + "\n\n... (truncated)" else raw
}


fun isStonecutterOneLiner(oneLiner: String): Boolean {
    if (!oneLiner.startsWith("e: ")) return false
    val path = oneLiner.removePrefix("e: ").substringBefore(" ")
    return "stonecutter" in path
}

fun normalizeOneLiner(oneLiner: String): String {
    if (!oneLiner.startsWith("e: ")) return oneLiner
    val rest = oneLiner.removePrefix("e: ")
    val fileAndPosition = rest.substringBefore(" ").substringAfterLast("/")
    val message = rest.substringAfter(" ", missingDelimiterValue = "")
    return if (message.isEmpty()) "e: $fileAndPosition" else "e: $fileAndPosition $message"
}

fun filterStonecutterDuplicates(versions: List<Pair<String, String?>>): List<Pair<String, String?>> {
    val nonEmpty = versions.filter { !it.second.isNullOrBlank() }
    if (nonEmpty.size != 2) return versions
    val oneLiners = nonEmpty.map { parseOneLiner(it.second!!) }
    if (oneLiners.any { it == null }) return versions
    val (ol1, ol2) = oneLiners.requireNoNulls()
    if (normalizeOneLiner(ol1) != normalizeOneLiner(ol2)) return versions
    // Both versions fail with the same error.
    // Prefer the non-Stonecutter version; if neither is Stonecutter, keep the first.
    val keepIndex = if (isStonecutterOneLiner(ol1) && !isStonecutterOneLiner(ol2)) 1 else 0
    val keep = nonEmpty[keepIndex]
    val combinedLabel = "${nonEmpty[0].first} and ${nonEmpty[1].first}"
    return listOf(combinedLabel to keep.second)
}

fun getJobIdsByVersion(runId: String, versionLabels: List<String>): Map<String, Long> {
    val (status, body) = ghRequest("GET", "/repos/$repo/actions/runs/$runId/jobs?per_page=100")
    if (status.isHttpError) return emptyMap()
    val jobs = (body as? JsonObject)?.get("jobs") as? JsonArray ?: return emptyMap()
    val result = mutableMapOf<String, Long>()
    for (label in versionLabels) {
        val job = jobs.find { it.isJsonObject && it.asJsonObject.get("name")?.asString?.contains(label) == true } ?: continue
        val jobId = job.asJsonObject.get("id")?.asLong ?: continue
        result[label] = jobId
    }
    return result
}

fun buildBuildFailureBody(versions: List<Pair<String, String?>>): String = buildString {
    appendLine(buildMarker)
    val workflowRunId = System.getenv("WORKFLOW_RUN_ID") ?: error("WORKFLOW_RUN_ID not set")
    val headSha = System.getenv("HEAD_SHA") ?: error("HEAD_SHA not set")
    val allVersionParts = versions.flatMap { (v, _) -> v.split(" and ").map { it.trim() } }.distinct()
    val jobIds = getJobIdsByVersion(workflowRunId, allVersionParts)
    for ((version, logContent) in versions) {
        if (logContent.isNullOrBlank()) continue
        appendWarningTitle("Build failed: $version")
        val oneLiner = parseOneLiner(logContent)
        if (oneLiner != null) {
            val displayLine = oneLiner.trim().removePrefix("e: ").removePrefix("w: ").take(300)
            appendLine("`$displayLine`")
            if ("warnings found and -Werror specified" in logContent) {
                appendLine()
                appendLine("_Warning elevated to error by `-Werror`_")
            }
        }
        appendLine()
        val versionParts = version.split(" and ").map { it.trim() }
        versionParts.forEach { part ->
            val jobId = jobIds[part] ?: error("no job id for $part")
            val jobUrl = "https://github.com/$repo/actions/runs/$workflowRunId/job/$jobId"
            val rawUrl = "https://github.com/$repo/commit/$headSha/checks/$jobId/logs"
            appendLine("[$part] \\[[job]($jobUrl)\\] \\[[raw log]($rawUrl)\\]")
        }
        appendLine()
        appendLine("<details><summary>Excerpt</summary>")
        appendLine()
        appendLine("~~~")
        appendLine(parseStackTrace(logContent))
        appendLine("~~~")
        appendLine()
        appendLine("</details>")
        appendLine()
    }
}

fun getMergeableState(prNumber: String): Boolean? {
    val (status, body) = ghRequest("GET", "/repos/$repo/pulls/$prNumber")
    status.requireSuccess("Error: could not fetch PR mergeable state (HTTP $status), aborting")
    val mergeableElement = (body as? JsonObject)?.get("mergeable") ?: return null
    if (mergeableElement.isJsonNull) return null
    return mergeableElement.asBoolean
}

fun getAllOpenPRNumbers(): List<String> {
    val numbers = mutableListOf<String>()
    var page = 1
    while (true) {
        val (status, body) = ghRequest("GET", "/repos/$repo/pulls?state=open&per_page=100&page=$page")
        status.requireSuccess("Error: could not fetch open PRs (HTTP $status), aborting")
        val array = body as? JsonArray ?: error("Error: unexpected response format for open PRs, aborting")
        for (element in array) {
            if (!element.isJsonObject) continue
            val number = element.asJsonObject.get("number")?.takeIf { it.isJsonPrimitive }?.asString ?: continue
            numbers.add(number)
        }
        if (array.size() < 100) break
        page++
    }
    return numbers
}

fun buildConflictBody(): String = buildString {
    appendLine(conflictMarker)
    appendWarningTitle("Merge conflicts detected")
    append("This pull request has conflicts with the base branch. Please resolve them before this PR can be merged.")
}

fun runMergeConflictMode(prNumber: String) {
    val mergeableState = getMergeableState(prNumber)
    if (mergeableState == null) {
        println("PR #$prNumber: mergeable is null, skipping")
        return
    }

    if (!mergeableState) {
        val alreadyLabeled = conflictLabel in getPrLabels(prNumber)
        if (alreadyLabeled) {
            println("PR #$prNumber: conflicts found, already labeled, skipping")
            return
        }
        val existingId = findExistingComment(prNumber, conflictMarker)
        if (existingId != null) markCommentAsStale(
            existingId,
            conflictMarker,
            conflictStaleMarker,
            "Show previous conflicts",
        )
        val (postStatus, _) = ghRequest("POST", "/repos/$repo/issues/$prNumber/comments", mapOf("body" to buildConflictBody()))
        postStatus.requireSuccess("Error: could not post conflict comment (HTTP $postStatus)")
        setLabel(prNumber, conflictLabel, true)
        println("PR #$prNumber: conflicts found, comment posted")
    } else {
        val existingId = findExistingComment(prNumber, conflictMarker)
        if (existingId != null) markCommentAsStale(
            existingId,
            conflictMarker,
            conflictStaleMarker,
            "Show previous conflicts",
        )
        setLabel(prNumber, conflictLabel, false)
        println("PR #$prNumber: no conflicts")
    }
}

fun runDetektMode(prNumber: String) {
    val existingId = findExistingComment(prNumber, detektMarker)
    if (existingId != null) markCommentAsStale(
        existingId,
        detektMarker,
        detektStaleMarker,
        "Show previous warnings"
    )

    val artifactDir = Path(System.getenv("ARTIFACT_DIR") ?: "detekt-artifact")
    val sarifFile = artifactDir / "main.sarif"

    if (!sarifFile.exists()) {
        println("No SARIF found, removing detekt label")
        setLabel(prNumber, detektLabel, false)
        exitProcess(0)
    }

    val workspace = System.getenv("GITHUB_WORKSPACE") ?: ""
    val sarif: JsonObject = runCatching { JsonParser.parseString(sarifFile.readText()).asJsonObject }.getOrElse {
        error("Failed to parse SARIF: ${it.message}")
    }

    val findings = parseSarifFindings(sarif, workspace)

    if (findings.isEmpty()) {
        println("No findings, removing detekt label")
        setLabel(prNumber, detektLabel, false)
        exitProcess(0)
    }

    val (postStatus, _) = ghRequest("POST", "/repos/$repo/issues/$prNumber/comments", mapOf("body" to buildDetektBody(findings)))
    postStatus.requireSuccess("Error: could not post comment (HTTP $postStatus)")
    setLabel(prNumber, detektLabel, true)
    println("Done: ${findings.size} finding(s) posted")
}

fun parseSarifFindings(sarif: JsonObject, workspace: String): List<Finding> = buildList {
    for (run in sarif.getAsJsonArray("runs") ?: JsonArray()) {
        if (!run.isJsonObject) continue
        for (result in run.asJsonObject.getAsJsonArray("results") ?: JsonArray()) {
            if (!result.isJsonObject) continue
            val resultObj = result.asJsonObject
            for (loc in resultObj.getAsJsonArray("locations") ?: JsonArray()) {
                if (!loc.isJsonObject) continue
                val phys = loc.asJsonObject.getAsJsonObject("physicalLocation") ?: continue
                val uri = phys.getAsJsonObject("artifactLocation")?.get("uri")
                    ?.takeIf { it.isJsonPrimitive }?.asString ?: continue
                val region = phys.getAsJsonObject("region") ?: JsonObject()
                val line = region.get("startLine")
                    ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
                    ?.asInt?.takeIf { it > 0 } ?: 1
                val ruleId = resultObj.get("ruleId")
                    ?.takeIf { it.isJsonPrimitive }?.asString ?: "Unknown"
                val message = resultObj.getAsJsonObject("message")?.get("text")
                    ?.takeIf { it.isJsonPrimitive }?.asString ?: ""
                val path = normalizePath(uri, workspace)
                add(Finding(path, line, ruleId, message))
            }
        }
    }
}

fun runBuildMode(prNumber: String) {
    val log1 = readBuildLog(System.getenv("ARTIFACT_DIR_1"))
    val log2 = readBuildLog(System.getenv("ARTIFACT_DIR_2"))

    val existingId = findExistingComment(prNumber, buildMarker)

    if (log1.isNullOrBlank() && log2.isNullOrBlank()) {
        println("No build failures found, removing build label")
        if (existingId != null) markCommentAsStale(
            existingId,
            buildMarker,
            buildStaleMarker,
            "Show previous errors"
        )
        setLabel(prNumber, buildLabel, false)
        exitProcess(0)
    }

    if (existingId != null) markCommentAsStale(
        existingId,
        buildMarker,
        buildStaleMarker,
        "Show previous errors"
    )

    val versions = filterStonecutterDuplicates(listOf("1.21.11" to log1, "26.1" to log2))
    val (postStatus, _) = ghRequest("POST", "/repos/$repo/issues/$prNumber/comments", mapOf("body" to buildBuildFailureBody(versions)))
    postStatus.requireSuccess("Error: could not post build failure comment (HTTP $postStatus)")
    setLabel(prNumber, buildLabel, true)
    println("Done: build failure comment posted, added label")
}

fun readChangelogErrors(artifactDirPath: String?): String? {
    if (artifactDirPath == null) return null
    return runCatching {
        Path(artifactDirPath).toFile().walkTopDown()
            .firstOrNull { it.isFile && it.name == "changelog_errors.txt" }
            ?.readText()?.takeIf { it.isNotBlank() }
    }.getOrNull()
}

fun buildChangelogBody(errors: String): String = buildString {
    appendLine(changelogMarker)
    appendWarningTitle("Changelog verification failed")
    appendLine()
    append(errors.trimEnd())
}

fun runChangelogMode(prNumber: String) {
    val workflowConclusion = System.getenv("WORKFLOW_CONCLUSION") ?: ""
    val existingId = findExistingComment(prNumber, changelogMarker)

    fun staleExisting() {
        val id = existingId ?: return
        markCommentAsStale(id, changelogMarker, changelogStaleMarker, "Show previous issues")
    }

    if (workflowConclusion == "success") {
        println("Changelog check passed, cleaning up")
        staleExisting()
        setLabel(prNumber, changelogLabel, false)
        exitProcess(0)
    }

    val errors = readChangelogErrors(System.getenv("ARTIFACT_DIR"))
        ?: error("Artifact missing - changelog step likely failed before artifact upload")

    staleExisting()

    val (postStatus, _) = ghRequest(
        "POST",
        "/repos/$repo/issues/$prNumber/comments",
        mapOf("body" to buildChangelogBody(errors)),
    )
    postStatus.requireSuccess("Error: could not post changelog comment (HTTP $postStatus)")
    setLabel(prNumber, changelogLabel, true)
    println("Done: changelog check comment posted")
}

fun parseDependencies(body: String): List<Dependency> {
    val repoOwner = repo.substringBefore("/")
    val repoName = repo.substringAfter("/")
    val deps = mutableListOf<Dependency>()

    val urlRegex = Regex("""- https://github\.com/([\w-]+)/([\w-]+)/pull/(\d+)""")
    for (match in urlRegex.findAll(body)) {
        val depOwner = match.groupValues[1]
        val depRepo = match.groupValues[2]
        val depNum = match.groupValues[3].toInt()
        if (depOwner == "hannibal002" && depRepo == "SkyHanni-REPO") continue
        deps.add(Dependency(depOwner, depRepo, depNum))
    }

    val prRegex = Regex("""- #(\d+)""")
    for (match in prRegex.findAll(body)) {
        deps.add(Dependency(repoOwner, repoName, match.groupValues[1].toInt()))
    }

    return deps
}

fun isDependencyOpen(dep: Dependency): Boolean {
    val (status, body) = ghRequest("GET", "/repos/${dep.owner}/${dep.repoName}/pulls/${dep.pullNumber}")
    if (status == 404) {
        System.err.println("Warning: dependency ${dep.owner}/${dep.repoName}#${dep.pullNumber} not found, skipping")
        return false
    }
    if (status.isHttpError) {
        error("Error: unexpected status $status for dependency ${dep.owner}/${dep.repoName}#${dep.pullNumber}")
    }
    val state = (body as? JsonObject)?.get("state")?.takeIf { it.isJsonPrimitive }?.asString
    return state == "open"
}


fun checkPrDependencies(issueNumber: String): Boolean {
    val (status, body) = ghRequest("GET", "/repos/$repo/pulls/$issueNumber")
    if (status.isHttpError) {
        error("Error: could not fetch PR #$issueNumber (HTTP $status)")
    }
    val prBody = (body as? JsonObject)?.get("body")?.takeIf { !it.isJsonNull }?.asString ?: ""

    if ("## Dependencies" !in prBody) {
        println("PR #$issueNumber: no Dependencies section, skipping")
        return false
    }

    val deps = parseDependencies(prBody)
    if (deps.isEmpty()) {
        println("PR #$issueNumber: no dependency links found, skipping")
        return false
    }

    val openDeps = deps.filter { isDependencyOpen(it) }
    val hasOpen = openDeps.isNotEmpty()
    val wasAlreadyLabeled = dependencyLabel in getPrLabels(issueNumber)
    setLabel(issueNumber, dependencyLabel, hasOpen)
    if (hasOpen && !wasAlreadyLabeled) {
        postDependencyWaitingComment(issueNumber, openDeps)
    }
    println("PR #$issueNumber: ${if (hasOpen) "has open dependencies" else "all dependencies resolved"}")
    return hasOpen
}

fun fetchAllLabeledOpenPRs(): List<JsonObject> {
    val result = mutableListOf<JsonObject>()
    val encoded = URLEncoder.encode(dependencyLabel, StandardCharsets.UTF_8).replace("+", "%20")
    var page = 1
    while (true) {
        val (status, body) = ghRequest("GET", "/repos/$repo/issues?labels=$encoded&state=open&per_page=100&page=$page")
        if (status.isHttpError) {
            error("Error: could not fetch labeled PRs (HTTP $status)")
        }
        val array = body as? JsonArray ?: break
        for (element in array) {
            val obj = element as? JsonObject ?: continue
            if (obj.get("pull_request") == null) continue
            result.add(obj)
        }
        if (array.size() < 100) break
        page++
    }
    return result
}


fun fetchAllOpenPRs(): List<JsonObject> {
    val result = mutableListOf<JsonObject>()
    var page = 1
    while (true) {
        val (status, body) = ghRequest("GET", "/repos/$repo/issues?state=open&per_page=100&page=$page")
        if (status.isHttpError) {
            error("Error: could not fetch open PRs (HTTP $status)", commentError = false)
        }
        val array = body as? JsonArray ?: break
        for (element in array) {
            val obj = element as? JsonObject ?: continue
            if (obj.get("pull_request") == null) continue
            result.add(obj)
        }
        if (array.size() < 100) break
        page++
    }
    return result
}

fun recheckPRsDependingOn(targetPrNum: Int) {
    val repoOwner = repo.substringBefore("/")
    val repoName = repo.substringAfter("/")
    println("Rechecking all open PRs that depend on #$targetPrNum")
    for (pr in fetchAllOpenPRs()) {
        val num = pr.get("number")?.takeIf { it.isJsonPrimitive }?.asString ?: continue
        if (num.toIntOrNull() == targetPrNum) continue
        val body = pr.get("body")?.takeIf { !it.isJsonNull }?.asString ?: ""
        if ("## Dependencies" !in body) continue
        val deps = parseDependencies(body)
        if (deps.any { it.owner == repoOwner && it.repoName == repoName && it.pullNumber == targetPrNum }) {
            checkPrDependencies(num)
        }
    }
}

fun buildDependencyWaitingComment(deps: List<Dependency>): String = buildString {
    if (deps.size == 1) {
        val dep = deps.first()
        val link = "https://github.com/${dep.owner}/${dep.repoName}/pull/${dep.pullNumber}"
        appendLine("This PR is now waiting on [#${dep.pullNumber}]($link).")
    } else {
        appendLine("This PR is now waiting on the following dependencies:")
        for (dep in deps) {
            val link = "https://github.com/${dep.owner}/${dep.repoName}/pull/${dep.pullNumber}"
            appendLine("- [#${dep.pullNumber}]($link)")
        }
    }
}

fun postDependencyWaitingComment(prNum: String, openDeps: List<Dependency>) {
    val message = buildDependencyWaitingComment(openDeps)
    val (status, _) = ghRequest("POST", "/repos/$repo/issues/$prNum/comments", mapOf("body" to message))
    if (status.isHttpError) System.err.println("Warning: could not post dependency waiting comment on PR #$prNum (HTTP $status)")
    else println("PR #$prNum: posted dependency waiting comment")
}

fun postDependencyNotification(prNum: String, closedPrNum: Int, merged: Boolean, remainingOpen: Int) {
    val message = buildDependencyNotificationMessage(closedPrNum, merged, remainingOpen)
    val (status, _) = ghRequest("POST", "/repos/$repo/issues/$prNum/comments", mapOf("body" to message))
    if (status.isHttpError) System.err.println("Warning: could not post notification on PR #$prNum (HTTP $status)")
    else println("PR #$prNum: posted dependency notification")
}

fun buildDependencyNotificationMessage(closedPrNum: Int, merged: Boolean, remainingOpen: Int): String = buildString {
    val closedLink = "https://github.com/$repo/pull/$closedPrNum"
    if (merged) {
        append("[PR #$closedPrNum]($closedLink) was merged.")
        when (remainingOpen) {
            0 -> append(" This PR now has all dependencies resolved.")
            1 -> append(" This PR still has 1 open dependency.")
            else -> append(" This PR still has $remainingOpen open dependencies.")
        }
    } else {
        append("[PR #$closedPrNum]($closedLink) was closed without merging.")
        append(" You may need to re-evaluate this PR's dependencies.")
    }
}

fun runDependenciesMode(prState: String, prNum: String?, merged: Boolean): Boolean {
    if (prState != "closed") {
        val num = prNum ?: run { println("PR_NUMBER not set, skipping"); return false }
        val hasOpenDeps = checkPrDependencies(num)
        val prAction = System.getenv("PR_ACTION") ?: ""
        if (prAction == "reopened") {
            val targetPrNum = num.toIntOrNull()
            if (targetPrNum != null) recheckPRsDependingOn(targetPrNum)
        }
        return hasOpenDeps
    }

    println("PR ${prNum ?: "unknown"} closed (merged=$merged), rechecking all open PRs with label \"$dependencyLabel\"")
    val closedPrNum = prNum?.toIntOrNull() ?: error("PR_NUMBER not set or invalid for closed event")
    val repoOwner = repo.substringBefore("/")
    val repoName = repo.substringAfter("/")

    for (pr in fetchAllLabeledOpenPRs()) {
        val num = pr.get("number")?.takeIf { it.isJsonPrimitive }?.asString ?: continue
        val body = pr.get("body")?.takeIf { !it.isJsonNull }?.asString ?: ""

        if ("## Dependencies" !in body) continue

        val deps = parseDependencies(body)
        if (deps.isEmpty()) continue

        val isDirectDep = deps.any { it.owner == repoOwner && it.repoName == repoName && it.pullNumber == closedPrNum }
        val remainingDeps = deps.filter { !(it.owner == repoOwner && it.repoName == repoName && it.pullNumber == closedPrNum) }
        val openRemainingCount = remainingDeps.count { isDependencyOpen(it) }

        if (isDirectDep) postDependencyNotification(num, closedPrNum, merged, openRemainingCount)
        setLabel(num, dependencyLabel, openRemainingCount > 0)
    }
    return false
}

val prNumberEnv: String? = System.getenv("PR_NUMBER")?.takeIf { it.isNotEmpty() }

if (mode == "mergeconflict") {
    if (prNumberEnv != null) {
        runMergeConflictMode(prNumberEnv)
    } else {
        println("No PR_NUMBER set, rechecking all open PRs")
        getAllOpenPRNumbers().forEach { runMergeConflictMode(it) }
    }
    exitProcess(0)
}

val prNumber: String = prNumberEnv ?: run { println("PR_NUMBER not set, skipping"); exitProcess(0) }

if (mode == "dependencies") {
    val prState = System.getenv("PR_STATE") ?: error("PR_STATE not set")
    val prMerged = System.getenv("PR_MERGED") == "true"
    val hasOpenDeps = runDependenciesMode(prState, prNumberEnv, prMerged)
    exitProcess(if (hasOpenDeps) 1 else 0)
}

when (mode) {
    "detekt" -> runDetektMode(prNumber)
    "build" -> runBuildMode(prNumber)
    "changelog" -> runChangelogMode(prNumber)
    else -> error("Unsupported MODE: $mode")
}

