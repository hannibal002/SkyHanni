import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("dev.detekt")
}

// TODO remove this once Detekt 2.0.0-alpha.6 is out
// https://github.com/detekt/detekt/issues/9409
abstract class DetektTestMetadataRule : ComponentMetadataRule {

    override fun execute(context: ComponentMetadataContext) {
        val version = context.details.id.version
        if (version != "2.0.0-alpha.4" && version != "2.0.0-alpha.5") return

        context.details.withVariant("runtimeElements") {
            withDependencies {
                // detekt-test 2.0.0-alpha.4 and 2.0.0-alpha.5 request detekt-api test fixtures, but
                // detekt-api only publishes fixture sources.
                removeAll { it.group == "dev.detekt" && it.name == "detekt-api" }
                add("dev.detekt:detekt-api:$version")
            }
        }
    }
}

dependencies {
    // TODO remove this once Detekt 2.0.0-alpha.6 is out
    // https://github.com/detekt/detekt/issues/9409
    components {
        withModule<DetektTestMetadataRule>("dev.detekt:detekt-test")
    }

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
