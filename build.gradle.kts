import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask
import org.jmailen.gradle.kotlinter.support.ReporterType

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.publish)
    alias(libs.plugins.dokka)
}

group = "me.devnatan"
version = property("version")
    .toString()
    .ifEmpty { null }
    ?.takeUnless { it == "unspecified" }
    ?.filterNot { it == 'v' } ?: nextGitTag()

fun nextGitTag(): String {
    val latestTag = providers.exec {
        commandLine("git", "describe", "--tags", "--abbrev=0")
    }.standardOutput.asText.get().trim()

    val versionParts = latestTag.removePrefix("v").split(".")
    val major = versionParts.getOrNull(0)?.toIntOrNull() ?: 0
    val minor = versionParts.getOrNull(1)?.toIntOrNull() ?: 0

    return "$major.${minor + 1}.0-SNAPSHOT"
}

repositories {
    mavenCentral()
}

kotlin {
    explicitApi()

    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
    }

    compilerOptions {
        freeCompilerArgs.addAll(listOf(
            "-Xconsistent-data-class-copy-visibility",
            "-Xexpect-actual-classes",
            "-opt-in=kotlin.time.ExperimentalTime"
        ))
    }

    jvm {
        tasks.named<Test>("jvmTest") {
            useJUnitPlatform()
            testLogging {
                showExceptions = true
                showStandardStreams = true
                events = setOf(
                    org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                    org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
                )
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }
    }

    linuxX64()
    macosX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.ktx.coroutines.core)
                implementation(libs.bundles.ktor)
                implementation(libs.bundles.ktx)
                implementation(libs.kotlinx.io.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.ktx.coroutines.test)
            }
        }

        val jvmMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.junixsocket)
                implementation(libs.ktor.client.engine.okhttp)
                implementation(libs.slf4j.api)
                api(libs.apache.compress)
            }
        }

        val jvmTest by getting {
            dependsOn(commonTest)
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }

        val nativeMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.engine.cio)
            }
        }

        val nativeTest by creating {
            dependsOn(commonTest)
        }

        val linuxX64Main by getting { dependsOn(nativeMain) }
        val linuxX64Test by getting { dependsOn(nativeTest) }
        val macosX64Main by getting { dependsOn(nativeMain) }
        val macosX64Test by getting { dependsOn(nativeTest) }
    }
}

tasks.check {
    dependsOn("installKotlinterPrePushHook")
}

tasks.withType<DokkaGenerateTask>().configureEach {
    notCompatibleWithConfigurationCache("Direct project access")

    doLast {
        val packageJsonFile = layout.projectDirectory.file("docs/package.json").asFile
        val currentVersion = project.version.toString()

        val packageJsonContent = packageJsonFile.readText()
        val updatedContent = packageJsonContent.replaceFirst(
            Regex(""""version":\s*"[^"]*""""),
            """"version": "$currentVersion"""",
        )
        packageJsonFile.writeText(updatedContent)
    }
}

kotlinter {
    reporters = arrayOf(ReporterType.html.name)
}

dokka {
    dokkaPublications.html {
        moduleName.set("Docker Kotlin")
        moduleVersion.set(project.version.toString())
        outputDirectory.set(layout.projectDirectory.dir("docs/public/docker-kotlin"))
    }

    dokkaSourceSets.configureEach {
        documentedVisibilities.set(setOf(VisibilityModifier.Public))

        sourceLink {
            val sourceSetName = this@configureEach.name
            localDirectory.set(file("src/$sourceSetName/kotlin"))

            remoteUrl("https://github.com/devnatan/docker-kotlin/blob/main/src/$sourceSetName/kotlin")
            remoteLineSuffix.set("#L")
        }
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true, validateDeployment = true)
    signAllPublications()
    pomFromGradleProperties()
    configureBasedOnAppliedPlugins()
}