@file:DependsOn("com.google.code.gson:gson:2.10.1")
// Execution context: base branch
// called from detekt-review.yml, build-review.yml, label-merge-conflict.yml, changelog-review.yml, check_dependencies.yml,
// and keyword-labels.yml

// TODO: remove the suppressions and split the complex functions once this file can be broken up into
//  several files. Splitting them now would only add more top level functions to a file that is
//  already well over 1000 lines long.
@file:Suppress("CyclomaticComplexMethod", "LoopWithTooManyJumpStatements")

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
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

/**
 * A keyword an author can put on its own line in the pull request description to control a label.
 * The line has to match exactly, the same way ChangelogVerification handles "exclude_from_changelog".
 *
 * [description] explains the label in the comment posted when it gets added.
 * [blocking] additionally publishes a commit status, so the pull request cannot be merged while the keyword is present.
 * [markerId] identifies the comments this entry posts, see [CommentType]. It is spelled out instead of derived
 * from [keyword] or [label], because both of those can be renamed while the markers already sit on open pull
 * requests.
 */
data class KeywordLabel(
    val keyword: String,
    val label: String,
    val markerId: String,
    val description: String,
    val blocking: Boolean,
) {
    val comment: CommentType = CommentType(markerId, "Show previous status")
}

// Announced in the state marker of every comment this mode posts, so a later run can tell which direction was
// announced last. Both have to stay within the character set stateMarkerRegex accepts.
val keywordStateActive = "active"
val keywordStateInactive = "inactive"

// The label of every blocking entry doubles as its status context and is also used by the set-pending job in
// keyword-labels.yml, both must stay in sync.
val keywordLabels = listOf(
    KeywordLabel(
        keyword = "waiting_on_hypixel_alpha",
        label = "Waiting on Hypixel",
        markerId = "keyword-label-waiting-on-hypixel",
        description = "The relevant feature is only available on the Hypixel alpha server, so this pull request can " +
            "only be tested there. It must not be merged before the feature reaches the main server.",
        blocking = true,
    ),
)

val workflowFailedMarker = "<!-- workflow-failed -->"

// Suggested in every workflow error comment. Declared this early because error() falls back to it, and error()
// already runs while the environment constants below are still being initialized.
val defaultErrorFix = "merge the beta branch into this PR."

// Nothing an author can do, so the default suggestion would send them down the wrong path.
val apiFormatErrorFix = "re-run the workflow. The GitHub API returned a response this script could not read, " +
    "which is not caused by anything in this pull request."


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
 * because there the presence of a marker cannot tell which direction was announced. The keyword label mode is
 * the second kind, it announces the label being added and being removed.
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
val dependencyComment = CommentType("dependency-check-review", "Show previous dependencies")

val dependencySectionHeading = "## Dependencies"
val dependencyEntryPrefix = "- "

// The line the pull request template ships with. Left in place, it means the section was never filled in.
val dependencyTemplatePlaceholder = "- pr_number_or_link_here"

// Announced in the state marker of every dependency comment. Both directions are announced, so the presence of
// a marker alone cannot tell which one it was.
val dependencyStateWaiting = "waiting"
val dependencyStateResolved = "resolved"

val warningIcon = "⚠\uFE0F"

val maxDirectFindings = 15
val maxErrorContinuations = 5
val maxOverloadCandidates = 3
val maxLogChars = 10_000

// Its candidate block is separated by a blank line and starts at column 0, out of reach for continuations.
val overloadErrorMarker = "None of the following candidates is applicable:"


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

fun error(message: String, commentError: Boolean = true, fix: String = defaultErrorFix): Nothing {
    System.err.println(message)
    if (commentError && !errorCommentPosted) {
        postPrComment(
            prNumber = prNumber,
            body = buildErrorComment(message, fix),
            commentError = false,
        ) { "Error: could not post workflow error as comment (HTTP $it)" }
        errorCommentPosted = true
    }
    exitProcess(1)
}

fun buildErrorComment(message: String, fix: String): String = buildString {
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
    appendLine(fix)
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

// [sourceLine] keeps the raw line: a typo is only visible in what the author wrote, not in the resolved link.
data class Dependency(val owner: String, val repoName: String, val pullNumber: Int, val sourceLine: String) {
    val link: String = "https://github.com/$owner/$repoName/pull/$pullNumber"
}

enum class DependencyState {
    OPEN,
    CLOSED,
    UNRESOLVED
}

// Split during parsing, not derived afterward: a line on hannibal002/SkyHanni-REPO is valid and deliberately
// produces no dependency, so subtracting the recognized entries would report it as malformed.
data class ParsedDependencySection(val dependencies: List<Dependency>, val unrecognizedLines: List<String>)

// Everything wrong with one dependency section, so the comment can name all of it at once. Placeholder and
// duplicate heading are flags, two placeholder lines are still one mistake.
data class DependencyProblems(
    val unresolved: List<Dependency>,
    val malformed: List<String>,
    val hasPlaceholder: Boolean,
    val hasDuplicateHeading: Boolean,
) {
    val count: Int = unresolved.size + malformed.size +
        (if (hasPlaceholder) 1 else 0) + (if (hasDuplicateHeading) 1 else 0)

    val isEmpty: Boolean get() = count == 0
}

// The closed pull request that triggered this run, used only when it is a dependency.
data class DependencyTrigger(val pullNumber: Int, val merged: Boolean)

// [fix] travels with the exception because the handler that turns it into a comment sits several frames away and
// cannot tell which kind of failure it is looking at.
class DependencyCheckException(message: String, val fix: String = defaultErrorFix) : Exception(message)

// PRs whose dependency check could not be completed. Collected instead of aborting, so one unreachable
// PR cannot stop the remaining ones from getting their label and status updated.
val failedDependencyChecks = mutableListOf<String>()

fun dependencyError(message: String, fix: String = defaultErrorFix): Nothing =
    throw DependencyCheckException(message, fix)

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

    // The GitHub API answers with 502/503/504 occasionally. Those are transient, so a single one
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
    return array.mapNotNull { element ->
        (element as? JsonObject)?.get("name")?.takeIf { it.isJsonPrimitive }?.asString
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

// Only backticks have to go, one would close the code span early. Everything else, an "@" mention included, is
// inert inside a span, and sanitize would leave its backslashes visible there.
fun sanitizeCodeSpan(text: String, maxLen: Int = 300): String = text.take(maxLen).replace("`", "'")

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
        // The message renders outside the code span, so it keeps the full Markdown escaping.
        val message = sanitize(finding.message)
        val className = sanitizeCodeSpan(fileName)
        val line = finding.line
        appendLine("- ```$className:$line```: $message")
    }
}

