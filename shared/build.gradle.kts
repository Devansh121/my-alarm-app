import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target: KotlinNativeTarget ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.sqldelight.runtime)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        iosMain.dependencies { implementation(libs.sqldelight.native) }
    }
}

sqldelight {
    databases {
        create("AlarmDb") { packageName.set("com.devansh.alarm.db") }
    }
}
