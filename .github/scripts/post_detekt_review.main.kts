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
import kotlin.io.path.readText
import kotlin.system.exitProcess

val maxInline = 20
val label = "Detekt"
val repo: String = System.getenv("GITHUB_REPOSITORY") ?: run { System.err.println("GITHUB_REPOSITORY not set"); exitProcess(1) }
val token: String = System.getenv("GH_TOKEN") ?: run { System.err.println("GH_TOKEN not set"); exitProcess(1) }

val httpClient: HttpClient = HttpClient.newHttpClient()
val gson = Gson()

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
        if (status !in 200..201) System.err.println("Warning: could not add $label label (HTTP $status)")
    } else {
        val encoded = URLEncoder.encode(label, StandardCharsets.UTF_8)
        val (status, _) = ghRequest("DELETE", "/repos/$repo/issues/$prNumber/labels/$encoded")
        if (status !in 200..204 && status != 404) System.err.println("Warning: could not remove $label label (HTTP $status)")
    }
}

fun normalizePath(uri: String, workspace: String): String {
    val path = uri.removePrefix("file://")
    if (workspace.isNotEmpty() && path.startsWith(workspace)) return path.removePrefix(workspace).trimStart('/')
    if ("SkyHanni/" in path) return path.substringAfter("SkyHanni/")
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

fun buildBody(findings: List<Finding>, inlinePosted: Boolean): String = buildString {
    appendLine("## Detekt found ${findings.size} issue(s)\n")
    when {
        inlinePosted && findings.size <= maxInline ->
            appendLine("All issues are shown as inline comments.")

        inlinePosted -> {
            appendLine("The first $maxInline issues are shown as inline comments.")
            val overflow = findings.drop(maxInline)
            appendLine("\n<details><summary>${overflow.size} more issue(s)</summary>\n")
            overflow.forEach { appendLine("- **`${sanitize(it.path)}`**:${it.line} `${sanitize(it.ruleId)}`: ${sanitize(it.message)}") }
            appendLine("\n</details>")
        }

        else -> {
            appendLine("Could not add inline comments (violations may be in unchanged code).\n")
            findings.take(maxInline)
                .forEach { appendLine("- **`${sanitize(it.path)}`**:${it.line} `${sanitize(it.ruleId)}`: ${sanitize(it.message)}") }
            val overflow = findings.drop(maxInline)
            if (overflow.isNotEmpty()) {
                appendLine("\n<details><summary>${overflow.size} more issue(s)</summary>\n")
                overflow.forEach { appendLine("- **`${sanitize(it.path)}`**:${it.line} `${sanitize(it.ruleId)}`: ${sanitize(it.message)}") }
                appendLine("\n</details>")
            }
        }
    }
}

fun postReview(prNumber: String, body: String, comments: List<Map<String, Any>>? = null): Pair<Int, JsonElement> {
    val payload = buildMap {
        put("body", body)
        put("event", "COMMENT")
        if (!comments.isNullOrEmpty()) put("comments", comments)
    }
    return ghRequest("POST", "/repos/$repo/pulls/$prNumber/reviews", payload)
}

val prNumber: String = System.getenv("PR_NUMBER")?.takeIf { it.isNotEmpty() }
    ?: run { println("PR_NUMBER not set, skipping"); exitProcess(0) }

val artifactDir = Path("detekt-artifact")
val sarifFile = artifactDir / "main.sarif"

if (!sarifFile.exists()) {
    println("No SARIF found, skipping")
    exitProcess(0)
}

val workspace = System.getenv("GITHUB_WORKSPACE") ?: ""
val sarif: JsonObject = runCatching { JsonParser.parseString(sarifFile.readText()).asJsonObject }.getOrElse {
    System.err.println("Failed to parse SARIF: ${it.message}")
    exitProcess(1)
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

val inlineComments = findings.take(maxInline).map { f ->
    mapOf("path" to f.path, "line" to f.line, "side" to "RIGHT", "body" to "`${sanitize(f.ruleId)}`: ${sanitize(f.message)}")
}

val firstResult = postReview(prNumber, buildBody(findings, inlinePosted = true), inlineComments)
var status = firstResult.first
var resp = firstResult.second

if (status == 422) {
    val msg = (resp as? JsonObject)?.get("message")?.asString ?: ""
    println("Inline comments rejected (HTTP 422: $msg), retrying body-only")
    val retryResult = postReview(prNumber, buildBody(findings, inlinePosted = false))
    status = retryResult.first
    resp = retryResult.second
}

if (status !in 200..201) {
    val msg = (resp as? JsonObject)?.get("message")?.asString ?: ""
    System.err.println("Failed to post review: HTTP $status: $msg")
    exitProcess(1)
}

setLabel(prNumber, true)
val overflow = maxOf(0, findings.size - maxInline)
println("Done: ${minOf(findings.size, maxInline)} inline comment(s), $overflow in body spoiler")