fun StringBuilder.appendFull(findings: List<Finding>) {
    for (finding in findings) {
        val fileName = finding.path.substringAfterLast('/')
        val ruleId = sanitizeCodeSpan(finding.ruleId)
        val message = sanitizeCodeSpan(finding.message)
        val className = sanitizeCodeSpan(fileName)
        val line = finding.line
        val path = sanitizeCodeSpan(finding.path)
        appendLine("- ```$className:$line```")
        appendLine("  message: `$message`")
        appendLine("  rule: `$ruleId`")
        appendLine("  path: `$path`")
        appendLine()
    }
}

data class PrComment(val id: Long, val body: String)

data class StateComment(val comment: PrComment, val state: String)

// Iterates every comment of a pull request in ascending comment id, which is the order the API documents.
// [action] returns false to stop early.
//
// [onFailure] must not return: continuing would hand out the pages read so far as if they were the whole listing.
fun forEachComment(prNumber: String, onFailure: (String) -> Nothing = { error(it) }, action: (PrComment) -> Boolean) {
    var page = 1
    while (true) {
        val (status, body) = ghRepoGet("/issues/$prNumber/comments?per_page=100&page=$page")
        if (status.isHttpError) onFailure("Error: could not fetch PR comments (HTTP $status)")
        val array = body as? JsonArray ?: onFailure("Error: unexpected response format for PR comments")
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

// One pagination run per pull request instead of one per lookup, so a mode looking up once per configured
// entry does not silently multiply its requests when a second entry is added.
fun fetchComments(prNumber: String, onFailure: (String) -> Nothing = { error(it) }): List<PrComment> =
    buildList { forEachComment(prNumber, onFailure) { comment -> add(comment); true } }

fun CommentType.findExisting(prNumber: String): PrComment? {
    var found: PrComment? = null
    forEachComment(prNumber) { comment ->
        val matches = comment.body.hasMarkerLine(marker)
        if (matches) found = comment
        !matches
    }
    return found
}

// Every comment still carrying an active state marker, oldest first, so the last one is the current
// announcement. More than one is a leftover from a race or from a collapse that only warned.
fun CommentType.findAllStates(comments: List<PrComment>): List<StateComment> = comments.mapNotNull { comment ->
    val state = comment.body.lineSequence()
        .firstNotNullOfOrNull { stateMarkerRegex.matchEntire(it.trim()) }
        ?.groupValues?.get(1)
    state?.let { StateComment(comment, it) }
}

fun CommentType.post(prNumber: String, body: String, errorMessage: (Int) -> String) {
    postPrComment(prNumber, "$marker\n$body", errorMessage = errorMessage)
}

// Posts under a state marker instead of the plain one, for the modes that announce both directions.
fun CommentType.postState(prNumber: String, state: String, body: String, errorMessage: (Int) -> String) {
    postPrComment(prNumber, "${stateMarker(state)}\n$body", errorMessage = errorMessage)
}


// A collapsed comment loses its active marker, including the state variant. Only the stale marker remains, so
// it can never be mistaken for the current announcement.
//
// Every body posted under a CommentType needs a line starting with "###" followed by a space, it becomes
// the title of the spoiler the collapsed comment turns into. A body without one ends up under [fallbackTitle].
// A failed collapse is harmless, so [onFailure] may return: the newest-state lookup ignores the leftover.
fun CommentType.markAsStale(
    comment: PrComment,
    fallbackTitle: String = "Unknown",
    onFailure: (String) -> Unit = { error(it) },
) {
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

    if (status.isHttpError) onFailure("Error: could not mark comment as stale (HTTP $status)")
}

fun CommentType.staleExisting(prNumber: String, fallbackTitle: String = "Unknown") {
    val existing = findExisting(prNumber) ?: return
    markAsStale(existing, fallbackTitle)
}

fun CommentType.staleAll(states: List<StateComment>, onFailure: (String) -> Unit = { error(it) }) {
    for (state in states) markAsStale(state.comment, onFailure = onFailure)
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
    while (i < lines.size && result.size < maxErrorContinuations) {
        val next = lines[i]
        // Only indented lines belong to the error above, everything else starts at column 0.
        if (next.isBlank() || next == next.trimStart()) break
        // Indented, but its own diagnosis.
        if (next.trimStart().startsWith("e: ") || next.trimStart().startsWith("w: ")) break
        result.add(next.trim())
        i++
    }
    return result
}

// Signatures only, the indented details below each one add length but no information.
fun parseOverloadCandidates(logContent: String, errorLine: String): List<String> {
    if (!errorLine.trimEnd().endsWith(overloadErrorMarker)) return emptyList()
    val lines = logContent.lines()
    val idx = lines.indexOfFirst { it.trim() == errorLine }
    if (idx < 0) return emptyList()
    val result = mutableListOf<String>()
    for (line in lines.drop(idx + 1)) {
        val trimmed = line.trimStart()
        if (trimmed.startsWith("e: ") || trimmed.startsWith("w: ")) break
        if (line.startsWith("FAILURE")) break
        if (line != trimmed || "fun " !in line) continue
        result.add(line.trim())
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
        appendBuildErrors(rawErrorLines, workspace, logContent)
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

fun StringBuilder.appendBuildErrors(rawErrorLines: List<String>, workspace: String, logContent: String) {
    if (rawErrorLines.isNotEmpty()) {
        for (rawLine in rawErrorLines.take(5)) {
            appendErrorLine(rawLine, workspace, logContent)
        }
        if (rawErrorLines.size > 5) appendLine("_...and ${rawErrorLines.size - 5} more_")
    } else {
        val oneLiner = parseOneLiner(logContent)
        if (oneLiner != null) {
            val displayLine = oneLiner.trim().removePrefix("e: ").removePrefix("w: ")
            appendLine("`${sanitizeCodeSpan(displayLine)}`")
        }
    }
}

fun StringBuilder.appendErrorLine(rawLine: String, workspace: String, logContent: String) {
    val display = rawLine.trimStart().removePrefix("e: ")
        .let {
            if (workspace.isNotEmpty()) it.replace("file://$workspace/", "").replace("$workspace/", "")
            else it
        }
    appendLine("- `${sanitizeCodeSpan(display)}`")
    if (rawLine.trimStart().startsWith("e: ")) {
        for (cont in parseErrorContinuations(logContent, rawLine)) {
            appendLine("  - `${sanitizeCodeSpan(cont)}`")
        }
        val candidates = parseOverloadCandidates(logContent, rawLine)
        for (candidate in candidates.take(maxOverloadCandidates)) {
            appendLine("  - `${sanitizeCodeSpan(candidate)}`")
        }
        if (candidates.size > maxOverloadCandidates) {
            appendLine("  - _...and ${candidates.size - maxOverloadCandidates} more candidates_")
        }
    }
}

// error() exits the process and cannot be caught, so this mode throws and lets the caller decide.
class MergeCheckException(message: String) : Exception(message)

fun mergeCheckError(message: String): Nothing = throw MergeCheckException(message)

// Collected so one unreadable pull request cannot end the whole push run.
val failedMergeChecks = mutableListOf<String>()

// [mergeable] is null while GitHub is still computing the merge, and when the field is unreadable. The head SHA
// comes from the same response, the push trigger has no pull request in its event payload to take it from.
data class MergeState(val headSha: String, val mergeable: Boolean?)

fun fetchMergeState(prNumber: String): MergeState {
    val (status, body) = ghRepoGet("/pulls/$prNumber")
    if (status.isHttpError) mergeCheckError("Error: could not fetch PR #$prNumber (HTTP $status)")
    val pr = body as? JsonObject ?: mergeCheckError("Error: unexpected response format for PR #$prNumber")
    val headSha = (pr.get("head") as? JsonObject)?.get("sha")?.takeIf { it.isJsonPrimitive }?.asString
        ?: mergeCheckError("Error: head SHA missing for PR #$prNumber")
    val mergeable = pr.get("mergeable")?.takeIf { it.isJsonPrimitive }?.asBoolean
    return MergeState(headSha, mergeable)
}

// GitHub starts computing the merge when first asked, so the run after a new commit regularly asks too early.
val mergeableAttempts = 3

fun fetchMergeStateWaiting(prNumber: String): MergeState {
    var state = fetchMergeState(prNumber)
    repeat(mergeableAttempts - 1) { index ->
        if (state.mergeable != null) return state
        System.err.println("Warning: mergeable still unknown for PR #$prNumber, retry ${index + 1} of ${mergeableAttempts - 1}")
        Thread.sleep(retryDelayMillis * (index + 1))
        state = fetchMergeState(prNumber)
    }
    return state
}

// [labels] comes from the listing, so the push path does not fetch them again per pull request.
data class OpenPr(val number: String, val labels: Set<String>)

fun parseLabelNames(pr: JsonObject): Set<String> {
    val array = pr.get("labels") as? JsonArray ?: return emptySet()
    return array.mapNotNullTo(mutableSetOf()) { element ->
        (element as? JsonObject)?.get("name")?.takeIf { it.isJsonPrimitive }?.asString
    }
}

fun getAllOpenPrs(): List<OpenPr> {
    val prs = mutableListOf<OpenPr>()
    var page = 1
    while (true) {
        val (status, body) = ghRepoGet("/pulls?state=open&per_page=100&page=$page")
        status.requireSuccess("Error: could not fetch open PRs (HTTP $status), aborting", commentError = false)
        val array = body as? JsonArray
            ?: error("Error: unexpected response format for open PRs, aborting", commentError = false)
        for (element in array) {
            val pr = element as? JsonObject ?: continue
            val number = pr.get("number")?.takeIf { it.isJsonPrimitive }?.asString ?: continue
            prs.add(OpenPr(number, parseLabelNames(pr)))
        }
        if (array.size() < 100) break
        page++
    }
    return prs
}

fun buildConflictBody(): String = buildString {
    appendWarningTitle("Merge conflicts detected")
    append("This pull request has conflicts with the base branch. Please resolve them before this PR can be merged.")
}

// Warns instead of aborting, one unwritable status must not stop the push path from reaching the rest.
fun setConflictStatus(prNumber: String, headSha: String, hasConflicts: Boolean) {
    val payload = mutableMapOf<String, Any>(
        "state" to if (hasConflicts) "failure" else "success",
        "context" to conflictLabel,
        "description" to if (hasConflicts) "Conflicts with the base branch" else "No conflicts with the base branch",
    )
    val runId = System.getenv("GITHUB_RUN_ID")
    if (runId != null) payload["target_url"] = "https://github.com/$repo/actions/runs/$runId"

    val (status, _) = ghRequest("POST", "/repos/$repo/statuses/$headSha", payload)
    if (status.isHttpError) {
        System.err.println("Warning: could not update conflict status on PR #$prNumber (HTTP $status)")
    }
}

// [waitForMergeable] only for the triggering pull request, see fetchMergeStateWaiting.
// [knownLabels] null means fetch them.
fun runMergeConflictMode(prNumber: String, waitForMergeable: Boolean, knownLabels: Set<String>? = null) {
    val mergeState = if (waitForMergeable) fetchMergeStateWaiting(prNumber) else fetchMergeState(prNumber)
    val mergeable = mergeState.mergeable
    if (mergeable == null) {
        // No status either, every state the API offers would claim an answer this run does not have.
        println("PR #$prNumber: mergeable is null, skipping")
        return
    }
    // Before the label check below, an already labeled pull request is exactly the one that needs the status.
    setConflictStatus(prNumber, mergeState.headSha, !mergeable)

    if (!mergeable) {
        // Only the comment is skipped, every push to beta would otherwise post another one.
        if (conflictLabel in (knownLabels ?: getPrLabels(prNumber))) {
            println("PR #$prNumber: conflicts found, already labeled, skipping comment")
            return
        }
        // Label first, setLabel only warns: a comment without a label is invisible to the cleanup below.
        setLabel(prNumber, conflictLabel, true)
        conflictComment.staleExisting(prNumber)
        conflictComment.post(prNumber, buildConflictBody()) { "Error: could not post conflict comment (HTTP $it)" }
        println("PR #$prNumber: conflicts found, comment posted")
    } else {
        // Relies on the label being written before the comment above, so no label means no comment to collapse.
        if (knownLabels != null && conflictLabel !in knownLabels) {
            println("PR #$prNumber: no conflicts, nothing to clean up")
            return
        }
        conflictComment.staleExisting(prNumber)
        setLabel(prNumber, conflictLabel, false)
        println("PR #$prNumber: no conflicts")
    }
}

fun skipFailedMergeCheck(prNumber: String, reason: String) {
    System.err.println("Warning: could not check PR #$prNumber ($reason), skipping")
    failedMergeChecks.add(prNumber)
}

// A dropped connection has to be treated like an unreadable response, otherwise it still ends the loop.
fun tryRunMergeConflictMode(pr: OpenPr) {
    try {
        runMergeConflictMode(pr.number, waitForMergeable = false, knownLabels = pr.labels)
    } catch (e: MergeCheckException) {
        skipFailedMergeCheck(pr.number, e.message ?: e.toString())
    } catch (e: IOException) {
        skipFailedMergeCheck(pr.number, e.toString())
    }
}

fun buildDetektCrashBody(logContent: String): String = buildString {
    appendWarningTitle("Detekt could not run")
    appendLine()
    val oneLiner = parseOneLiner(logContent)
    if (oneLiner != null) {
        val displayLine = oneLiner.trim().removePrefix("e: ").removePrefix("w: ")
        appendLine("`${sanitizeCodeSpan(displayLine)}`")
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
                        "Check the workflow run for details.",
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

// Posted when the run is red but carries no log to quote. Every reason for a missing artifact looks the same
// from here, so it names the conclusion instead of guessing.
fun buildGenericFailureBody(conclusion: String): String = buildString {
    val workflowRunId = System.getenv("WORKFLOW_RUN_ID") ?: error("WORKFLOW_RUN_ID not set")
    appendWarningTitle("Build failed")
    appendLine()
    appendLine("The build workflow finished with `${sanitizeCodeSpan(conclusion)}` but uploaded no error log.")
    appendLine("That happens when a step fails outside of the parts that capture their output, so the cause is")
    appendLine("only visible in the run itself.")
    appendLine()
    appendLine("\\[[workflow run](https://github.com/$repo/actions/runs/$workflowRunId)\\]")
}

fun runBuildMode(prNumber: String) {
    val log1 = readBuildLog(System.getenv("ARTIFACT_DIR_1"))

    buildComment.staleExisting(prNumber)

    if (log1.isNullOrBlank()) {
        // A missing artifact is not proof of a green build: a step failing before its upload leaves it missing
        // while the run is red. Only the conclusion tells those two apart.
        val conclusion = System.getenv("WORKFLOW_CONCLUSION")?.takeIf { it.isNotEmpty() }
            ?: error("WORKFLOW_CONCLUSION not set")
        if (conclusion != "success") {
            buildComment.post(prNumber, buildGenericFailureBody(conclusion)) {
                "Error: could not post build failure comment (HTTP $it)"
            }
            setLabel(prNumber, buildLabel, true)
            println("Build failed without a log artifact, posted generic comment, added label")
            exitProcess(0)
        }
        println("No build failures found, removing build label")
        setLabel(prNumber, buildLabel, false)
        exitProcess(0)
    }

    val versions = filterStonecutterDuplicates(listOf("26.1" to log1))
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

// Anchored and applied per line: unanchored over the whole body, a changelog line like "+ Fixed X - #1234." reads
// as a dependency. Trailing text stays allowed, "- #1234 (needed for the item API)" is common.
val dependencyUrlRegex = Regex("""^- https://github\.com/([\w-]+)/([\w-]+)/pull/(\d+)""")
val dependencyNumberRegex = Regex("""^- #(\d+)""")

fun countDependencyHeadings(body: String): Int = body.lines().count { it.trim() == dependencySectionHeading }

// Null when the section is absent, empty when it exists without entries. Only the second case can carry a malformed
// entry. The section ends at the first line that is not a list entry, but blank lines right below the heading are
// skipped, writing one there is too common to let it drop everything underneath.
fun extractDependencySection(body: String): List<String>? {
    val lines = body.lines()
    val headingIndex = lines.indexOfFirst { it.trim() == dependencySectionHeading }
    if (headingIndex < 0) return null

    var index = headingIndex + 1
    while (index < lines.size && lines[index].isBlank()) index++

    val entries = mutableListOf<String>()
    while (index < lines.size && lines[index].startsWith(dependencyEntryPrefix)) {
        entries.add(lines[index].trimEnd())
        index++
    }
    return entries
}

fun parseDependencySection(sectionLines: List<String>): ParsedDependencySection {
    val repoOwner = repo.substringBefore("/")
    val repoName = repo.substringAfter("/")
    val deps = mutableListOf<Dependency>()
    val unrecognized = mutableListOf<String>()

    for (line in sectionLines) {
        val urlMatch = dependencyUrlRegex.find(line)
        if (urlMatch != null) {
            val depOwner = urlMatch.groupValues[1]
            val depRepo = urlMatch.groupValues[2]
            // A number too large for an Int cannot be a pull request. toInt would throw, and nothing catches
            // that: the closed event iterates over every labeled PR and would abandon the rest of them.
            val depNum = urlMatch.groupValues[3].toIntOrNull()
            if (depNum == null) {
                unrecognized.add(line)
                continue
            }
            // Valid, and deliberately produces no dependency. Never a malformed line.
            if (depOwner == "hannibal002" && depRepo == "SkyHanni-REPO") continue
            deps.add(Dependency(depOwner, depRepo, depNum, line))
            continue
        }
        val depNum = dependencyNumberRegex.find(line)?.groupValues?.get(1)?.toIntOrNull()
        if (depNum == null) {
            unrecognized.add(line)
            continue
        }
        deps.add(Dependency(repoOwner, repoName, depNum, line))
    }

    return ParsedDependencySection(deps, unrecognized)
}

// A 404 used to count as "not open", so a typo turned the status green. It is reported instead, but never as
// "does not exist": an unreadable repository and a link pointing at an issue both answer 404 as well.
fun getDependencyState(dep: Dependency): DependencyState {
    val (status, body) = ghRequest("GET", "/repos/${dep.owner}/${dep.repoName}/pulls/${dep.pullNumber}")
    if (status == 404) {
        System.err.println("Warning: dependency ${dep.owner}/${dep.repoName}#${dep.pullNumber} could not be resolved")
        return DependencyState.UNRESOLVED
    }
    if (status.isHttpError) {
        dependencyError("Error: unexpected status $status for dependency ${dep.owner}/${dep.repoName}#${dep.pullNumber}")
    }
    val state = (body as? JsonObject)?.get("state")?.takeIf { it.isJsonPrimitive }?.asString
    // Treating anything unreadable as closed would let the pull request merge, which is the same mistake the 404
    // handling above used to make. A pull request only ever has these two states.
    return when (state) {
        "open" -> DependencyState.OPEN
        "closed" -> DependencyState.CLOSED
        else -> dependencyError(
            "Error: dependency ${dep.owner}/${dep.repoName}#${dep.pullNumber} has no usable state (got: $state)",
            fix = apiFormatErrorFix,
        )
    }
}

// Kept under the status API's 140-character limit. A problem hides the open count, like the comment.
fun buildDependencyStatusDescription(openCount: Int, problems: DependencyProblems): String = when {
    !problems.isEmpty -> dependencyProblemsTitle(problems.count)
    openCount > 0 -> "Waiting on $openCount open dependency ${if (openCount == 1) "PR" else "PRs"}"
    else -> "All dependency PRs are resolved"
}

fun setDependencyStatus(headSha: String, openDependencies: List<Dependency>, problems: DependencyProblems) {
    val blocking = openDependencies.isNotEmpty() || !problems.isEmpty
    val description = buildDependencyStatusDescription(openDependencies.size, problems)
    val payload = mutableMapOf<String, Any>(
        "state" to if (blocking) "failure" else "success",
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
// must use tryCheckPrDependencies instead.
fun checkPrDependencies(issueNumber: String, trigger: DependencyTrigger? = null) {
    val (status, body) = ghRepoGet("/pulls/$issueNumber")
    if (status.isHttpError) {
        dependencyError("Error: could not fetch PR #$issueNumber (HTTP $status)")
    }
    val pr = body as? JsonObject ?: dependencyError("Error: unexpected response format for PR #$issueNumber")
    val prBody = pr.get("body")?.takeIf { !it.isJsonNull }?.asString ?: ""

    val headSha = (pr.get("head") as? JsonObject)?.get("sha")?.takeIf { it.isJsonPrimitive }?.asString
        ?: dependencyError("Error: head SHA missing for PR #$issueNumber")

    // No early exit for a section without entries: one holding nothing but the template placeholder has no
    // dependency and still has a problem to report. An empty entry list costs no requests below anyway.
    val sectionLines = extractDependencySection(prBody)
    val parsed = sectionLines?.let { parseDependencySection(it) }
    val deps = parsed?.dependencies.orEmpty()

    // Resolved once per entry, the state is needed twice below and a second pass would double the requests.
    val evaluated = deps.map { it to getDependencyState(it) }
    val openDeps = evaluated.filter { it.second == DependencyState.OPEN }.map { it.first }

    val unrecognized = parsed?.unrecognizedLines.orEmpty()
    val problems = DependencyProblems(
        unresolved = evaluated.filter { it.second == DependencyState.UNRESOLVED }.map { it.first },
        malformed = unrecognized.filterNot { it.trim() == dependencyTemplatePlaceholder },
        hasPlaceholder = unrecognized.any { it.trim() == dependencyTemplatePlaceholder },
        hasDuplicateHeading = countDependencyHeadings(prBody) > 1,
    )

    // The label stays reserved for actually open dependencies, a problem blocks through status and comment only.
    setLabel(issueNumber, dependencyLabel, openDeps.isNotEmpty())
    setDependencyStatus(headSha, openDeps, problems)
    handleDependencyComment(issueNumber, deps, openDeps, problems, trigger)

    val summary = when {
        !problems.isEmpty -> "has ${problems.count} section ${if (problems.count == 1) "problem" else "problems"}"
        openDeps.isNotEmpty() -> "has open dependencies"
        sectionLines == null -> "no Dependencies section"
        else -> "all dependencies resolved"
    }
    println("PR #$issueNumber: $summary")
}


fun skipFailedDependencyCheck(issueNumber: String, reason: String) {
    System.err.println("Warning: could not evaluate dependencies of PR #$issueNumber ($reason), skipping")
    failedDependencyChecks.add(issueNumber)
}

// A dropped connection has to be treated like a repeated gateway timeout, otherwise a transport failure
// still aborts the whole loop while an HTTP failure does not.
fun tryCheckPrDependencies(issueNumber: String, trigger: DependencyTrigger? = null) {
    try {
        checkPrDependencies(issueNumber, trigger)
    } catch (e: DependencyCheckException) {
        skipFailedDependencyCheck(issueNumber, e.message ?: e.toString())
    } catch (e: IOException) {
        skipFailedDependencyCheck(issueNumber, e.toString())
    }
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
        val sectionLines = extractDependencySection(body) ?: continue
        val deps = parseDependencySection(sectionLines).dependencies
        if (deps.any { it.owner == repoOwner && it.repoName == repoName && it.pullNumber == targetPrNum }) {
            tryCheckPrDependencies(num)
        }
    }
}

val dependencyReEvaluateNote = "You may need to re-evaluate this PR's dependencies."

// Starts all state texts and marks the end of the trigger section.
// Must remain exact, otherwise dependencyStateLines drops the entire comment.
val dependencyStatePrefix = "This PR is"

// The problem comment has no state line, so dependencyStateLines needs this second anchor. Must stay a prefix of
// dependencyProblemsTitle, otherwise a corrected section never gets announced.
val dependencyProblemsTitlePrefix = "### $warningIcon The dependency section has"

// The count sits at the end, so the prefix above stays stable.
fun dependencyProblemsTitle(count: Int): String =
    "The dependency section has $count ${if (count == 1) "problem" else "problems"}"

// Trigger link format must stay identical when building and recognizing trigger entries.
fun dependencyTriggerLink(pullNumber: Int): String = "- https://github.com/$repo/pull/$pullNumber"

fun StringBuilder.appendDependencyState(openDependencies: List<Dependency>) {
    if (openDependencies.isEmpty()) {
        // Closed does not imply merged.
        appendLine("$dependencyStatePrefix no longer waiting on any open dependency PRs.")
        return
    }

    val word = if (openDependencies.size == 1) "dependency" else "dependencies"
    appendLine("$dependencyStatePrefix now waiting on the following $word:")

    for (dep in openDependencies) {
        appendLine("- ${dep.link}")
    }
}

fun StringBuilder.appendProblemLines(lines: List<String>) {
    for (line in lines) {
        appendLine("- `${sanitizeCodeSpan(line)}`")
    }
    appendLine()
}

fun StringBuilder.appendDependencyProblems(problems: DependencyProblems) {
    appendWarningTitle(dependencyProblemsTitle(problems.count))
    appendLine()

    if (problems.unresolved.isNotEmpty()) {
        appendLine("Could not be resolved:")
        appendProblemLines(problems.unresolved.map { it.sourceLine })
        appendLine(
            "Check the number or the link for a typo. If the entry points at a pull request in a repository this " +
                "bot cannot read, remove it and mention it in the What section instead.",
        )
        appendLine()
    }

    if (problems.malformed.isNotEmpty()) {
        appendLine("Not a valid dependency entry:")
        appendProblemLines(problems.malformed)
        appendLine("Use `- #<pr number>` for this repository, or `- <url>` for another one.")
        appendLine()
    }

    if (problems.hasPlaceholder) {
        appendLine("The section still holds the template placeholder. Fill it in or remove the section.")
        appendLine()
    }

    if (problems.hasDuplicateHeading) {
        appendLine(
            "The `## Dependencies` heading appears more than once. Only the first one is read, so every entry " +
                "has to sit under it.",
        )
        appendLine()
    }

    append("This blocks the pull request.")
}

fun buildDependencyComment(
    trigger: DependencyTrigger?,
    openDependencies: List<Dependency>,
    problems: DependencyProblems,
): String = buildString {
    // A broken section makes the open dependencies irrelevant, only the problems are worth showing.
    if (!problems.isEmpty) {
        appendDependencyProblems(problems)
        return@buildString
    }

    appendLine("### Dependencies")
    appendLine()

    if (trigger != null) {
        val what = if (trigger.merged) "was merged" else "was closed without merging"
        appendLine("The following dependency PR $what:")
        appendLine(dependencyTriggerLink(trigger.pullNumber))
        appendLine()
    }

    appendDependencyState(openDependencies)

    if (trigger != null && !trigger.merged) {
        appendLine()
        append(dependencyReEvaluateNote)
    }
}

// Extracts state text and open dependencies to detect changes.
// Drops the trigger section because its links look identical to open dependencies.
fun dependencyStateLines(body: String): List<String> = body.lineSequence()
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .dropWhile { !it.startsWith(dependencyStatePrefix) && !it.startsWith(dependencyProblemsTitlePrefix) }
    .filterNot { it == dependencyReEvaluateNote }
    .toList()

fun handleDependencyComment(
    issueNumber: String,
    dependencies: List<Dependency>,
    openDependencies: List<Dependency>,
    problems: DependencyProblems,
    trigger: DependencyTrigger?,
) {
    val repoOwner = repo.substringBefore("/")
    val repoName = repo.substringAfter("/")
    // A close event matters only if the closed pull request is listed as a dependency. Removing the line
    // before the event runs is therefore intentionally silent.
    val matchingTrigger = trigger?.takeIf { closed ->
        dependencies.any { it.owner == repoOwner && it.repoName == repoName && it.pullNumber == closed.pullNumber }
    }

    // Skips this pull request instead of ending the loop over all of them. Reading no comments must not count as
    // nothing announced, the missing marker would read as resolved and post the announcement twice.
    val comments = fetchComments(issueNumber) { dependencyError(it) }
    val announcedStates = dependencyComment.findAllStates(comments)
    val announced = announcedStates.lastOrNull()
    // A pull request that was never announced is in the same position as one whose dependencies are all closed.
    val announcedState = announced?.state ?: dependencyStateResolved
    // The marker answers "is this blocked", not why: both reasons can apply at once, a third value would fit neither.
    val blocking = openDependencies.isNotEmpty() || !problems.isEmpty
    val currentState = if (blocking) dependencyStateWaiting else dependencyStateResolved

    val body = buildDependencyComment(matchingTrigger, openDependencies, problems)

    // The state alone misses a second dependency being added while the pull request keeps waiting.
    val stateChanged = announcedState != currentState ||
        (announced != null && dependencyStateLines(announced.comment.body) != dependencyStateLines(body))

    // Searches only the trigger section of the newest comment that still carries an active marker
    // Ignores the state section to prevent false positive matches with open dependencies.
    val triggerAlreadyAnnounced = matchingTrigger != null && announced != null &&
        announced.comment.body.lineSequence()
            .map { it.trim() }
            .takeWhile { !it.startsWith(dependencyStatePrefix) }
            .any { it == dependencyTriggerLink(matchingTrigger.pullNumber) }

    // The problem comment never shows the trigger, so without this every closed dependency reposts it.
    val triggerIsNew = matchingTrigger != null && !triggerAlreadyAnnounced && problems.isEmpty

    val posting = stateChanged || triggerIsNew
    if (posting) {
        // Not routed through CommentType.post, that aborts the run on failure. This one runs in a loop over
        // every labeled pull request, where one unreachable pull request must not stop the rest.
        val status = postComment(issueNumber, "${dependencyComment.stateMarker(currentState)}\n$body")
        if (status.isHttpError) {
            System.err.println("Warning: could not post dependency comment on PR #$issueNumber (HTTP $status)")
            return
        }
        println("PR #$issueNumber: posted dependency comment")
    }

    // After the post, so a failed post cannot erase the announcement. Leftovers go on every run, without a post
    // the newest one stays as the current announcement. Warns per comment, one that cannot be patched must not
    // block the rest.
    val outdated = if (posting) announcedStates else announcedStates.dropLast(1)
    dependencyComment.staleAll(outdated) { System.err.println("Warning: $it on PR #$issueNumber") }
}

fun runDependenciesModeForOpenPr(prNum: String?) {
    val num = prNum ?: run { println("PR_NUMBER not set, skipping"); return }
    // This PR is the one the workflow was triggered for, so a failure belongs on it as a comment.
    try {
        checkPrDependencies(num)
    } catch (e: DependencyCheckException) {
        error(e.message ?: "Error: dependency check failed for PR #$num", fix = e.fix)
    }
    if (System.getenv("PR_ACTION") == "reopened") {
        val targetPrNum = num.toIntOrNull() ?: return
        recheckPRsDependingOn(targetPrNum)
    }
}

fun recheckLabeledPRsAfterClose(trigger: DependencyTrigger) {
    for (pr in fetchAllLabeledOpenPRs()) {
        val num = pr.get("number")?.takeIf { it.isJsonPrimitive }?.asString ?: continue
        tryCheckPrDependencies(num, trigger)
    }
}

fun runDependenciesMode(prState: String, prNum: String?, merged: Boolean) {
    if (prState != "closed") {
        runDependenciesModeForOpenPr(prNum)
        return
    }

    println("PR ${prNum ?: "unknown"} closed (merged=$merged), rechecking all open PRs with label \"$dependencyLabel\"")
    val closedPrNum = prNum?.toIntOrNull() ?: error("PR_NUMBER not set or invalid for closed event", commentError = false)
    recheckLabeledPRsAfterClose(DependencyTrigger(closedPrNum, merged))
}

fun buildKeywordLabelAddedComment(entry: KeywordLabel): String = buildString {
    appendLine("### Labeled ${entry.label}")
    appendLine()
    appendLine("This pull request is now labeled **${entry.label}**.")
    appendLine()
    appendLine(entry.description)
    appendLine()
    append("The label disappears automatically once the line `${entry.keyword}` ")
    append("is removed from the pull request description.")
}

fun buildKeywordLabelRemovedComment(entry: KeywordLabel): String = buildString {
    appendLine("### Removed ${entry.label}")
    appendLine()
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
    // Fetched once for all entries. A comment posted inside the loop is missing here and a collapsed one still
    // carries its old text, but every entry only ever matches its own marker id, so neither can reach another
    // entry.
    val comments = fetchComments(prNumber)

    for (entry in keywordLabels) {
        val keywordPresent = entry.keyword in bodyLines
        val wasLabeled = entry.label in currentLabels
        val currentState = if (keywordPresent) keywordStateActive else keywordStateInactive

        if (keywordPresent != wasLabeled) setLabel(prNumber, entry.label, keywordPresent)

        // The marker decides, not the label, so neither a hand-edited label nor a label write that only warned
        // can turn into a duplicated or a missing comment.
        val announcedStates = entry.comment.findAllStates(comments)
        val announced = announcedStates.lastOrNull()
        // Never announced is the same as having announced inactive. Comparing the nullable value directly would
        // post a label-removed comment on every untouched pull request.
        val announcedState = announced?.state ?: keywordStateInactive

        val posting = announcedState != currentState
        if (posting) {
            val body = if (keywordPresent) buildKeywordLabelAddedComment(entry)
            else buildKeywordLabelRemovedComment(entry)
            entry.comment.postState(prNumber, currentState, body) {
                "Error: could not post \"${entry.label}\" comment (HTTP $it)"
            }
        }

        // After the post, unlike the detekt and build modes: a failed post after a collapse would leave the
        // announcement gone and the absent marker reads as inactive. Leftovers go on every run, without a post
        // the newest one stays. Warns instead of ending the run, which would skip the commit status below.
        entry.comment.staleAll(if (posting) announcedStates else announcedStates.dropLast(1)) {
            System.err.println("Warning: $it on PR #$prNumber")
        }

        // Always published, also when nothing changed, so the status exists on every head SHA.
        if (entry.blocking) setKeywordLabelStatus(headSha, entry, keywordPresent)

        println("PR #$prNumber: ${entry.keyword} is ${if (keywordPresent) "present" else "absent"}")
    }
}

val prNumberEnv: String? = System.getenv("PR_NUMBER")?.takeIf { it.isNotEmpty() }

if (mode == "merge_conflict") {
    if (prNumberEnv != null) {
        // Nothing to salvage when the triggering pull request itself cannot be read.
        try {
            runMergeConflictMode(prNumberEnv, waitForMergeable = true)
        } catch (e: MergeCheckException) {
            error(e.message ?: "Error: could not check PR #$prNumberEnv", commentError = false)
        } catch (e: IOException) {
            error("Error: could not check PR #$prNumberEnv ($e)", commentError = false)
        }
    } else {
        println("No PR_NUMBER set, rechecking all open PRs")
        // No waiting here, a beta push invalidates every open pull request at once and waiting adds minutes.
        getAllOpenPrs().forEach { tryRunMergeConflictMode(it) }
        // After the loop, so every reachable pull request is done before the run turns red.
        if (failedMergeChecks.isNotEmpty()) {
            error("Error: could not check ${failedMergeChecks.joinToString(", ") { "#$it" }}", commentError = false)
        }
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
