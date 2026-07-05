@file:DependsOn("com.google.code.gson:gson:2.10.1")

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

val label = "Detekt"
val marker = "<!-- detekt-review -->"
val staleMarker = "<!-- detekt-review-stale -->"
val buildMarker = "<!-- build-failure-review -->"
val buildStaleMarker = "<!-- build-failure-review-stale -->"
val maxDirectFindings = 8
val maxLogChars = 10_000

val repo: String = System.getenv("GITHUB_REPOSITORY") ?: error("GITHUB_REPOSITORY not set")
val token: String = System.getenv("GH_TOKEN") ?: error("GH_TOKEN not set")
val mode: String = System.getenv("MODE") ?: error("MODE not set")

val httpClient: HttpClient = HttpClient.newHttpClient()
val gson = Gson()

fun error(message: String): Nothing {
    System.err.println(message)
    exitProcess(1)
}

val Int.isHttpError: Boolean get() = this !in 200..299

fun Int.requireSuccess(message: String) {
    if (isHttpError) error(message)
}

data class Finding(val path: String, val line: Int, val ruleId: String, val message: String)

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

fun setLabel(prNumber: String, hasFindings: Boolean) {
    if (hasFindings) {
        val (status, _) = ghRequest("POST", "/repos/$repo/issues/$prNumber/labels", mapOf("labels" to listOf(label)))
        if (status.isHttpError) System.err.println("Warning: could not add $label label (HTTP $status)")
    } else {
        val encoded = URLEncoder.encode(label, StandardCharsets.UTF_8)
        val (status, _) = ghRequest("DELETE", "/repos/$repo/issues/$prNumber/labels/$encoded")
        if (status.isHttpError && status != 404) System.err.println("Warning: could not remove $label label (HTTP $status)")
    }
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

fun buildDetektBody(findings: List<Finding>): String = buildString {
    appendLine(marker)
    appendLine("### Detekt found ${findings.size} ${if (findings.size == 1) "issue" else "issues"}")
    appendLine("")
    val direct = findings.take(maxDirectFindings)
    val overflow = findings.drop(maxDirectFindings)
    direct.forEach {
        val fileName = it.path.substringAfterLast('/')
        appendLine("- ${sanitize(it.message)} (`${sanitize(it.ruleId)}`)")
        appendLine("  `${sanitize(fileName)}`:${it.line} (`${sanitize(it.path)}`)")
    }
    if (overflow.isNotEmpty()) {
        appendLine("\n<details><summary>${overflow.size} more ${if (overflow.size == 1) "issue" else "issues"}</summary>\n")
        overflow.forEach {
            val fileName = it.path.substringAfterLast('/')
            appendLine("- ${sanitize(it.message)} (`${sanitize(it.ruleId)}`)")
            appendLine("  `${sanitize(fileName)}`:${it.line} (`${sanitize(it.path)}`)")
        }
        appendLine("\n</details>")
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
    staleHeading: String,
    expandLabel: String,
) {
    val oldBody = getCommentBody(commentId) ?: error("Error: comment body was null for comment $commentId, aborting")
    val staleBody = buildString {
        appendLine(staleMarker)
        appendLine("### $staleHeading")
        appendLine("<details><summary>$expandLabel</summary>")
        appendLine()
        appendLine(oldBody.replace(activeMarker, ""))
        appendLine()
        append("</details>")
    }
    val (status, _) = ghRequest("PATCH", "/repos/$repo/issues/comments/$commentId", mapOf("body" to staleBody))
    status.requireSuccess("Error: could not mark comment as stale (HTTP $status), aborting")
}

fun readBuildLog(artifactDirPath: String?): String? {
    if (artifactDirPath == null) return null
    return runCatching {
        Path(artifactDirPath).listDirectoryEntries("*.log").firstOrNull()?.readText()
    }.getOrNull()
}

fun parseOneLiner(logContent: String): String? =
    logContent.lines().firstOrNull { it.trimStart().startsWith("e: ") }
        ?: logContent.lines().firstOrNull { it.contains("> Task :") && it.trimEnd().endsWith("FAILED") }

fun parseStackTrace(logContent: String): String {
    val startIndex = logContent.indexOf("FAILURE: Build failed with an exception")
    val raw = if (startIndex >= 0) logContent.substring(startIndex) else logContent
    return if (raw.length > maxLogChars) raw.take(maxLogChars) + "\n\n... (truncated)" else raw
}

fun buildBuildFailureBody(versions: List<Pair<String, String?>>): String = buildString {
    appendLine(buildMarker)
    for ((version, logContent) in versions) {
        if (logContent.isNullOrBlank()) continue
        appendLine()
        appendLine("### Build failed: $version")
        val oneLiner = parseOneLiner(logContent)
        if (oneLiner != null) appendLine("`${oneLiner.trim().take(300)}`")
        appendLine()
        appendLine("<details><summary>Full output</summary>")
        appendLine()
        appendLine("~~~")
        appendLine(parseStackTrace(logContent))
        appendLine("~~~")
        appendLine()
        appendLine("</details>")
    }
}

fun runDetektMode(prNumber: String) {
    val existingId = findExistingComment(prNumber, marker)
    if (existingId != null) markCommentAsStale(existingId, marker, staleMarker, "Outdated Detekt issues", "click to show old warnings")

    val artifactDir = Path(System.getenv("ARTIFACT_DIR") ?: "detekt-artifact")
    val sarifFile = artifactDir / "main.sarif"

    if (!sarifFile.exists()) {
        println("No SARIF found, removing label")
        setLabel(prNumber, false)
        exitProcess(0)
    }

    val workspace = System.getenv("GITHUB_WORKSPACE") ?: ""
    val sarif: JsonObject = runCatching { JsonParser.parseString(sarifFile.readText()).asJsonObject }.getOrElse {
        error("Failed to parse SARIF: ${it.message}")
    }

    val findings = buildList {
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
                    add(
                        Finding(
                            path = normalizePath(uri, workspace),
                            line = region.get("startLine")
                                ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
                                ?.asInt?.takeIf { it > 0 } ?: 1,
                            ruleId = resultObj.get("ruleId")
                                ?.takeIf { it.isJsonPrimitive }?.asString ?: "Unknown",
                            message = resultObj.getAsJsonObject("message")?.get("text")
                                ?.takeIf { it.isJsonPrimitive }?.asString ?: "",
                        )
                    )
                }
            }
        }
    }

    if (findings.isEmpty()) {
        println("No findings, removing label")
        setLabel(prNumber, false)
        exitProcess(0)
    }

    val (postStatus, _) = ghRequest("POST", "/repos/$repo/issues/$prNumber/comments", mapOf("body" to buildDetektBody(findings)))
    postStatus.requireSuccess("Error: could not post comment (HTTP $postStatus)")
    setLabel(prNumber, true)
    println("Done: ${findings.size} finding(s) posted")
}

