import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
    id("com.vanniktech.maven.publish") version "0.29.0"
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "16.0"
        framework {
            baseName = "shared"
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            //put your multiplatform dependencies here
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace =  "io.github.cubitsachita"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}


mavenPublishing {
    // Define coordinates for the published artifact
    coordinates(
        groupId = "io.github.cubitsachita",
        artifactId = "shared",
        version = "1.0.1"
    )

    // Configure POM metadata for the published artifact
    pom {
        name.set("KMP Library for saving data to test")
        description.set("This library can be used by Android and iOS targets for the shared functionality ")
        inceptionYear.set("2024")
        url.set("https://github.com/CubitSachita/hyqqbcecpl")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        // Specify developer information
        developers {
            developer {
                id.set("CubitSachita")
                name.set("CubitSachita")
                email.set("cubitsachita@gmail.com")
            }
        }

        // Specify SCM information
        scm {
            url.set("https://github.com/CubitSachita/hyqqbcecpl")
        }
    }

    // Configure publishing to Maven Central
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    // Enable GPG signing for all publications
    signAllPublications()
}


