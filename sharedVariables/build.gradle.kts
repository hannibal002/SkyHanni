plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(libs.gson)
    implementation(libs.guava)
}

sourceSets.main {
    kotlin.srcDir(file("src"))
}

gradlePlugin {
    plugins {
        create("simplePlugin") {
            id = "at.skyhanni.shared-variables"
            implementationClass = "at.skyhanni.sharedvariables.NoOp"
        }
    }
}
