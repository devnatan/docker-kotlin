import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.publish)
}

group = "me.devnatan"
version = property("version")
    .toString()
    .takeUnless { it == "unspecified" }
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

    mingwX64()

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
                runtimeOnly(libs.junixsocket.native)
                implementation(libs.junixsocket.common)
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

        val nativeTest by creating { dependsOn(commonTest) }

        val mingwX64Main by getting { dependsOn(nativeMain) }
        val mingwX64Test by getting { dependsOn(nativeTest) }
    }

    targets.withType<KotlinNativeTarget> {
        val interopDir = layout.projectDirectory.dir("../interop")
        compilations["main"].cinterops {
            val libhttp by creating {
                defFile("src/nativeInterop/cinterop/http_native.def")
                packageName("me.devnatan.dockerkt.interop")
                includeDirs.allHeaders("src/nativeInterop/cinterop")

                val platform = this@withType.name
                when (platform) {
                    "mingwX64", "mingwX64Main" -> {
                        extraOpts(
                            "-libraryPath", "$interopDir/target/x86_64-pc-windows-gnu/debug",
                        )
                    }
                    else -> extraOpts(
                        "-libraryPath", "$interopDir/target/debug"
                    )
                }
            }
        }
    }
}

tasks.check {
    dependsOn("installKotlinterPrePushHook")
}

mavenPublishing {
    signing.isRequired = false
}

publishing {
    repositories {
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/devnatan/docker-kotlin")
            credentials(PasswordCredentials::class)
        }
    }
}