fun runBuildMode(prNumber: String) {
    val log1 = readBuildLog(System.getenv("ARTIFACT_DIR_1"))
    val log2 = readBuildLog(System.getenv("ARTIFACT_DIR_2"))

    val existingId = findExistingComment(prNumber, buildMarker)

    if (log1.isNullOrBlank() && log2.isNullOrBlank()) {
        println("No build failures found")
        if (existingId != null) markCommentAsStale(
            existingId,
            buildMarker,
            buildStaleMarker,
            "Outdated build failure",
            "click to show old output"
        )
        exitProcess(0)
    }

    if (existingId != null) markCommentAsStale(
        existingId,
        buildMarker,
        buildStaleMarker,
        "Outdated build failure",
        "click to show old output"
    )

    val versions = listOf("1.21.11" to log1, "26.1" to log2)
    val (postStatus, _) = ghRequest("POST", "/repos/$repo/issues/$prNumber/comments", mapOf("body" to buildBuildFailureBody(versions)))
    postStatus.requireSuccess("Error: could not post build failure comment (HTTP $postStatus)")
    println("Done: build failure comment posted")
}

val prNumber: String = System.getenv("PR_NUMBER")?.takeIf { it.isNotEmpty() }
    ?: run { println("PR_NUMBER not set, skipping"); exitProcess(0) }

when (mode) {
    "detekt" -> runDetektMode(prNumber)
    "build" -> runBuildMode(prNumber)
    else -> error("Unsupported MODE: $mode")
}
