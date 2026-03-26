import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("io.gitlab.arturbosch.detekt")
}

dependencies {
    implementation(libs.detekt.api)
    ksp(libs.autoservice.ksp)
    implementation(libs.autoservice.annotations)
    implementation(libs.detekt.formatting)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.detekt.test)
    detektPlugins(libs.detektrules.authors)
}

tasks.withType<Detekt>().configureEach {
    onlyIf { false }
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    onlyIf { false }
}
