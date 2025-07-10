import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
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
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.github.SkyHanniStudios:SkyHanniChangelogBuilder:1.1.2")
    implementation(files("../sharedVariables/build/libs/sharedVariables.jar"))
    implementation("com.github.mizosoft.methanol:methanol:1.8.3")
}

tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget("21"))
}
