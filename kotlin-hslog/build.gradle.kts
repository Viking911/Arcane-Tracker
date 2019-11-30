plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

kotlin {
    jvm()
    android {
        publishAllLibraryVariants()
    }
    macosX64 {
        binaries {
            framework {
                export(project(":kotlin-hsmodel")) {
                    transitiveExport = true // for kotlinx.io
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(Libs.stdlibCommon)
                implementation(Libs.klock)
                implementation(project(":kotlin-deckstring"))
                api(project(":kotlin-hsmodel"))
                api(project(":kotlin-hsreplay-api"))
                api(project(":kotlin-console"))
            }
        }
        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation(Libs.kotlinTestJvm)
                implementation(Libs.serializationRuntime)
            }
        }
        named("androidMain") {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }
    }
}

android {
    compileSdkVersion(Versions.compileSdkVersion)
    defaultConfig {
        minSdkVersion(Versions.minSdkVersion)
        targetSdkVersion(Versions.targetSdkVersion)
    }
}
