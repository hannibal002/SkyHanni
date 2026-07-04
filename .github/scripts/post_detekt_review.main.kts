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

val label = "Detekt"
val MARKER = "<!-- detekt-review -->"
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
        if (status !in 200..299) System.err.println("Warning: could not add $label label (HTTP $status)")
    } else {
        val encoded = URLEncoder.encode(label, StandardCharsets.UTF_8)
        val (status, _) = ghRequest("DELETE", "/repos/$repo/issues/$prNumber/labels/$encoded")
        if (status !in 200..299 && status != 404) System.err.println("Warning: could not remove $label label (HTTP $status)")
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

fun buildBody(findings: List<Finding>): String = buildString {
    appendLine(MARKER)
    if (findings.isEmpty()) {
        appendLine("## No Detekt issues found ✅")
        return@buildString
    }
    appendLine("## Detekt found ${findings.size} ${if (findings.size == 1) "issue" else "issues"}")
    appendLine("")
    val direct = findings.take(20)
    val overflow = findings.drop(20)
    direct.forEach { appendLine("- **`${sanitize(it.path)}`**:${it.line} `${sanitize(it.ruleId)}`: ${sanitize(it.message)}") }
    if (overflow.isNotEmpty()) {
        appendLine("\n<details><summary>${overflow.size} more ${if (overflow.size == 1) "issue" else "issues"}</summary>\n")
        overflow.forEach { appendLine("- **`${sanitize(it.path)}`**:${it.line} `${sanitize(it.ruleId)}`: ${sanitize(it.message)}") }
        appendLine("\n</details>")
    }
}

fun findExistingComment(prNumber: String): Long? {
    var page = 1
    while (true) {
        val (status, body) = ghRequest("GET", "/repos/$repo/issues/$prNumber/comments?per_page=100&page=$page")
        if (status !in 200..299) return null
        val arr = body as? JsonArray ?: return null
        if (arr.size() == 0) return null
        for (elem in arr) {
            if (!elem.isJsonObject) continue
            val bodyText = elem.asJsonObject.get("body")?.takeIf { it.isJsonPrimitive }?.asString ?: continue
            if (MARKER in bodyText) return elem.asJsonObject.get("id")?.takeIf { it.isJsonPrimitive }?.asLong
        }
        if (arr.size() < 100) return null
        page++
    }
}

fun upsertComment(prNumber: String, body: String) {
    val existingId = findExistingComment(prNumber)
    if (existingId != null) {
        val (status, _) = ghRequest("PATCH", "/repos/$repo/issues/comments/$existingId", mapOf("body" to body))
        if (status !in 200..299) System.err.println("Warning: could not update comment (HTTP $status)")
    } else {
        val (status, _) = ghRequest("POST", "/repos/$repo/issues/$prNumber/comments", mapOf("body" to body))
        if (status !in 200..299) System.err.println("Warning: could not post comment (HTTP $status)")
    }
}

val prNumber: String = System.getenv("PR_NUMBER")?.takeIf { it.isNotEmpty() }
    ?: run { println("PR_NUMBER not set, skipping"); exitProcess(0) }

val artifactDir = Path(System.getenv("ARTIFACT_DIR") ?: "detekt-artifact")
val sarifFile = artifactDir / "main.sarif"

if (!sarifFile.exists()) {
    println("No SARIF found, removing label")
    setLabel(prNumber, false)
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

upsertComment(prNumber, buildBody(findings))
setLabel(prNumber, true)
println("Done: ${findings.size} finding(s) posted")
