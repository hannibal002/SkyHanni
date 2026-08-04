@file:DependsOn("com.google.code.gson:gson:2.10.1")
// Execution context: base branch
// called from detekt-review.yml, build-review.yml, label-merge-conflict.yml, changelog-review.yml, check_dependencies.yml,
// and keyword-labels.yml

import com.google.gson.*
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import kotlin.io.path.*
import kotlin.system.exitProcess


/**
 * A keyword an author can put on its own line in the pull request description to control a label.
 * The line has to match exactly, the same way ChangelogVerification handles "exclude_from_changelog".
 *
 * [description] explains the label in the comment posted when it gets added.
 * [blocking] additionally publishes a commit status, so the pull request cannot be merged while the keyword is present.
 */
data class KeywordLabel(
    val keyword: String,
    val label: String,
    val description: String,
    val blocking: Boolean,
)

// The label of every blocking entry doubles as its status context and is also used by the set-pending job in
// keyword-labels.yml, both must stay in sync.
val keywordLabels = listOf(
    KeywordLabel(
        keyword = "waiting_on_hypixel_alpha",
        label = "Waiting on Hypixel",
        description = "The relevant feature is only available on the Hypixel alpha server, so this pull request can " +
            "only be tested there. It must not be merged before the feature reaches the main server.",
        blocking = true,
    ),
)

val workflowFailedMarker = "<!-- workflow-failed -->"


/**
 * Identifies a comment posted by this script, so a later run finds its own previous comment again.
 *
 * [markerId] is a permanent identifier. The comments carrying it outlive every run, so changing it orphans
 * every marker sitting on every currently open pull request. Never derive it from something that can be
 * renamed.
 * [expandLabel] names the spoiler an outdated comment is collapsed into.
 *
 * A mode that only ever announces one direction uses [marker], because the resolved case posts nothing and the
 * presence of the marker is the whole information. A mode that announces both directions uses [stateMarker],
 * because there the presence of a marker cannot tell which direction was announced.
 */
data class CommentType(val markerId: String, val expandLabel: String) {
    val marker: String = "<!-- $markerId -->"

    // Deliberately a suffix and not a colon, so a collapsed comment no longer matches stateMarkerRegex.
    val staleMarker: String = "<!-- $markerId-stale -->"

    // Requires the closing arrow and a restricted character set, so arbitrary text in a comment body cannot be
    // read as an announced state.
    val stateMarkerRegex: Regex = Regex("""<!-- ${Regex.escape(markerId)}:([a-z0-9_-]+) -->""")

    fun stateMarker(state: String): String = "<!-- $markerId:$state -->"
}

val detektLabel = "Detekt"
val detektComment = CommentType("detekt-review", "Show previous warnings")

val buildLabel = "Fails Multi-Version"
val buildComment = CommentType("build-failure-review", "Show previous errors")

val conflictLabel = "Merge Conflicts"
val conflictComment = CommentType("merge-conflict-review", "Show previous conflicts")

val changelogLabel = "Wrong Title/Changelog"
val changelogComment = CommentType("changelog-check-review", "Show previous issues")

val dependencyLabel = "Waiting on Dependency PR"
// Also used by the set-pending job in check_dependencies.yml, both must stay in sync.
val dependencyStatusContext = "Check PR Dependencies"

val warningIcon = "⚠\uFE0F"

val maxDirectFindings = 15
val maxLogChars = 10_000

val maxRequestAttempts = 3
val retryDelayMillis = 2_000L
val retryStatusCodes = setOf(502, 503, 504)

// POST endpoints that end in the same state no matter how often they are sent. Adding a label twice is a no-op,
// and only the newest commit status per context is ever shown. Creating a comment is deliberately not on this
// list: a gateway timeout that the server applied anyway would post the same comment twice on retry.
val idempotentPostEndpoints = listOf("labels", "statuses")

val repo: String = System.getenv("GITHUB_REPOSITORY") ?: error("GITHUB_REPOSITORY not set")
val token: String = System.getenv("GH_TOKEN") ?: error("GH_TOKEN not set")
val mode: String = System.getenv("MODE") ?: error("MODE not set")

