import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://jitpack.io") {
        content {
            includeGroupByRegex("com\\.github\\..*")
        }
    }
}

dependencies {
    implementation(libs.gson)
    implementation(libs.kotlin.stdlib)
    implementation(libs.changelog.builder)
    implementation(files("../sharedVariables/build/libs/sharedVariables.jar"))
    implementation(libs.methanol)
}

tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget("25"))
}
