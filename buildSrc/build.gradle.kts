
plugins {
    `kotlin-dsl`
    kotlin("jvm")version "1.8.10"
}
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}

sourceSets.main {
    kotlin {
        srcDir(file("src"))
    }
}
