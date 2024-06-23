import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    kotlin("jvm")
    java
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.8.0-1.0.8")

    implementation("com.squareup:javapoet:1.13.0")
    implementation("com.squareup:kotlinpoet:1.13.0")
    implementation("com.squareup:kotlinpoet-ksp:1.13.0")
    implementation("com.squareup:kotlinpoet-javapoet:1.13.0")
}

tasks.withType<JavaCompile> {
    if (JavaVersion.current().isJava9Compatible) {
        options.release.set(8)
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
