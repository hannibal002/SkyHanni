import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask


plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("dev.detekt")
}

dependencies {
    compileOnly(libs.detekt.api)
    ksp(libs.autoservice.ksp)
    implementation(libs.autoservice.annotations)
    implementation(libs.detektrules.ktlint)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.detekt.test)
    detektPlugins(libs.detektrules.authors)
    detektPlugins(libs.detektrules.ktlint)
}

tasks.withType<Detekt>().configureEach {
    onlyIf { false }
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    onlyIf { false }
}
