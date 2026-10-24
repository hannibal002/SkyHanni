plugins {
    alias(libs.plugins.loom) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply false
    id("dev.kikugie.stonecutter")
}

allprojects {
    group = "at.hannibal2.skyhanni"

    val buildToolsPath = when (name) {
        "SkyHanni" -> layout.projectDirectory.dir("buildTools")
        "annotation-processors", "detekt" -> layout.projectDirectory.dir("../buildTools")
        else -> layout.projectDirectory.dir("../../buildTools")
    }

    /**
     * The version of the project.
     * Stable version
     * Beta version
     * Bugfix version
     */
    version = providers.fileContents(buildToolsPath.file("PROJECT_VERSION")).asText.map { it.trim() }.get()

    repositories {
        mavenCentral()
        mavenLocal()

        // Fabric
        exclusiveContent {
            forRepository {
                maven("https://maven.fabricmc.net")
            }
            filter {
                includeGroupAndSubgroups("net.fabricmc")
            }
        }

        // Mixin
        exclusiveContent {
            forRepository {
                maven("https://repo.spongepowered.org/repository/maven-public")
            }
            filter {
                includeGroup("org.spongepowered")
            }
        }

        // DevAuth
        exclusiveContent {
            forRepository {
                maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
            }
            filter {
                includeGroup("me.djtheredstoner")
            }
        }

        // libautoupdate
        exclusiveContent {
            forRepository {
                maven("https://repo.nea.moe/releases")
            }
            filter {
                includeGroup("moe.nea")
            }
        }

        // MoulConfig and a few Detekt rules
        exclusiveContent {
            forRepositories(
                repositories.mavenLocal(),
                repositories.maven("https://maven.notenoughupdates.org/releases"),
            )
            filter {
                includeGroupAndSubgroups("org.notenoughupdates")
            }
        }

        // Hypixel Mod API
        exclusiveContent {
            forRepository {
                maven("https://repo.hypixel.net/repository/Hypixel")
            }
            filter {
                includeGroup("net.hypixel")
            }
        }

        // Modrinth
        exclusiveContent {
            forRepository {
                maven("https://api.modrinth.com/maven")
            }
            filter {
                includeGroup("maven.modrinth")
            }
        }

        // REI for compat plugin
        exclusiveContent {
            forRepository {
                maven("https://maven.shedaniel.me")
            }
            filter {
                includeGroup("dev.architectury")
                includeGroupAndSubgroups("me.shedaniel")
            }
        }

        exclusiveContent {
            forRepositories(
                repositories.maven("https://maven.azureaaron.net/releases"),
            )
            filter {
                includeGroupAndSubgroups("net.azureaaron")
            }
        }
    }
}

stonecutter active "26.2"

stonecutter handlers {
    configure("fsh", "vsh") {
        commenter = line("//")
    }
}

stonecutter parameters {
    filters.include("**/*.fsh", "**/*.vsh")
}
