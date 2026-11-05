import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}
kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }
    jvm("desktop")
    sourceSets {
        commonMain.dependencies {
            implementation(project(":mockEngine"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies { implementation(libs.androidx.activity.compose) }
        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")
            }
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        named("desktopTest") { dependencies { implementation(kotlin("test-junit")) } }
    }
}
android {
    namespace = "org.mz.botanicalsmock"
    compileSdk = 35
    defaultConfig {
        applicationId = "org.mz.botanicalsmock"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
compose.desktop {
    application {
        mainClass = "org.mz.botanicalsmock.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            includeAllModules = true
            packageName = "MzbotanicalsMock"
            packageVersion = "1.0.0"
            description = "Offline MZ Botanicals sales demonstration"
            windows {
                iconFile.set(project.file("src/desktopMain/resources/mock.ico"))
                perUserInstall = true
                menu = true
                shortcut = true
                menuGroup = "MzbotanicalsMock"
                upgradeUuid = "82c8382b-5c44-45bf-a382-23cc8c5f014a"
            }
        }
    }
}
