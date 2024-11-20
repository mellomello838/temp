import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
    id("com.vanniktech.maven.publish") version "0.29.0"
    id("kover")
}
var androidTarget: String = ""

kotlin {
    val android = android {
        publishLibraryVariants("release")
    }
    androidTarget = android.name
    val xcf = XCFramework()
    val iosX64 = iosX64()
    val iosArm64 = iosArm64()
    val iosSim = iosSimulatorArm64()
    configure(listOf(iosX64, iosArm64, iosSim)) {
        binaries {
            framework {
                //Any dependecy you add for ios should be added here using export()
                export(libs.kotlin.stdlib)
                xcf.add(this)
            }
        }
    }

    targets.withType<KotlinNativeTarget> {
        binaries.all {
            freeCompilerArgs += listOf("-Xgc=cms")
        }
    }

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = libs.versions.library.version.get()
        ios.deploymentTarget =  "16.0"
        framework {
            baseName = "shared"
            isStatic = false
            transitiveExport = true
            embedBitcode(BitcodeEmbeddingMode.BITCODE)
        }
        specRepos {
            url("https://github.com/mellomello838/temp.git") //use your repo here
        }
        publishDir = rootProject.file("./")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlin.stdlib)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val androidMain by getting {
            dependencies {
                //Add your specific android dependencies here
            }
        }
        val androidUnitTest by getting {
            dependsOn(androidMain)
            dependsOn(commonMain)
            dependencies {
                implementation(kotlin("test-junit"))
                //you should add the android junit here
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                //Add any ios specific dependencies here, remember to also add them to the export block
            }
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}

android {
    namespace = "io.github.cubitsachita"
    compileSdk = 34
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    beforeEvaluate {
        libraryVariants.all {
            compileOptions {
                // Flag to enable support for the new language APIs
                isCoreLibraryDesugaringEnabled = true
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

publishing {
    repositories {
        maven {
            name = "github"
            url = uri("https://maven.pkg.github.com/mellomello838/temp")
            credentials {
                username = System.getenv()["MYUSER"]
                password = System.getenv()["MYPAT"]
            }
        }
    }
    val thePublications = listOf(androidTarget) + "kotlinMultiplatform"
    publications {
        matching { it.name in thePublications }.all {
            val targetPublication = this@all
            tasks.withType<AbstractPublishToMaven>()
                .matching { it.publication == targetPublication }
                .configureEach { onlyIf { findProperty("isMainHost") == "true" } }
        }
        matching { it.name.contains("ios", true) }.all {
            val targetPublication = this@all
            tasks.withType<AbstractPublishToMaven>()
                .matching { it.publication == targetPublication }
                .forEach { it.enabled = false }
        }
    }
}

afterEvaluate {
    tasks.named("podPublishDebugXCFramework") {
        enabled = false
    }
    tasks.named("podSpecDebug") {
        enabled = false
    }
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
    }
    tasks.withType<AbstractTestTask>().configureEach {
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events("started", "skipped", "passed", "failed")
            showStandardStreams = true
        }
    }
}

val buildIdAttribute = Attribute.of("buildIdAttribute", String::class.java)
configurations.forEach {
    it.attributes {
        attribute(buildIdAttribute, it.name)
    }
}

val moveIosPodToRoot by tasks.registering {
    group = "myGroup"
    doLast {
        val releaseDir = rootProject.file(
            "./release"
        )
        releaseDir.copyRecursively(
            rootProject.file("./"),
            true
        )
        releaseDir.deleteRecursively()
    }
}

tasks.named("podPublishReleaseXCFramework") {
    finalizedBy(moveIosPodToRoot)
}

val publishPlatforms by tasks.registering {
    group = "myGroup"
    dependsOn(
        tasks.named("podPublishReleaseXCFramework")
    )
    doLast {
        exec { commandLine = listOf("git", "add", "-A") }
        exec {
            commandLine = listOf(
                "git",
                "commit",
                "-m",
                "iOS binary lib for version ${libs.versions.library.version.get()}"
            )
        }
        exec { commandLine = listOf("git", "push", "origin", "main") }
        exec { commandLine = listOf("git", "tag", libs.versions.library.version.get()) }
        exec { commandLine = listOf("git", "push", "--tags") }
        println("version ${libs.versions.library.version.get()} built and published")
    }
}

val compilePlatforms by tasks.registering {
    group = "myGroup"
    dependsOn(
        tasks.named("compileKotlinIosArm64"),
        tasks.named("compileKotlinIosX64"),
        tasks.named("compileKotlinIosSimulatorArm64"),
        tasks.named("compileReleaseKotlinAndroid")
    )
    doLast {
        println("Finished compilation")
    }
}