val httpClient: HttpClient = HttpClient.newHttpClient()
val gson = Gson()

var errorCommentPosted = false

fun error(message: String, commentError: Boolean = true): Nothing {
    System.err.println(message)
    if (commentError && !errorCommentPosted) {
        postPrComment(
            prNumber = prNumber,
            body = buildErrorComment(message),
            commentError = false
        ) { "Error: could not post workflow error as comment (HTTP $it)" }
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

    appendLine("Mode:")
    appendLine(mode)
    appendLine()

    appendLine("Most likely fix:")
    val theSecretFix = "merge the beta branch into this PR."
    appendLine(theSecretFix)
    appendLine()

    appendLine("If the issue persists, please ping a maintainer on [SkyHanni Discord](https://discord.gg/skyhanni-997079228510117908).")
    appendLine()

    val runId = System.getenv("GITHUB_RUN_ID")
    if (runId != null) {
        val runLink = " \\[[workflow run](https://github.com/$repo/actions/runs/$runId)\\]"
        appendLine("For investigating this error, see $runLink")
    } else {
        appendLine("GITHUB_RUN_ID is null, good luck finding the issue ;)")
    }

}

val Int.isHttpError: Boolean get() = this !in 200..299

fun Int.requireSuccess(message: String, commentError: Boolean = true) {
    if (isHttpError) error(message, commentError)
}

data class Finding(val path: String, val line: Int, val ruleId: String, val message: String)

data class Dependency(val owner: String, val repoName: String, val pullNumber: Int)

data class DependencyCheckResult(
    val dependencies: List<Dependency>,
    val openDependencies: List<Dependency>,
)

class DependencyCheckException(message: String) : Exception(message)

// PRs whose dependency check could not be completed. Collected instead of aborting, so one unreachable
// PR cannot stop the remaining ones from getting their label and status updated.
val failedDependencyChecks = mutableListOf<String>()

fun dependencyError(message: String): Nothing = throw DependencyCheckException(message)

fun sendGhRequest(request: HttpRequest): Pair<Int, JsonElement> {
    val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    val body = runCatching { JsonParser.parseString(response.body()) }.getOrDefault(JsonNull.INSTANCE)
    return response.statusCode() to body
}

fun buildGhRequest(method: String, path: String, payload: Any?): HttpRequest {
    val bodyPublisher = if (payload != null)
        HttpRequest.BodyPublishers.ofString(gson.toJson(payload))
    else
        HttpRequest.BodyPublishers.noBody()

    return HttpRequest.newBuilder()
        .uri(URI.create("https://api.github.com$path"))
        .header("Authorization", "Bearer $token")
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .header("Content-Type", "application/json")
        .method(method, bodyPublisher)
        .build()
}

// Retrying a request must not change the outcome when the server already applied it. Every GET, PATCH and
// DELETE this script sends is a read, an edit of one known comment, or a label removal, and repeating any of
// them lands in the same state. POST creates something, so it is only retried for the endpoints listed as
// idempotent. The decision lives here instead of at the call site, so a newly added POST cannot inherit the
// retry by accident. An endpoint missing from the list only loses its retry, which is harmless, while the
// opposite default would silently duplicate a comment.
fun isRetryable(method: String, path: String): Boolean {
    if (method != "POST") return true
    // Whole path segments only, and only those behind the "/repos/<owner>/<name>" prefix, so neither the
    // repository name nor any other interpolated value can be mistaken for an endpoint name.
    val segments = path.substringBefore("?")
        .removePrefix("/repos/$repo")
        .split("/")
        .filter { it.isNotEmpty() }
    return segments.any { it in idempotentPostEndpoints }
}

fun ghRequest(method: String, path: String, payload: Any? = null): Pair<Int, JsonElement> {
    val request = buildGhRequest(method, path, payload)
    if (!isRetryable(method, path)) return sendGhRequest(request)

    // The GitHub API answers with 502/503/504 every now and then. Those are transient, so a single one
    // must not fail the whole workflow run. The last attempt returns whatever it gets.
    repeat(maxRequestAttempts - 1) { index ->
        val attempt = index + 1
        val reason = try {
            val result = sendGhRequest(request)
            if (result.first !in retryStatusCodes) return result
            "HTTP ${result.first}"
        } catch (e: IOException) {
            e.toString()
        }
        System.err.println("Warning: $method $path failed ($reason), retry $attempt of ${maxRequestAttempts - 1}")
        Thread.sleep(retryDelayMillis * attempt)
    }
    return sendGhRequest(request)
}


fun ghRepoGet(path: String): Pair<Int, JsonElement> = ghRequest("GET", "/repos/$repo$path")

fun postComment(prNumber: String, body: String): Int {
    val (status, _) = ghRequest("POST", "/repos/$repo/issues/$prNumber/comments", mapOf("body" to body))
    return status
}

fun postPrComment(prNumber: String, body: String, commentError: Boolean = true, errorMessage: (Int) -> String) {
    val status = postComment(prNumber, body)
    status.requireSuccess(errorMessage(status), commentError)
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
    val (status, body) = ghRepoGet("/issues/$prNumber/labels")
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
    appendWarningTitle("Detekt found ${findings.size} ${if (findings.size == 1) "issue" else "issues"}")
    appendLine("")
    val direct = findings.take(maxDirectFindings)
    val overflow = findings.drop(maxDirectFindings)
    appendCompact(direct)
    if (overflow.isNotEmpty()) {
        appendLine("\n<details><summary>${overflow.size} more ${if (overflow.size == 1) "issue" else "issues"}</summary>\n")
        appendCompact(overflow)
        appendLine("\n</details>")
    }
    appendLine("\n<details><summary>More Details</summary>\n")
    appendFull(findings)
    appendLine("\n</details>")
}

fun StringBuilder.appendCompact(findings: List<Finding>) {
    for (finding in findings) {
        val fileName = finding.path.substringAfterLast('/')
        val message = sanitize(finding.message)
        val className = sanitize(fileName)
        val line = finding.line
        appendLine("- ```$className:$line```: $message")
    }
}

fun StringBuilder.appendFull(findings: List<Finding>) {
    for (finding in findings) {
        val fileName = finding.path.substringAfterLast('/')
        val ruleId = sanitize(finding.ruleId)
        val message = sanitize(finding.message)
        val className = sanitize(fileName)
        val line = finding.line
        val path = sanitize(finding.path)
        appendLine("- ```$className:$line```")
        appendLine("  message: `$message`")
        appendLine("  rule: `$ruleId`")
        appendLine("  path: `$path`")
        appendLine()
    }
}

data class PrComment(val id: Long, val body: String)

data class StateComment(val comment: PrComment, val state: String)

// Iterates every comment of a pull request, oldest first, which is the order the API documents. [action]
// returns false to stop early.
fun forEachComment(prNumber: String, action: (PrComment) -> Boolean) {
    var page = 1
    while (true) {
        val (status, body) = ghRepoGet("/issues/$prNumber/comments?per_page=100&page=$page")
        status.requireSuccess("Error: could not fetch PR comments (HTTP $status), aborting")
        val array = body as? JsonArray ?: error("Error: unexpected response format for PR comments, aborting")
        for (element in array) {
            val obj = element as? JsonObject ?: continue
            val id = obj.get("id")?.takeIf { it.isJsonPrimitive }?.asLong ?: continue
            val commentBody = obj.get("body")?.takeIf { it.isJsonPrimitive }?.asString ?: continue
            if (!action(PrComment(id, commentBody))) return
        }
        if (array.size() < 100) return
        page++
    }
}

// Whole line instead of substring. Substring matching happens to be safe for the marker strings in use, but a
// newly added marker id that is a prefix of another one would break it silently.
fun String.hasMarkerLine(marker: String): Boolean = lineSequence().any { it.trim() == marker }

fun CommentType.findExisting(prNumber: String): PrComment? {
    var found: PrComment? = null
    forEachComment(prNumber) { comment ->
        val matches = comment.body.hasMarkerLine(marker)
        if (matches) found = comment
        !matches
    }
    return found
}

// Walks every page and keeps the last hit, because the issue specific comments endpoint accepts only since,
// per_page and page. There is no way to ask for the newest comment directly.
fun CommentType.findNewestState(prNumber: String): StateComment? {
    var newest: StateComment? = null
    forEachComment(prNumber) { comment ->
        val state = comment.body.lineSequence()
            .firstNotNullOfOrNull { stateMarkerRegex.matchEntire(it.trim()) }
            ?.groupValues?.get(1)
        if (state != null) newest = StateComment(comment, state)
        true
    }
    return newest
}

fun CommentType.post(prNumber: String, marker: String, body: String, errorMessage: (Int) -> String) {
    postPrComment(prNumber, "$marker\n$body", errorMessage = errorMessage)
}

fun CommentType.post(prNumber: String, body: String, errorMessage: (Int) -> String) {
    post(prNumber, marker, body, errorMessage)
}


// A collapsed comment loses its active marker, including the state variant. Only the stale marker remains, so
// it can never be mistaken for the current announcement.
fun CommentType.markAsStale(comment: PrComment, fallbackTitle: String = "Unknown") {
    val cleanedOld = comment.body
        .lineSequence()
        .filterNot { it.trim() == marker || stateMarkerRegex.matches(it.trim()) }
        .joinToString("\n")
        .trim()

    val header = cleanedOld
        .lineSequence()
        .firstOrNull { it.startsWith("### ") }
        ?.removePrefix("###")
        ?.replace(warningIcon, "")
        ?.trim()
        ?: fallbackTitle

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

    val (status, _) = ghRequest("PATCH", "/repos/$repo/issues/comments/${comment.id}", mapOf("body" to staleBody))

    status.requireSuccess("Error: could not mark comment as stale (HTTP $status), aborting")
}

fun CommentType.staleExisting(prNumber: String, fallbackTitle: String = "Unknown") {
    val existing = findExisting(prNumber) ?: return
    markAsStale(existing, fallbackTitle)
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
        ?: lines.firstOrNull { ": error:" in it && ".java:" in it }
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

fun parseAllErrors(logContent: String): List<String> =
    logContent.lines()
        .filter {
            (it.trimStart().startsWith("e: ") || (": error:" in it && ".java:" in it)) &&
                "warnings found and -Werror specified" !in it
        }
        .map { it.trim() }
        .distinct()

fun parseErrorContinuations(logContent: String, errorLine: String): List<String> {
    val lines = logContent.lines()
    val idx = lines.indexOf(errorLine)
    if (idx < 0) return emptyList()
    val result = mutableListOf<String>()
    var i = idx + 1
    while (i < lines.size) {
        val next = lines[i]
        if (next.isBlank()) break
        if (next.trimStart().startsWith("e: ") || next.trimStart().startsWith("w: ")) break
        if (next.startsWith("> ") || next.startsWith("FAILURE") || next.startsWith("*")) break
        result.add(next.trim())
        i++
    }
    return result
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
    val (status, body) = ghRepoGet("/actions/runs/$runId/jobs?per_page=100")
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
    val workflowRunId = System.getenv("WORKFLOW_RUN_ID") ?: error("WORKFLOW_RUN_ID not set")
    val headSha = System.getenv("HEAD_SHA") ?: error("HEAD_SHA not set")
    val allVersionParts = versions.flatMap { (v, _) -> v.split(" and ").map { it.trim() } }.distinct()
    val jobIds = getJobIdsByVersion(workflowRunId, allVersionParts)
    for ((version, logContent) in versions) {
        if (logContent.isNullOrBlank()) continue
        appendWarningTitle("Build failed: $version")
        val workspace = System.getenv("GITHUB_WORKSPACE") ?: ""
        val rawErrorLines = parseAllErrors(logContent)
        if (rawErrorLines.isNotEmpty()) {
            for (rawLine in rawErrorLines.take(5)) {
                val display = rawLine.trimStart().removePrefix("e: ")
                    .let {
                        if (workspace.isNotEmpty()) it.replace("file://$workspace/", "").replace("$workspace/", "")
                        else it
                    }
                    .take(300)
                appendLine("- `$display`")
                if (rawLine.trimStart().startsWith("e: ")) {
                    for (cont in parseErrorContinuations(logContent, rawLine)) {
                        appendLine("  - `${cont.take(300)}`")
                    }
                }
            }
            if (rawErrorLines.size > 5) appendLine("_...and ${rawErrorLines.size - 5} more_")
        } else {
            val oneLiner = parseOneLiner(logContent)
            if (oneLiner != null) {
                val displayLine = oneLiner.trim().removePrefix("e: ").removePrefix("w: ").take(300)
                appendLine("`$displayLine`")
            }
        }
        if ("warnings found and -Werror specified" in logContent) {
            appendLine()
            appendLine("_Warning elevated to error by `-Werror`_")
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
    val (status, body) = ghRepoGet("/pulls/$prNumber")
    status.requireSuccess("Error: could not fetch PR mergeable state (HTTP $status), aborting")
    val mergeableElement = (body as? JsonObject)?.get("mergeable") ?: return null
    if (mergeableElement.isJsonNull) return null
    return mergeableElement.asBoolean
}

fun getAllOpenPRNumbers(): List<String> {
    val numbers = mutableListOf<String>()
    var page = 1
    while (true) {
        val (status, body) = ghRepoGet("/pulls?state=open&per_page=100&page=$page")
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
        conflictComment.staleExisting(prNumber)
        conflictComment.post(prNumber, buildConflictBody()) { "Error: could not post conflict comment (HTTP $it)" }
        setLabel(prNumber, conflictLabel, true)
        println("PR #$prNumber: conflicts found, comment posted")
    } else {
        conflictComment.staleExisting(prNumber)
        setLabel(prNumber, conflictLabel, false)
        println("PR #$prNumber: no conflicts")
    }
}

fun buildDetektCrashBody(logContent: String): String = buildString {
    appendWarningTitle("Detekt could not run")
    appendLine()
    val oneLiner = parseOneLiner(logContent)
    if (oneLiner != null) {
        val displayLine = oneLiner.trim().removePrefix("e: ").removePrefix("w: ").take(300)
        appendLine("`$displayLine`")
        appendLine()
    }
    appendLine("<details><summary>Excerpt</summary>")
    appendLine()
    appendLine("~~~")
    appendLine(parseStackTrace(logContent))
    appendLine("~~~")
    appendLine()
    appendLine("</details>")
}

fun runDetektMode(prNumber: String) {
    detektComment.staleExisting(prNumber)

    val artifactDir = Path(System.getenv("ARTIFACT_DIR") ?: "detekt-artifact")
    val sarifFile = artifactDir / "main.sarif"

    if (!sarifFile.exists()) {
        val conclusion = System.getenv("WORKFLOW_CONCLUSION")
            ?: error("WORKFLOW_CONCLUSION is not set")
        if (conclusion != "success") {
            val logFile = artifactDir / "detekt-run.log"
            val logContent = runCatching { logFile.takeIf { it.exists() }?.readText() }.getOrNull()
            if (!logContent.isNullOrBlank()) {
                val body = buildDetektCrashBody(logContent)
                detektComment.post(prNumber, body) { "Error: could not post workflow error as comment (HTTP $it)" }
                println("Detekt workflow did not complete successfully; posted explanatory comment")
                exitProcess(0)
            } else {
                error(
                    "Detekt workflow did not complete successfully AND detekt-run.log does not exist, is null or empty. " +
                        "(conclusion: $conclusion). " +
                        "Check the workflow run for details."
                )
            }
        }
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

    detektComment.post(prNumber, buildDetektBody(findings)) { "Error: could not post comment (HTTP $it)" }
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

    buildComment.staleExisting(prNumber)

    if (log1.isNullOrBlank() && log2.isNullOrBlank()) {
        println("No build failures found, removing build label")
        setLabel(prNumber, buildLabel, false)
        exitProcess(0)
    }

    val versions = filterStonecutterDuplicates(listOf("1.21.11" to log1, "26.1" to log2))
    buildComment.post(prNumber, buildBuildFailureBody(versions)) {
        "Error: could not post build failure comment (HTTP $it)"
    }
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
    appendWarningTitle("Changelog verification failed")
    appendLine()
    append(errors.trimEnd())
}

fun runChangelogMode(prNumber: String) {
    val workflowConclusion = System.getenv("WORKFLOW_CONCLUSION") ?: ""

    if (workflowConclusion == "success") {
        println("Changelog check passed, cleaning up")
        changelogComment.staleExisting(prNumber)
        setLabel(prNumber, changelogLabel, false)
        exitProcess(0)
    }

    val errors = readChangelogErrors(System.getenv("ARTIFACT_DIR"))
        ?: error("Artifact missing - changelog step likely failed before artifact upload")

    changelogComment.staleExisting(prNumber)

    changelogComment.post(prNumber, buildChangelogBody(errors)) { "Error: could not post changelog comment (HTTP $it)" }
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
        dependencyError("Error: unexpected status $status for dependency ${dep.owner}/${dep.repoName}#${dep.pullNumber}")
    }
    val state = (body as? JsonObject)?.get("state")?.takeIf { it.isJsonPrimitive }?.asString
    return state == "open"
}

fun setDependencyStatus(headSha: String, openDependencies: List<Dependency>) {
    val hasOpenDependencies = openDependencies.isNotEmpty()
    val description = when (openDependencies.size) {
        0 -> "All dependency PRs are resolved"
        1 -> "Waiting on 1 dependency PR"
        else -> "Waiting on ${openDependencies.size} dependency PRs"
    }
    val payload = mutableMapOf<String, Any>(
        "state" to if (hasOpenDependencies) "failure" else "success",
        "context" to dependencyStatusContext,
        "description" to description,
    )
    val runId = System.getenv("GITHUB_RUN_ID")
    if (runId != null) payload["target_url"] = "https://github.com/$repo/actions/runs/$runId"

    val (status, _) = ghRequest("POST", "/repos/$repo/statuses/$headSha", payload)
    if (status.isHttpError) {
        dependencyError("Error: could not update dependency status for $headSha (HTTP $status)")
    }
}

// Throws DependencyCheckException when this PR could not be evaluated. Callers that iterate over many PRs
// must use checkPrDependenciesOrNull instead.
fun checkPrDependencies(issueNumber: String): DependencyCheckResult {
    val (status, body) = ghRepoGet("/pulls/$issueNumber")
    if (status.isHttpError) {
        dependencyError("Error: could not fetch PR #$issueNumber (HTTP $status)")
    }
    val pr = body as? JsonObject ?: dependencyError("Error: unexpected response format for PR #$issueNumber")
    val prBody = pr.get("body")?.takeIf { !it.isJsonNull }?.asString ?: ""

    val headSha = (pr.get("head") as? JsonObject)?.get("sha")?.takeIf { it.isJsonPrimitive }?.asString
        ?: dependencyError("Error: head SHA missing for PR #$issueNumber")

    if ("## Dependencies" !in prBody) {
        println("PR #$issueNumber: no Dependencies section, skipping")
        setLabel(issueNumber, dependencyLabel, false)
        setDependencyStatus(headSha, emptyList())
        return DependencyCheckResult(emptyList(), emptyList())
    }

    val deps = parseDependencies(prBody)
    if (deps.isEmpty()) {
        println("PR #$issueNumber: no dependency links found, skipping")
        setLabel(issueNumber, dependencyLabel, false)
        setDependencyStatus(headSha, emptyList())
        return DependencyCheckResult(emptyList(), emptyList())
    }

    val openDeps = deps.filter { isDependencyOpen(it) }
    val hasOpen = openDeps.isNotEmpty()
    val wasAlreadyLabeled = dependencyLabel in getPrLabels(issueNumber)
    setLabel(issueNumber, dependencyLabel, hasOpen)
    setDependencyStatus(headSha, openDeps)
    if (hasOpen && !wasAlreadyLabeled) {
        postDependencyWaitingComment(issueNumber, openDeps)
    }
    println("PR #$issueNumber: ${if (hasOpen) "has open dependencies" else "all dependencies resolved"}")
    return DependencyCheckResult(deps, openDeps)
}


fun skipFailedDependencyCheck(issueNumber: String, reason: String): DependencyCheckResult? {
    System.err.println("Warning: could not evaluate dependencies of PR #$issueNumber ($reason), skipping")
    failedDependencyChecks.add(issueNumber)
    return null
}

// A dropped connection has to be treated like a repeated gateway timeout, otherwise a transport failure
// still aborts the whole loop while an HTTP failure does not.
fun checkPrDependenciesOrNull(issueNumber: String): DependencyCheckResult? = try {
    checkPrDependencies(issueNumber)
} catch (e: DependencyCheckException) {
    skipFailedDependencyCheck(issueNumber, e.message ?: e.toString())
} catch (e: IOException) {
    skipFailedDependencyCheck(issueNumber, e.toString())
}


fun fetchAllLabeledOpenPRs(): List<JsonObject> {
    val result = mutableListOf<JsonObject>()
    val encoded = URLEncoder.encode(dependencyLabel, StandardCharsets.UTF_8).replace("+", "%20")
    var page = 1
    while (true) {
        val (status, body) = ghRepoGet("/issues?labels=$encoded&state=open&per_page=100&page=$page")
        if (status.isHttpError) {
            error("Error: could not fetch labeled PRs (HTTP $status)", commentError = false)
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
        val (status, body) = ghRepoGet("/issues?state=open&per_page=100&page=$page")
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
            checkPrDependenciesOrNull(num)
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
    val status = postComment(prNum, message)
    if (status.isHttpError) System.err.println("Warning: could not post dependency waiting comment on PR #$prNum (HTTP $status)")
    else println("PR #$prNum: posted dependency waiting comment")
}

fun postDependencyNotification(prNum: String, closedPrNum: Int, merged: Boolean, remainingOpen: Int) {
    val message = buildDependencyNotificationMessage(closedPrNum, merged, remainingOpen)
    val status = postComment(prNum, message)
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


fun runDependenciesModeForOpenPr(prNum: String?) {
    val num = prNum ?: run { println("PR_NUMBER not set, skipping"); return }
    // This PR is the one the workflow was triggered for, so a failure belongs on it as a comment.
    try {
        checkPrDependencies(num)
    } catch (e: DependencyCheckException) {
        error(e.message ?: "Error: dependency check failed for PR #$num")
    }
    if (System.getenv("PR_ACTION") == "reopened") {
        val targetPrNum = num.toIntOrNull() ?: return
        recheckPRsDependingOn(targetPrNum)
    }
}

fun recheckLabeledPRsAfterClose(closedPrNum: Int, merged: Boolean) {
    val repoOwner = repo.substringBefore("/")
    val repoName = repo.substringAfter("/")

    for (pr in fetchAllLabeledOpenPRs()) {
        val num = pr.get("number")?.takeIf { it.isJsonPrimitive }?.asString ?: continue
        val result = checkPrDependenciesOrNull(num) ?: continue
        val isDirectDep = result.dependencies.any {
            it.owner == repoOwner && it.repoName == repoName && it.pullNumber == closedPrNum
        }

        if (isDirectDep) {
            postDependencyNotification(num, closedPrNum, merged, result.openDependencies.size)
        }
    }
}

fun runDependenciesMode(prState: String, prNum: String?, merged: Boolean) {
    if (prState != "closed") {
        runDependenciesModeForOpenPr(prNum)
        return
    }

    println("PR ${prNum ?: "unknown"} closed (merged=$merged), rechecking all open PRs with label \"$dependencyLabel\"")
    val closedPrNum = prNum?.toIntOrNull() ?: error("PR_NUMBER not set or invalid for closed event", commentError = false)
    recheckLabeledPRsAfterClose(closedPrNum, merged)
}

fun buildKeywordLabelAddedComment(entry: KeywordLabel): String = buildString {
    appendLine("This pull request is now labeled **${entry.label}**.")
    appendLine()
    appendLine(entry.description)
    appendLine()
    append("The label disappears automatically once the line `${entry.keyword}` ")
    append("is removed from the pull request description.")
}

fun buildKeywordLabelRemovedComment(entry: KeywordLabel): String = buildString {
    append("The line `${entry.keyword}` is no longer part of the pull request description, ")
    append("so the **${entry.label}** label was removed.")
}

fun setKeywordLabelStatus(headSha: String, entry: KeywordLabel, keywordPresent: Boolean) {
    val payload = mutableMapOf<String, Any>(
        "state" to if (keywordPresent) "failure" else "success",
        "context" to entry.label,
        "description" to if (keywordPresent) "Marked as \"${entry.label}\"" else "Not marked as \"${entry.label}\"",
    )
    val runId = System.getenv("GITHUB_RUN_ID")
    if (runId != null) payload["target_url"] = "https://github.com/$repo/actions/runs/$runId"

    val (status, _) = ghRequest("POST", "/repos/$repo/statuses/$headSha", payload)
    status.requireSuccess("Error: could not update \"${entry.label}\" status for $headSha (HTTP $status)")
}

fun runKeywordLabelMode(prNumber: String) {
    val (status, body) = ghRepoGet("/pulls/$prNumber")
    if (status.isHttpError) error("Error: could not fetch PR #$prNumber (HTTP $status)")
    val pr = body as? JsonObject ?: error("Error: unexpected response format for PR #$prNumber")
    val prBody = pr.get("body")?.takeIf { !it.isJsonNull }?.asString ?: ""
    val headSha = (pr.get("head") as? JsonObject)?.get("sha")?.takeIf { it.isJsonPrimitive }?.asString
        ?: error("Error: head SHA missing for PR #$prNumber")

    val bodyLines = prBody.lines()
    val currentLabels = getPrLabels(prNumber)

    for (entry in keywordLabels) {
        val keywordPresent = entry.keyword in bodyLines
        val wasLabeled = entry.label in currentLabels

        // Only comment on an actual state change, otherwise every unrelated description edit would post again.
        if (keywordPresent != wasLabeled) {
            setLabel(prNumber, entry.label, keywordPresent)
            val comment = if (keywordPresent) buildKeywordLabelAddedComment(entry)
            else buildKeywordLabelRemovedComment(entry)
            postPrComment(prNumber, comment) { "Error: could not post \"${entry.label}\" comment (HTTP $it)" }
        }

        // Always published, also when nothing changed, so the status exists on every head SHA.
        if (entry.blocking) setKeywordLabelStatus(headSha, entry, keywordPresent)

        println("PR #$prNumber: ${entry.keyword} is ${if (keywordPresent) "present" else "absent"}")
    }
}

val prNumberEnv: String? = System.getenv("PR_NUMBER")?.takeIf { it.isNotEmpty() }

if (mode == "merge_conflict") {
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
    runDependenciesMode(prState, prNumberEnv, prMerged)
    if (failedDependencyChecks.isNotEmpty()) {
        // No comment here, PR_NUMBER points at the triggering PR, not at the ones that failed.
        error(
            "Error: could not evaluate dependencies of ${failedDependencyChecks.joinToString(", ") { "#$it" }}",
            commentError = false,
        )
    }
    exitProcess(0)
}

when (mode) {
    "detekt" -> runDetektMode(prNumber)
    "build" -> runBuildMode(prNumber)
    "changelog" -> runChangelogMode(prNumber)
    "keyword_labels" -> runKeywordLabelMode(prNumber)
    else -> error("Unsupported MODE: $mode")
}
