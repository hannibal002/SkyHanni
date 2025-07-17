#!/usr/bin/env kotlin

import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

// How many times to retry - override with MAX_RETRIES env var
val maxAttempts = System.getenv("MAX_RETRIES")?.toIntOrNull() ?: 3

// Grab everything after the script name as our gradlew args
val gradleArgs = args.toList()
if (gradleArgs.isEmpty()) {
    println("Usage: gradlew-retry.kts <gradlew-args...>")
    exitProcess(1)
}
val cmd = listOf("./gradlew") + gradleArgs

/**
 * REGEX-TEST: Could not determine the dependencies of task ':1.8.9:shadowJar'.
 * REGEX-TEST: Could not GET 'https://repo.spongepowered.org/repository/maven-public/org/spongepowered/mixin/0.7.11-SNAPSHOT/mixin-0.7.11-20180703.121122-1.jar'
 * REGEX-TEST: Could not GET 'https://maven.minecraftforge.net/org/apache/logging/log4j/log4j-core/'. Received status code 502 from server: Bad Gateway
 * REGEX-TEST: Could not resolve all files for configuration ':1.16.5-forge:compileClasspath'.
 * REGEX-TEST: > Could not resolve all files for configuration ':1.8.9:compileClasspath'.
 * REGEX-TEST:          > Could not GET 'https://maven.shedaniel.me/dev/architectury/architectury-naming-service/2.0.9/architectury-naming-service-2.0.9.jar'.
 */
val retryableErrorRegex = Regex(
    // language=RegExp
    "(?:(?: +)?\\> +)?Could not (?:GET '?(?<url>https?:\\/\\/[^']+)(?:'\\.?)?(?: (?<error>.*))?|determine|resolve)(?: (?:all files for configuration|the dependencies of task) '(?<task>:[^']+)')?",
)

for (i in 1..maxAttempts) {
    println("Gradle attempt #$i/$maxAttempts: ${cmd.joinToString(" ")}")

    val proc = ProcessBuilder(cmd)
        .redirectErrorStream(true)
        .start()
    val output = proc.inputStream.bufferedReader().readText()
    proc.waitFor()
    print(output)

    if (proc.exitValue() == 0) {
        println("✅ Build succeeded on attempt #$i")
        exitProcess(0)
    } else if (retryableErrorRegex.containsMatchIn(output)) {
        val backoffTime = (i * 1000).milliseconds
        println("   detected retryable error; retrying in ${backoffTime.inWholeSeconds} seconds...")
        Thread.sleep(backoffTime.toJavaDuration())
    } else {
        println("   non-retryable failure; aborting.")
        exitProcess(proc.exitValue())
    }
}

println("❌ failed after $maxAttempts attempts")
exitProcess(1)
