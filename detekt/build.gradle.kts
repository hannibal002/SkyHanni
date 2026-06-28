import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

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
